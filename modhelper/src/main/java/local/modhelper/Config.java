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
    /** Версия формата конфига: при росте версии шаблоны чата и звук сбрасываются на новые значения по умолчанию. */
    private static final int CURRENT_VERSION = 4;

    public int configVersion = 0;
    public boolean enabled = true;

    // ---- звук ----
    public boolean playSound = true;
    /** arrow_hit (по умолчанию), levelup, anvil, bell */
    public String soundName = "arrow_hit";
    /** Сколько копий звука играть одновременно (громкость выше 1.0 игра не даёт, поэтому звук накладывается). */
    public int soundLayers = 2;
    public float soundVolume = 1.0f;
    public float soundPitch = 1.0f;

    // ---- подсветка ----
    /** Цвет всего сообщения нарушителя, hex. */
    public String highlightColor = "#FF55FF";
    /** Что красить: "nick" — ник и текст (префиксы/значки привилегий не трогаем), "body" — только текст, "all" — всю строку. */
    public String highlightScope = "nick";

    // ---- команда мута ----
    /** Плейсхолдеры: {nick} {time} {reason}. Команд бана пока нет. */
    public String muteCommand = "/tempmute {nick} {time} {reason}";
    /** Текст кнопки. Плейсхолдеры: {id} {time}. */
    public String muteButtonLabel = "МУТ {id} {time}";
    /** Максимум кнопок мута под одним сообщением. */
    public int maxButtons = 4;
    /** Суффиксы времени в твоей команде: 30m / 2h / 3d. */
    public String daySuffix = "d";
    public String hourSuffix = "h";
    public String minuteSuffix = "m";

    // ---- пороги ----
    /** Сколько матерных слов нужно для 3.7. 1 = любое матерное слово; 2 = строго по правилам «от 2+ слов». */
    public int matThreshold = 1;
    /** Сколько раз слово должно повториться в одном сообщении (или подряд). */
    public int repeatWordThreshold = 3;
    /** Сколько похожих предложений в одном сообщении считается повтором. */
    public int repeatSentenceThreshold = 2;
    /** 3.1: сколько похожих сообщений от одного ника за окно времени. */
    public int floodCount = 3;
    public int floodWindowSeconds = 60;
    /** Похожесть по буквам (0..1). Меньше = строже ловит «слегка изменённые» сообщения. */
    public double floodSimilarity = 0.7;
    /** Похожесть по словам (0..1): доля совпавших слов, нужна для шаблонного спама. */
    public double floodWordSimilarity = 0.6;
    /** 3.4: слово БЕЗ пробелов длиной от N символов, состоящее максимум из 3 разных символов (АХАХАХАХАХ, ПХПХПХПХПХ, !!!!!!!!!!). */
    public int spamRunLength = 10;
    /** 3.4: столько одинаковых знаков подряд без пробелов (!!!!!!!!!!). Меньше — не флуд. */
    public int symbolRepeat = 10;
    /** 3.5: столько слов целиком КАПСОМ. */
    public int capsWords = 3;
    /** 3.5: слова короче этого числа букв (LL, ZZ, GG, EZ) в капсе не учитываются. */
    public int capsMinWordLength = 3;

    /** Шаблоны разбора строки чата. Обязательны группы (?<nick>...) и (?<msg>...). */
    public List<String> chatPatterns = new ArrayList<>(List.of(
            "^<(?<nick>[A-Za-z0-9_]{3,16})>\\s*(?<msg>.+)$",
            "^(?:.*?)(?<![A-Za-z0-9_])(?<nick>[A-Za-z0-9_]{3,16})\\s*(?:»|>>|›|→|:)\\s*(?<msg>.+)$"
    ));

    /** Название твоего проекта (для 3.10). Пусто = правило выключено. */
    public List<String> projectNames = new ArrayList<>();
    /** Сторонние проекты (для 4.5). */
    public List<String> foreignProjects = new ArrayList<>();
    /** Ники/имена ютуберов проекта (для 6.6). */
    public List<String> youtuberNames = new ArrayList<>();
    /** Слова, которыми называют администрацию (для 6.4). */
    public List<String> staffWords = new ArrayList<>(List.of("админ", "модер", "хелпер", "стафф", "персонал"));
    /** Домены, которые НЕ считаются рекламой (для 5.1). */
    public List<String> allowedDomains = new ArrayList<>(List.of("minecraft.net"));

    /** Ники, которых мод вообще не проверяет (без учёта регистра). */
    public List<String> ignoredNicks = new ArrayList<>();
    /** Префиксы/привилегии, игроки с которыми не проверяются, например ["[VIP]", "[Модер]"]. */
    public List<String> ignoredPrefixes = new ArrayList<>();

    public transient List<Pattern> compiledPatterns = new ArrayList<>();
    public transient int highlightRgb = 0xFF55FF;

    // ---- IO ----
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
        Config d = new Config();
        if (c.configVersion < 2) {
            c.chatPatterns = d.chatPatterns;
            c.soundName = d.soundName;
            c.soundLayers = d.soundLayers;
            c.soundVolume = d.soundVolume;
            c.soundPitch = d.soundPitch;
        }
        if (c.configVersion < 3) {
            c.matThreshold = d.matThreshold;
        }
        if (c.configVersion < 4) {
            c.symbolRepeat = d.symbolRepeat;
        }
        c.configVersion = CURRENT_VERSION;
        c.fixNulls();
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

    private void fixNulls() {
        Config d = new Config();
        if (highlightColor == null) highlightColor = d.highlightColor;
        if (highlightScope == null) highlightScope = d.highlightScope;
        if (soundName == null) soundName = d.soundName;
        if (muteCommand == null) muteCommand = d.muteCommand;
        if (muteButtonLabel == null) muteButtonLabel = d.muteButtonLabel;
        if (daySuffix == null) daySuffix = d.daySuffix;
        if (hourSuffix == null) hourSuffix = d.hourSuffix;
        if (minuteSuffix == null) minuteSuffix = d.minuteSuffix;
        if (chatPatterns == null) chatPatterns = d.chatPatterns;
        if (projectNames == null) projectNames = new ArrayList<>();
        if (foreignProjects == null) foreignProjects = new ArrayList<>();
        if (youtuberNames == null) youtuberNames = new ArrayList<>();
        if (staffWords == null) staffWords = d.staffWords;
        if (allowedDomains == null) allowedDomains = d.allowedDomains;
        if (ignoredNicks == null) ignoredNicks = new ArrayList<>();
        if (ignoredPrefixes == null) ignoredPrefixes = new ArrayList<>();
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
        try {
            highlightRgb = Integer.parseInt(highlightColor.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            highlightRgb = 0xFF55FF;
        }
    }
}
