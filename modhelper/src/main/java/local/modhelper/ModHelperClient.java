package local.modhelper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModHelperClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("modhelper");

    /** Ник попадает в команду, поэтому разрешаем только безопасные символы (защита от подмены команды через чат). */
    private static final Pattern SAFE_NICK = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private static Config config;
    private static Detector detector;

    private record Parsed(String nick, String msg, int nickStart, int nickEnd, int bodyStart) {}

    @Override
    public void onInitializeClient() {
        reload();

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : process(message));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("modhelper")
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                            config.enabled = !config.enabled;
                            config.save();
                            ctx.getSource().sendFeedback(Text.literal("ModHelper: " + (config.enabled ? "включён" : "выключен")));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(ClientCommandManager.literal("ignore")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("nick", StringArgumentType.word()).executes(ctx -> {
                                            String n = StringArgumentType.getString(ctx, "nick");
                                            boolean has = config.ignoredNicks.stream().anyMatch(x -> x.equalsIgnoreCase(n));
                                            if (!has) config.ignoredNicks.add(n);
                                            config.save();
                                            ctx.getSource().sendFeedback(Text.literal("ModHelper: " + n + " в белом списке"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("nick", StringArgumentType.word()).executes(ctx -> {
                                            String n = StringArgumentType.getString(ctx, "nick");
                                            config.ignoredNicks.removeIf(x -> x.equalsIgnoreCase(n));
                                            config.save();
                                            ctx.getSource().sendFeedback(Text.literal("ModHelper: " + n + " убран из белого списка"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                                .then(ClientCommandManager.literal("list").executes(ctx -> {
                                    ctx.getSource().sendFeedback(Text.literal("Белый список: " + config.ignoredNicks
                                            + " | префиксы: " + config.ignoredPrefixes));
                                    return Command.SINGLE_SUCCESS;
                                })))
                        .then(ClientCommandManager.literal("reload").executes(ctx -> {
                            reload();
                            ctx.getSource().sendFeedback(Text.literal("ModHelper: конфиг перезагружен"));
                            return Command.SINGLE_SUCCESS;
                        }))));
    }

    private static void reload() {
        config = Config.load();
        detector = new Detector(config);
    }

    private static Parsed parse(String plain) {
        for (Pattern p : config.compiledPatterns) {
            Matcher m = p.matcher(plain);
            if (m.matches()) {
                return new Parsed(m.group("nick"), m.group("msg"), m.start("nick"), m.end("nick"), m.start("msg"));
            }
        }
        return null;
    }

    private static Text process(Text original) {
        try {
            if (!config.enabled) return original;
            String plain = original.getString();
            Parsed p = parse(plain);
            if (p == null || !SAFE_NICK.matcher(p.nick()).matches()) return original;
            if (isIgnored(p, plain)) return original;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && p.nick().equalsIgnoreCase(mc.player.getName().getString())) return original;

            List<Rule> hits = detector.check(p.nick(), p.msg());
            if (hits.isEmpty()) return original;

            // Одно правило = одна запись (3.1 может прийти дважды: «похожие» и «повтор слов»)
            Map<String, Rule> byId = new LinkedHashMap<>();
            for (Rule r : hits) byId.putIfAbsent(r.id(), r);

            // Причины возле ника — только номера пунктов, подробности в подсказке при наведении
            StringBuilder all = new StringBuilder("Нарушения:");
            for (Rule r : hits) all.append("\n• ").append(r.display());
            MutableText tag = Text.literal(" [⚠ " + String.join(", ", byId.keySet()) + "]")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true)
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal(all.toString()))));

            // По кнопке мута на КАЖДОЕ нарушенное правило (кроме банов — для них кнопок пока нет)
            MutableText buttons = Text.empty();
            int shown = 0;
            for (Rule r : byId.values()) {
                if (r.ban()) continue;
                buttons.append(muteButton(r, p.nick()));
                if (++shown >= Math.max(1, config.maxButtons)) break;
            }

            String scope = config.highlightScope.toLowerCase(Locale.ROOT);
            int colorFrom = scope.equals("all") ? 0 : scope.equals("body") ? p.bodyStart() : p.nickStart();

            Text result = rebuild(original, p.nickEnd(), colorFrom, tag, buttons, config.highlightRgb);

            if (config.playSound) {
                mc.execute(() -> playAlert(mc));
            }
            return result;
        } catch (Exception e) {
            LOG.warn("Ошибка обработки сообщения", e);
            return original;
        }
    }

    /** Игроки из белого списка (ник или привилегия-префикс) не проверяются и не подсвечиваются. */
    private static boolean isIgnored(Parsed p, String plain) {
        for (String n : config.ignoredNicks) {
            if (n != null && n.equalsIgnoreCase(p.nick())) return true;
        }
        String head = plain.substring(0, Math.min(p.nickEnd(), plain.length())).toLowerCase(Locale.ROOT);
        for (String pref : config.ignoredPrefixes) {
            if (pref != null && !pref.isBlank() && head.contains(pref.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static SoundEvent pickSound(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "levelup" -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case "anvil" -> SoundEvents.BLOCK_ANVIL_LAND;
            case "bell" -> SoundEvents.BLOCK_BELL_USE;
            default -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
        };
    }

    /** Громкость выше 1.0 игра не даёт, поэтому для «громче» играем несколько копий звука одновременно. */
    private static void playAlert(MinecraftClient mc) {
        if (mc.player == null) return;
        SoundEvent sound = pickSound(config.soundName);
        int layers = Math.max(1, Math.min(config.soundLayers, 6));
        for (int i = 0; i < layers; i++) {
            mc.getSoundManager().play(new PositionedSoundInstance(
                    sound, SoundCategory.MASTER,
                    config.soundVolume, config.soundPitch, Random.create(),
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
    }

    private static Text muteButton(Rule rule, String nick) {
        String cmd = config.muteCommand
                .replace("{nick}", nick)
                .replace("{time}", rule.time(config))
                .replace("{reason}", rule.reason());
        // В 1.21.6+ click-событие всегда выполняет команду, слеш в начале не нужен.
        String clickCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        String label = " [" + config.muteButtonLabel
                .replace("{id}", rule.id())
                .replace("{time}", rule.time(config)) + "]";

        return Text.literal(label).setStyle(Style.EMPTY
                .withColor(Formatting.GOLD)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(clickCmd))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal(
                        "Нажми — выполнится:\n/" + clickCmd + "\nПричина: " + rule.display()))));
    }

    /**
     * Пересобирает сообщение: вставляет тег причины сразу после ника (tagAt),
     * красит текст с позиции colorFrom в цвет подсветки и добавляет кнопки в конец.
     */
    private static Text rebuild(Text original, int tagAt, int colorFrom, Text tag, Text buttons, int rgb) {
        record Seg(Style style, String text) {}
        List<Seg> segs = new ArrayList<>();
        original.visit((style, str) -> {
            segs.add(new Seg(style, str));
            return Optional.empty();
        }, Style.EMPTY);

        TextColor color = TextColor.fromRgb(rgb);

        MutableText out = Text.empty();
        int pos = 0;
        boolean tagDone = false;
        for (Seg seg : segs) {
            String s = seg.text();
            int i = 0;
            while (i < s.length()) {
                int abs = pos + i;
                int next = s.length();
                if (!tagDone && tagAt > abs) next = Math.min(next, tagAt - pos);
                if (colorFrom > abs) next = Math.min(next, colorFrom - pos);
                Style st = abs >= colorFrom ? seg.style().withColor(color) : seg.style();
                out.append(Text.literal(s.substring(i, next)).setStyle(st));
                i = next;
                if (!tagDone && pos + i == tagAt) {
                    out.append(tag);
                    tagDone = true;
                }
            }
            pos += s.length();
        }
        if (!tagDone) out.append(tag);
        out.append(buttons);
        return out;
    }
}
