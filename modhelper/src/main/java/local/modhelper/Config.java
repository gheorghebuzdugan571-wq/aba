package local.modhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Настройки: .minecraft/config/modhelper.json */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean playSound = true;
    public float soundVolume = 1.0f;
    public float soundPitch = 1.4f;

    /** Шрифт для текста нарушения. minecraft:uniform — "юникодный" шрифт, заметно отличается. */
    public String fontId = "minecraft:uniform";

    /** Команда мута. Плейсхолдеры: {nick} {time} {reason}. Команд бана пока нет. */
    public String muteCommand = "/tempmute {nick} {time} {reason}";

    /** Суффиксы времени в твоей команде: 30m / 2h / 3d. Поменяй под свой плагин (min, ч, д ...). */
    public String daySuffix = "d";
    public String hourSuffix = "h";
    public String minuteSuffix = "m";

    /** Сколько матерных слов нужно для 3.7 (в правилах «от 2+ слов»). */
    public int matThreshold = 2;

    /** Сколько раз слово должно повториться в одном сообщении (или подряд), чтобы считаться флудом. */
    public int repeatWordThreshold = 3;
    /** Сколько похожих предложений в одном сообщении считается повтором. */
    public int repeatSentenceThreshold = 2;

    /** Шаблоны разбора строки чата. Обязательны группы (?<nick>...) и (?<msg>...). Подгони под формат своего сервера. */
    public List<String> chatPatterns = new ArrayList<>(List.of(
            "^<(?<nick>[A-Za-z0-9_]{3,16})>\\s*(?<msg>.+)$",
            "^(?:.*?[\\s\\]\\)\\|])?(?<nick>[A-Za-z0-9_]{3,16})\\s*(?:»|>>|›|→|:)\\s*(?<msg>.+)$"
    ));

    /** Название твоего проекта (для 3.10), например ["mysrv"]. Пусто = правило выключено. */
    public List<String> projectNames = new ArrayList<>();
    /** Сторонние проекты (для 4.5). */
    public List<String> foreignProjects = new ArrayList<>();
    /** Ники/имена ютуберов проекта (для 6.6). */
    public List<String> youtuberNames = new ArrayList<>();
    /** Слова, которыми называют администрацию (для 6.4). */
    public List<String> staffWords = new ArrayList<>(List.of("админ", "модер", "хелпер", "стафф", "персонал"));
    /** Домены, которые НЕ считаются рекламой (для 5.1), например ["minecraft.net"]. */
    public List<String> allowedDomains = new ArrayList<>(List.of("minecraft.net"));

    /** Ники, которых мод вообще не проверяет (без учёта регистра). */
    public List<String> ignoredNicks = new ArrayList<>();
    /**
     * Привилегии/префиксы, игроки с которыми не проверяются. Ищется в части строки чата до ника включительно,
     * например ["[VIP]", "[Youtube]", "[Модер]"]. Пиши так, как префикс выглядит в чате.
     */
    public List<String> ignoredPrefixes = new ArrayList<>();

    public transient List<Pattern> compiledPatterns = new ArrayList<>();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("modhelper.json");
    }

    public static Config load() {
        Config c = null;
        try {
            Path p = path();
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    c = GSON.fromJson(r, Config.class);
                }
            }
        } catch (Exception e) {
            ModHelperClient.LOG.warn("Не удалось прочитать конфиг, использую настройки по умолчанию", e);
        }
        if (c == null) c = new Config();
        c.compile();
        c.save();
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            ModHelperClient.LOG.warn("Не удалось сохранить конфиг", e);
        }
    }

    private void compile() {
        compiledPatterns = new ArrayList<>();
        for (String s : chatPatterns) {
            try {
                compiledPatterns.add(Pattern.compile(s));
            } catch (PatternSyntaxException e) {
                ModHelperClient.LOG.warn("Неверный шаблон чата: {}", s);
            }
        }
    }
}
