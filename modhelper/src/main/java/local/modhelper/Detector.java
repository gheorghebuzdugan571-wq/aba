package local.modhelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Проверка сообщения по правилам. Возвращает нарушения, самое тяжёлое — первым. */
public final class Detector {
    private static final String B = "(?<![\\p{L}\\d])";

    private static Pattern w(String re) {
        return Pattern.compile(B + "(?:" + re + ")");
    }

    // ---- 3.7 мат (слова = 1 балл, фразы = 2 балла) ----
    private static final List<Pattern> MAT = List.of(
            w("(?:за|у|на|вы|по|от|до|пере|раз|с|съ|под|при|про|об)?еб(?:ал|ан|ат|ну|ыр|уч|ис|ет|ут|уш|ли|ля|ск|л)\\p{L}*"),
            w("пизд\\p{L}*"), w("бля\\p{L}*"),
            w("(?:на|по|ни|от|до|за|рас|с)?ху(?:й|и|е|я|ю)\\p{L}*"),
            w("муд(?:ак|ил|оз|ач)\\p{L}*"), w("залуп\\p{L}*"), w("дроч\\p{L}*"),
            w("пид[оаэ]р\\p{L}*"), w("пидр\\p{L}*"), w("педик\\p{L}*"), w("педераст\\p{L}*"), w("пипидастр\\p{L}*"),
            w("г[ао]ндон\\p{L}*"), w("долб[оа]еб\\p{L}*"),
            w("сук(?:а|и|е|у|ой|ам|ами)(?![\\p{L}\\d])"), w("сучк\\p{L}*"), w("сучар\\p{L}*"), w("сукин\\p{L}*"),
            w("шлюх\\p{L}*"), w("шалав\\p{L}*"));
    private static final List<Pattern> MAT_PHRASES = List.of(
            w("(?:пошел|пошол|пошла|пошли|иди|идите)\\s+(?:на\\s*)?(?:ху(?:й|и|е|я|ю)\\p{L}*|хер\\p{L}*|хрен\\p{L}*)"),
            w("я\\s+ебал\\p{L}*"), w("го\\s+секс"), w("го\\s+в\\s+попочк\\p{L}*"), w("дам\\s+в\\s+рот"),
            w("еб\\s+твою\\s+мат\\p{L}*"));

    // ---- 3.8 оскорбления ----
    private static final List<Pattern> INSULT = List.of(
            w("бомж\\p{L}*"), w("бездар\\p{L}*"), w("глуп(?:ый|ая|ое|ые|ого|ому)"), w("даун\\p{L}*"),
            w("дебил\\p{L}*"), w("додик\\p{L}*"), w("дурак\\p{L}*"), w("дура(?![\\p{L}\\d])"),
            w("дурень"), w("животн(?:ое|ые|ым)"), w("идиот\\p{L}*"), w("инвалид\\p{L}*"),
            w("козел"), w("козл\\p{L}*"), w("конченн?\\p{L}*"), w("лох(?:и|а|у|ов|ом|ам)?(?![\\p{L}\\d])"),
            w("мраз(?:ь|и|от\\p{L}*)"), w("лошар\\p{L}*"), w("негр\\p{L}*"), w("мартышк\\p{L}*"),
            w("нищ(?:ий|ая|ие|его|ему|им)"), w("нищеброд\\p{L}*"), w("немощ(?:ь|ный)"),
            w("обоссан\\p{L}*"), w("обезьян\\p{L}*"), w("оболтус\\p{L}*"), w("овощь(?![\\p{L}\\d])"),
            w("твар(?:ь|и)(?![\\p{L}\\d])"), w("туп(?:ой|ая|ое|ые|ица|ого|орыл\\p{L}*)"),
            w("чмо(?![\\p{L}\\d])"), w("чмошн\\p{L}*"), w("чтоб\\s+ты\\s+сдох"), w("кретин\\p{L}*"),
            // ебало и «закрой рот»-фразы
            w("еба(?:ло|льник)\\p{L}*"), w("хлеба(?:ло|льник)\\p{L}*"),
            w("(?:завали|заткни|закрой|захлопни|прикрой)\\s+(?:свое\\s+|свою\\s+|свой\\s+|твое\\s+)?(?:ебало|ебальник|хлебало|хлебальник|пасть|рот|клюв|жало)"),
            w("ублюд\\p{L}*"), w("выродок"), w("выродк\\p{L}*"), w("урод(?:ы|ина|ка|у|ом|а)?(?![\\p{L}\\d])"),
            w("скотин\\p{L}*"), w("сволоч\\p{L}*"), w("подонок"), w("подонк\\p{L}*"),
            w("отсос\\p{L}*"), w("соси(?![\\p{L}\\d])"), w("гнид(?:а|ы|ой|у|е)(?![\\p{L}\\d])"));

    // ---- слова, которые ловим даже с ошибкой в одной букве (далбаеб, пидорсы) ----
    private static final String[] FUZZY_MAT = {"долбаеб", "долбоеб", "ебанат", "ебанько", "пидорас", "пидарас",
            "пидрила", "пиздабол", "хуесос", "гандон", "блядина", "мудозвон", "пипидастр", "выебал", "залупа"};
    private static final String[] FUZZY_INSULT = {"кретин", "бездарь", "лошара", "ублюдок", "скотина", "обезьяна"};

    // ---- 3.10 / 5.1: разговоры про сервер ----
    /** «сервак», «сервер», «серв», «проект» и их формы. */
    private static final Pattern SERVER_WORD = w("сервак\\p{L}*|сервач\\p{L}*|сервер\\p{L}*|серв(?:а|у|е|ом|ы)?(?![\\p{L}\\d])|проект(?:а|у|е|ом|ы)?(?![\\p{L}\\d])");
    /** Негатив в сторону сервера: хуйня, говно, параша, отстой... (плюс все слова из списка оскорблений). */
    private static final Pattern NEG_SERVER = w("говно\\p{L}*|говн\\p{L}*|гавн\\p{L}*|дерьм\\p{L}*|параш\\p{L}*|мусор\\p{L}*|помойк\\p{L}*"
            + "|хуйн\\p{L}*|хуев\\p{L}*|ебан\\p{L}*|ебуч\\p{L}*|отстой|отстойн\\p{L}*|днищ\\p{L}*|дрянь|лажа|лажов\\p{L}*"
            + "|кал(?![\\p{L}\\d])|сос(?:ет|ёт|ешь)|мертв\\p{L}*|дохл\\p{L}*|гнил\\p{L}*|отвратн\\p{L}*|ужасн\\p{L}*"
            + "|фуфло|лохотрон\\p{L}*|развод(?![\\p{L}\\d])|шарага|хрень|скам|убог\\p{L}*|сдох\\p{L}*|умер\\p{L}*");
    /** Приглашение: заходите, залетайте, приходите, переходите... */
    private static final Pattern INVITE = w("заход\\p{L}*|залета\\p{L}*|залет\\p{L}*|приход\\p{L}*|игра(?:й|йте)(?![\\p{L}\\d])|перех\\p{L}*|переезж\\p{L}*|регайтесь|регистр\\p{L}*|присоедин\\p{L}*|вступай\\p{L}*");
    /** «Лучший сервак», «наш сервер», «новый сервер» — признак рекламы вместе с приглашением. */
    private static final Pattern PROMO_QUAL = w("лучш\\p{L}*|топ(?![\\p{L}\\d])|топов\\p{L}*|круч\\p{L}*|крут\\p{L}*|нов(?:ый|ая|ое|ого|ом)(?![\\p{L}\\d])|друг(?:ой|ого|ом)(?![\\p{L}\\d])|мо(?:й|ем|ём)(?![\\p{L}\\d])|наш(?:а|ем|ему|его)?(?![\\p{L}\\d])|хорош\\p{L}*|имба|бомб\\p{L}*|бесплатн\\p{L}*");

    // ---- 5.4 оскорбление родителей: «родственник» + оскорбление рядом ----
    /** мать/мама/матуха/мамка/мамаша..., батя/папа/отец/батька..., родители. */
    private static final Pattern PARENT_WORD = w("мать|матер(?:ь|и|ью|ей)(?![\\p{L}\\d])|матух\\p{L}*|матушк\\p{L}*"
            + "|мам(?:а|у|ы|е|ой|ою)(?![\\p{L}\\d])|мамк\\p{L}*|мамаш\\p{L}*|мамоч\\p{L}*|мамул\\p{L}*|мамуш\\p{L}*|маман\\p{L}*"
            + "|бат(?:я|ю|е|ей|ек|ьк\\p{L}*|юшк\\p{L}*|ян\\p{L}*)(?![\\p{L}\\d])"
            + "|пап(?:а|у|ы|е|ой|ою|очк\\p{L}*|аш\\p{L}*)(?![\\p{L}\\d])"
            + "|отец|отц(?:а|у|ом|е)(?![\\p{L}\\d])|родител\\p{L}*|родак\\p{L}*");
    /** Оскорбления, которые «цепляются» к родственнику (шлюха, пидарас и т.д.; плюс весь список оскорблений 3.8). */
    private static final Pattern PARENT_BAD = w("шлюх\\p{L}*|шалав\\p{L}*|пид[оаэ]р\\p{L}*|пидр\\p{L}*|педик\\p{L}*|педераст\\p{L}*"
            + "|сук(?:а|и|е|у|ой|ам|ами)(?![\\p{L}\\d])|сучк\\p{L}*|сучар\\p{L}*|сукин\\p{L}*"
            + "|мраз(?:ь|и|от\\p{L}*)|твар(?:ь|и)(?![\\p{L}\\d])|ублюд\\p{L}*|выродк\\p{L}*|выродок"
            + "|проститутк\\p{L}*|давалк\\p{L}*|подстилк\\p{L}*|потаскух\\p{L}*|шмар\\p{L}*|курв\\p{L}*|гнид(?:а|ы|ой|у|е)(?![\\p{L}\\d])"
            + "|ебан\\p{L}*|ебал\\p{L}*|ебуч\\p{L}*|хуесос\\p{L}*|хуйл\\p{L}*|залуп\\p{L}*|сосал\\p{L}*|сосет\\p{L}*|мудак\\p{L}*|мудил\\p{L}*");
    /** На сколько слов рядом с «мать/батя/папа» ищем оскорбление. */
    private static final int PARENT_WINDOW = 4;

    // ---- прочее ----
    /** Буквы, разделённые пробелами/точками: "д е б и л", "д.е.б.и.л". */
    private static final Pattern SPACED = Pattern.compile("(?<![\\p{L}\\d])(?:\\p{L}[\\s.\\-_*,]+){3,}\\p{L}(?![\\p{L}\\d])");
    /** Слова-числа вроде 1000000000 или 1.000.000 — не спам. */
    private static final Pattern NUMBERISH = Pattern.compile("[\\d.,'_-]+");
    private static final Pattern SUICIDE = w("убей\\s*ся|убей\\s+себя|повес(?:ься|ь\\s+себя)|вскрой(?:ся|\\s+вены)|выпились|спрыгни\\s+с\\p{L}*|сдохни|умри(?![\\p{L}\\d])|сделай\\s+харакири");
    private static final Pattern ALT_F4 = Pattern.compile("alt\\s*\\+?\\s*f\\s*4");
    private static final Pattern BEG_PUNISH = w("(?:дай\\p{L}*|выдай\\p{L}*)\\s+(?:мне\\s+|себе\\s+)?(?:мут|бан)\\p{L}*|(?:замут\\p{L}*|забан\\p{L}*)\\s+(?:меня|себя)");
    private static final Pattern BEG_RES = w("(?:дай\\p{L}*|выдай\\p{L}*|подкин\\p{L}*|подар\\p{L}*|кинь|скинь)\\s+(?:мне\\s+)?(?:админк\\p{L}*|вип|випк\\p{L}*|донат\\p{L}*|привилеги\\p{L}*|ресурс\\p{L}*|алмаз\\p{L}*|денег|деньги|монет\\p{L}*)");
    private static final Pattern AD = Pattern.compile("(?:https?://|www\\.)\\S+|\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?::\\d{2,5})?\\b|\\b[a-z0-9-]{2,}\\.(?:ru|com|net|org|gg|su|fun|pro|io|me|xyz|club|top|online|site|tk|ml)\\b");
    private static final Pattern PARENT = w("(?:твою|твоей|твоя|твоим)\\s+(?:мать|мамк\\p{L}*|мамаш\\p{L}*|маму|мам[аы]|родител\\p{L}*)|(?:мать|мамк\\p{L}*|мамаш\\p{L}*|маму|мама)\\s+(?:твою|твоя|твоей)");
    private static final Pattern PARENT_STRONG = w("ебал\\p{L}*\\s+(?:твою\\s+)?(?:мать|маму|мамк\\p{L}*|мамаш\\p{L}*)");
    private static final Pattern NEG = w("говно\\p{L}*|говн\\p{L}*|гавн\\p{L}*|дерьм\\p{L}*|параш\\p{L}*|мусор\\p{L}*|помойк\\p{L}*|хуйн\\p{L}*|отстой|днищ\\p{L}*|скам|убог\\p{L}*|сосет\\p{L}*|сосат\\p{L}*|сдох\\p{L}*|умер\\p{L}*");
    private static final Pattern POLITICS = w("путин\\p{L}*|зеленск\\p{L}*|навальн\\p{L}*|трамп(?:а|у|ом|е)?(?![\\p{L}\\d])|байден\\p{L}*|бандер\\p{L}*|слава\\s+(?:украин\\p{L}*|росси\\p{L}*)|голосуй\\p{L}*");
    private static final Pattern THREAT = w("забаню|забаним|забанят|замучу|замутим|замутят|выдам\\s+(?:тебе\\s+)?(?:мут|бан)|получишь\\s+(?:мут|бан)");

    private final Config cfg;
    private final Pattern symbolSpam;
    private final Map<String, Deque<Sample>> history = new HashMap<>();

    private record Sample(long time, String norm, List<String> words) {}

    public Detector(Config cfg) {
        this.cfg = cfg;
        int n = Math.max(2, cfg.symbolRepeat);
        this.symbolSpam = Pattern.compile("([^\\p{L}\\d\\s])\\1{" + (n - 1) + ",}");
    }

    public List<Rule> check(String nick, String msg) {
        String low = msg.toLowerCase(Locale.ROOT).replace('ё', 'е');
        String cyr = toCyr(low);
        Set<Rule> hits = new LinkedHashSet<>();

        // 3.1 флуд: N+ одинаковых/ПОХОЖИХ сообщений от одного ника за окно времени.
        // Похожими считаются и слегка изменённые, и «шаблонные» (одна фраза, меняется последнее слово).
        String norm = collapse(cyr.replaceAll("[^\\p{L}]", ""));
        if (norm.length() > 120) norm = norm.substring(0, 120);
        List<String> words = words(cyr);
        if (history.size() > 1000) history.clear();
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1, cfg.floodWindowSeconds) * 1000L;
        Deque<Sample> dq = history.computeIfAbsent(nick.toLowerCase(Locale.ROOT), k -> new ArrayDeque<>());
        while (!dq.isEmpty() && now - dq.peekFirst().time() > windowMs) dq.pollFirst();
        if (norm.length() >= 3) {
            int similar = 1;
            for (Sample sm : dq) {
                if (similarMsg(sm.norm(), sm.words(), norm, words)) similar++;
            }
            dq.addLast(new Sample(now, norm, words));
            while (dq.size() > 30) dq.pollFirst();
            if (similar >= Math.max(2, cfg.floodCount)) hits.add(Rule.R_3_1);
        }

        // 3.1 повторы ВНУТРИ одного сообщения: слова, фразы, предложения
        if (hasRepeats(words, cyr)) hits.add(Rule.R_3_1R);

        // 3.4 / 3.5
        if (isSymbolSpam(low)) hits.add(Rule.R_3_4);
        if (isCaps(msg)) hits.add(Rule.R_3_5);

        // 3.7 / 3.8
        // Проверяем несколько вариантов текста, чтобы поймать обходы:
        //  coll   — «схлопнутые» буквы (дебиииил)
        //  spaced — буквы через пробел/точки (д е б и л)
        //  tr     — латиница «по звучанию» в кириллицу (poshol nahui -> пошол нахуи, pidaras -> пидарас)
        String coll = collapse(cyr);
        String spaced = joinSpaced(cyr);
        String tr = translit(low);
        String trColl = collapse(tr);
        // Мат считается ПО ПРЕДЛОЖЕНИЯМ: два матерных слова должны быть в одном предложении (далеко друг от друга — можно).
        int mat = Math.max(Math.max(matInSentences(cyr), matInSentences(coll)), Math.max(matInSentences(tr), matInSentences(trColl)))
                + matScore(spaced);
        boolean insult = count(INSULT, cyr) > 0 || count(INSULT, coll) > 0 || count(INSULT, spaced) > 0
                || count(INSULT, tr) > 0 || count(INSULT, trColl) > 0
                || fuzzyInsult(cyr) || fuzzyInsult(coll) || fuzzyInsult(tr);
        if (mat >= Math.max(2, cfg.matThreshold)) hits.add(Rule.R_3_7);
        if (insult) hits.add(Rule.R_3_8);
        boolean rude = insult || mat > 0;

        // 3.9
        if (SUICIDE.matcher(cyr).find() || SUICIDE.matcher(tr).find()) hits.add(Rule.R_3_9);

        // 3.10 оскорбление сервера / 5.1 реклама другого сервера
        serverTalk(low, cyr, tr, hits);

        // 4.x
        if (BEG_PUNISH.matcher(cyr).find()) hits.add(Rule.R_4_1);
        if (BEG_RES.matcher(cyr).find()) hits.add(Rule.R_4_2);
        if (ALT_F4.matcher(low).find()) hits.add(Rule.R_4_4);
        if (containsAny(cfg.foreignProjects, low, cyr)) hits.add(Rule.R_4_5);

        // 5.x
        Matcher m = AD.matcher(low);
        while (m.find()) {
            String found = m.group();
            boolean allowed = false;
            for (String d : cfg.allowedDomains) {
                if (found.contains(d.toLowerCase(Locale.ROOT))) { allowed = true; break; }
            }
            if (!allowed) { hits.add(Rule.R_5_1); break; }
        }
        if (PARENT_STRONG.matcher(cyr).find() || (PARENT.matcher(cyr).find() && (rude || NEG.matcher(cyr).find()))) {
            hits.add(Rule.R_5_4);
        }
        parentTalk(cyr, tr, hits);

        // 6.x
        if (POLITICS.matcher(cyr).find()) hits.add(Rule.R_6_1);
        if (THREAT.matcher(cyr).find()) hits.add(Rule.R_6_3);
        if (rude && containsAny(cfg.staffWords, low, cyr)) hits.add(Rule.R_6_4);
        if (rude && containsAny(cfg.youtuberNames, low, cyr)) hits.add(Rule.R_6_6);

        List<Rule> out = new ArrayList<>(hits);
        out.sort((a, b) -> Integer.compare(b.severity(), a.severity()));
        return out;
    }

    private static final Set<String> STOP = Set.of("на", "не", "что", "это", "как", "так", "все", "его", "для", "или",
            "но", "по", "за", "из", "от", "до", "он", "она", "они", "мы", "вы", "ты", "же", "бы", "ли", "то", "там",
            "тут", "если", "при", "про", "ну", "да", "нет", "вот", "уже", "еще", "мне", "меня", "тебя", "мой", "моя");

    /** Повторы внутри сообщения: слово подряд / много раз, повтор фразы из 2-3 слов, повтор предложений. */
    private boolean hasRepeats(List<String> words, String cyr) {
        int n = words.size();

        // одно и то же слово подряд ("ха ха ха", "привет привет привет")
        int run = 1;
        for (int i = 1; i < n; i++) {
            if (sameWord(words.get(i), words.get(i - 1))) {
                if (++run >= cfg.repeatWordThreshold) return true;
            } else {
                run = 1;
            }
        }

        // одно слово много раз по всему тексту (с учётом искажений: "купи", "купиии", "kупи")
        List<String> reps = new ArrayList<>();
        List<Integer> cnt = new ArrayList<>();
        for (String w : words) {
            if (w.length() < 3 || STOP.contains(w)) continue;
            int idx = -1;
            for (int k = 0; k < reps.size(); k++) {
                if (sameWord(reps.get(k), w)) { idx = k; break; }
            }
            if (idx < 0) { reps.add(w); cnt.add(1); } else cnt.set(idx, cnt.get(idx) + 1);
        }
        for (int c : cnt) if (c >= cfg.repeatWordThreshold) return true;

        // повтор фразы ("да нет да нет да нет", "купи донат дёшево купи донат дёшево")
        if (ngramRepeats(words, 2, 3) || ngramRepeats(words, 3, 2)) return true;

        // повтор предложений
        List<String> sents = new ArrayList<>();
        for (String part : cyr.split("[.!?;\\n]+")) {
            String x = collapse(part.replaceAll("[^\\p{L}]", ""));
            if (x.length() >= 6) sents.add(x);
            if (sents.size() >= 20) break;
        }
        for (int i = 0; i < sents.size(); i++) {
            int same = 1;
            for (int j = 0; j < sents.size(); j++) {
                if (i != j && similar(sents.get(i), sents.get(j))) same++;
            }
            if (same >= cfg.repeatSentenceThreshold) return true;
        }
        return false;
    }

    private boolean ngramRepeats(List<String> words, int size, int threshold) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i + size <= words.size(); i++) {
            List<String> sub = words.subList(i, i + size);
            boolean allSame = true;
            for (String x : sub) if (!x.equals(sub.get(0))) { allSame = false; break; }
            if (allSame) continue;
            String key = String.join(" ", sub);
            if (key.length() < 6) continue;
            if (map.merge(key, 1, Integer::sum) >= threshold) return true;
        }
        return false;
    }

    private static boolean sameWord(String a, String b) {
        return a.equals(b) || similar(a, b);
    }

    /** Схлопывает повторяющиеся подряд буквы: "приветттт" -> "привет". */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        char prev = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != prev) sb.append(c);
            prev = c;
        }
        return sb.toString();
    }

    private static List<String> words(String cyr) {
        List<String> out = new ArrayList<>();
        for (String t : cyr.split("[^\\p{L}\\d]+")) {
            String c = collapse(t);
            if (c.length() >= 2) out.add(c);
        }
        return out;
    }

    /**
     * 3.5 (капс): либо N+ слов (от 3 букв) целиком заглавными, либо при 3+ словах 50%+ букв заглавные
     * и среди них минимум 2 слова от 3 букв целиком капсом. Короткие LL, ZZ, GG, EZ сами по себе капсом не считаются,
     * но в «AXAXAXXA 1vs2 HE ДОЖАЛИ» они учитываются в общей доле заглавных букв.
     */
    private boolean isCaps(String msg) {
        int minLen = Math.max(2, cfg.capsMinWordLength);
        int tokens = 0, letters = 0, upper = 0, capsWords = 0;
        for (String tok : msg.split("\\s+")) {
            int tl = 0, tu = 0;
            for (int i = 0; i < tok.length(); i++) {
                char ch = tok.charAt(i);
                if (Character.isLetter(ch)) {
                    tl++;
                    if (Character.isUpperCase(ch)) tu++;
                }
            }
            if (tl > 0) {
                tokens++;
                letters += tl;
                upper += tu;
                if (tl >= minLen && tu == tl) capsWords++;
            }
        }
        if (capsWords >= Math.max(2, cfg.capsWords)) return true;
        return tokens >= 3 && capsWords >= 2 && upper * 2 >= letters;
    }

    /** 3.4: !!!!, ////, и длинные «слова» из 1-3 разных символов (АХАХАХАХАХ, ПХПХПХПХХПХПХ, ыыыыыыыыыы). */
    private boolean isSymbolSpam(String low) {
        if (symbolSpam.matcher(low).find()) return true;
        for (String tok : low.split("\\s+")) {
            if (tok.length() < cfg.spamRunLength) continue;
            if (NUMBERISH.matcher(tok).matches()) continue;
            Set<Character> distinct = new HashSet<>();
            for (int i = 0; i < tok.length(); i++) distinct.add(tok.charAt(i));
            if (distinct.size() <= 3) return true;
        }
        return false;
    }

    private boolean similarMsg(String an, List<String> aw, String bn, List<String> bw) {
        if (an.equals(bn)) return true;
        int max = Math.max(an.length(), bn.length());
        if (max >= 6 && 1.0 - (double) levenshtein(an, bn) / max >= cfg.floodSimilarity) return true;
        return aw.size() >= 3 && bw.size() >= 3 && dice(aw, bw) >= cfg.floodWordSimilarity;
    }

    /** Доля совпавших слов (с поправкой на опечатки): 2*совпало / (слов в A + слов в B). */
    private static double dice(List<String> a, List<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        boolean[] used = new boolean[b.size()];
        int matched = 0;
        for (String x : a) {
            for (int j = 0; j < b.size(); j++) {
                if (!used[j] && sameWord(x, b.get(j))) {
                    used[j] = true;
                    matched++;
                    break;
                }
            }
        }
        return 2.0 * matched / (a.size() + b.size());
    }

    private static final Map<String, Character> DIGRAPHS = Map.of(
            "sh", 'ш', "ch", 'ч', "zh", 'ж', "kh", 'х', "ts", 'ц', "yo", 'е', "yu", 'ю', "ya", 'я', "ye", 'е');

    /** Латиница -> кириллица «по звучанию»: poshol nahui -> пошол нахуи, huesos -> хуесос. */
    private static String translit(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') {
                sb.append(c);
                i++;
                continue;
            }
            if (s.startsWith("shch", i)) { sb.append('щ'); i += 4; continue; }
            if (s.startsWith("sch", i)) { sb.append('щ'); i += 3; continue; }
            if (i + 1 < s.length()) {
                Character d = DIGRAPHS.get(s.substring(i, i + 2));
                if (d != null) { sb.append(d.charValue()); i += 2; continue; }
            }
            switch (c) {
                case 'a' -> sb.append('а');
                case 'b' -> sb.append('б');
                case 'c' -> sb.append('к');
                case 'd' -> sb.append('д');
                case 'e' -> sb.append('е');
                case 'f' -> sb.append('ф');
                case 'g' -> sb.append('г');
                case 'h' -> sb.append('х');
                case 'i' -> sb.append('и');
                case 'j' -> sb.append('й');
                case 'k' -> sb.append('к');
                case 'l' -> sb.append('л');
                case 'm' -> sb.append('м');
                case 'n' -> sb.append('н');
                case 'o' -> sb.append('о');
                case 'p' -> sb.append('п');
                case 'q' -> sb.append('к');
                case 'r' -> sb.append('р');
                case 's' -> sb.append('с');
                case 't' -> sb.append('т');
                case 'u' -> sb.append('у');
                case 'v' -> sb.append('в');
                case 'w' -> sb.append('в');
                case 'x' -> sb.append('х');
                case 'y' -> sb.append('й');
                default -> sb.append('з'); // z
            }
            i++;
        }
        return sb.toString();
    }

    private static boolean exactAny(String token) {
        for (Pattern p : MAT) if (p.matcher(token).lookingAt()) return true;
        for (Pattern p : INSULT) if (p.matcher(token).lookingAt()) return true;
        return false;
    }

    /** Слово похоже на корень с ошибкой не более чем в одной букве (долбаеб -> далбаеб). */
    private static boolean fuzzyRoot(String token, String root) {
        if (token.length() < root.length() - 1) return false;
        for (int len = root.length() - 1; len <= root.length() + 1; len++) {
            if (len > token.length()) break;
            if (levenshtein(token.substring(0, len), root) <= 1) return true;
        }
        return false;
    }

    /** Сколько слов — «почти мат» (не совпавших точно, но отличающихся на одну букву). */
    private static int fuzzyMat(String text) {
        int n = 0;
        for (String t : text.split("[^\\p{L}]+")) {
            if (t.length() < 6 || exactAny(t)) continue;
            for (String r : FUZZY_MAT) {
                if (fuzzyRoot(t, r)) { n++; break; }
            }
        }
        return n;
    }

    private static boolean fuzzyInsult(String text) {
        for (String t : text.split("[^\\p{L}]+")) {
            if (t.length() < 6 || exactAny(t)) continue;
            for (String r : FUZZY_INSULT) {
                if (fuzzyRoot(t, r)) return true;
            }
        }
        return false;
    }

    /**
     * 3.10: упоминание сервера/проекта (сервак, сервер, morecube...) и рядом (в пределах N слов) оскорбление/негатив.
     * 5.1: «лучший сервак, заходите на него» — приглашение на другой сервер, если в сообщении нет названия вашего проекта.
     */
    private void serverTalk(String low, String cyr, String tr, Set<Rule> hits) {
        String[] lowT = low.split("[^\\p{L}\\d]+");
        String[] cyrT = cyr.split("[^\\p{L}\\d]+");
        String[] trT = tr.split("[^\\p{L}\\d]+");
        int n = lowT.length;
        if (cyrT.length != n || trT.length != n || n == 0) return;

        boolean[] own = new boolean[n], foreign = new boolean[n], srv = new boolean[n];
        boolean[] neg = new boolean[n], invite = new boolean[n], qual = new boolean[n];
        boolean anyOwn = false;
        for (int i = 0; i < n; i++) {
            own[i] = nameMatch(cfg.projectNames, lowT[i], cyrT[i], trT[i]);
            foreign[i] = nameMatch(cfg.foreignProjects, lowT[i], cyrT[i], trT[i]);
            srv[i] = tokenMatch(SERVER_WORD, cyrT[i], trT[i]);
            neg[i] = tokenMatch(NEG_SERVER, cyrT[i], trT[i]) || tokenMatchList(INSULT, cyrT[i], trT[i]);
            invite[i] = tokenMatch(INVITE, cyrT[i], trT[i]);
            qual[i] = tokenMatch(PROMO_QUAL, cyrT[i], trT[i]);
            anyOwn |= own[i];
        }
        int win = Math.max(1, cfg.serverWindow);

        // 3.10
        for (int i = 0; i < n; i++) {
            if (!(srv[i] || own[i])) continue;
            for (int j = Math.max(0, i - win); j <= Math.min(n - 1, i + win); j++) {
                if (j != i && neg[j]) {
                    hits.add(Rule.R_3_10);
                    i = n;
                    break;
                }
            }
        }

        // 5.1
        if (anyOwn) return;
        for (int i = 0; i < n; i++) {
            if (!(srv[i] || foreign[i])) continue;
            boolean inv = false, q = foreign[i];
            for (int j = Math.max(0, i - win); j <= Math.min(n - 1, i + win); j++) {
                if (invite[j]) inv = true;
                if (qual[j]) q = true;
            }
            if (inv && q) {
                hits.add(Rule.R_5_1);
                return;
            }
        }
    }

    /** 5.4: «мать/мама/матуха/батя/папа...» и рядом (в пределах нескольких слов) оскорбление: «мать шлюха», «батя пидарас». */
    private void parentTalk(String cyr, String tr, Set<Rule> hits) {
        String[] cyrT = cyr.split("[^\\p{L}\\d]+");
        String[] trT = tr.split("[^\\p{L}\\d]+");
        int n = cyrT.length;
        if (trT.length != n || n == 0) return;
        boolean[] parent = new boolean[n];
        boolean[] bad = new boolean[n];
        for (int i = 0; i < n; i++) {
            parent[i] = tokenMatch(PARENT_WORD, cyrT[i], trT[i]);
            bad[i] = tokenMatch(PARENT_BAD, cyrT[i], trT[i]) || tokenMatchList(INSULT, cyrT[i], trT[i]);
        }
        for (int i = 0; i < n; i++) {
            if (!parent[i]) continue;
            for (int j = Math.max(0, i - PARENT_WINDOW); j <= Math.min(n - 1, i + PARENT_WINDOW); j++) {
                if (j != i && bad[j]) {
                    hits.add(Rule.R_5_4);
                    return;
                }
            }
        }
    }

    private static boolean tokenMatch(Pattern p, String... tokens) {
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            if (p.matcher(t).lookingAt() || p.matcher(collapse(t)).lookingAt()) return true;
        }
        return false;
    }

    private static boolean tokenMatchList(List<Pattern> list, String... tokens) {
        for (Pattern p : list) {
            if (tokenMatch(p, tokens)) return true;
        }
        return false;
    }

    private static boolean nameMatch(List<String> names, String lowTok, String cyrTok, String trTok) {
        for (String raw : names) {
            if (raw == null || raw.isBlank()) continue;
            String wl = raw.toLowerCase(Locale.ROOT).replace('ё', 'е').replace(" ", "");
            if (lowTok.contains(wl) || cyrTok.contains(toCyr(wl)) || trTok.contains(translit(wl))) return true;
        }
        return false;
    }

    /** Наибольшее число матерных «баллов» в одном предложении. */
    private static int matInSentences(String text) {
        int best = 0;
        for (String part : text.split("[.!?;\\n]+")) {
            if (part.isBlank()) continue;
            best = Math.max(best, matScore(part) + fuzzyMat(part));
        }
        return best;
    }

    private static int matScore(String text) {
        return count(MAT, text) + 2 * count(MAT_PHRASES, text);
    }

    private static String joinSpaced(String cyr) {
        Matcher m = SPACED.matcher(cyr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) sb.append(m.group().replaceAll("[^\\p{L}]", "")).append(' ');
        return sb.toString();
    }

    private static int count(List<Pattern> list, String text) {
        int n = 0;
        for (Pattern p : list) {
            Matcher m = p.matcher(text);
            while (m.find()) n++;
        }
        return n;
    }

    private static boolean containsAny(List<String> words, String low, String cyr) {
        for (String raw : words) {
            if (raw == null || raw.isBlank()) continue;
            String wl = raw.toLowerCase(Locale.ROOT).replace('ё', 'е');
            if (low.contains(wl) || cyr.contains(toCyr(wl))) return true;
        }
        return false;
    }

    /** Латинские «двойники» -> кириллица, чтобы ловить обход вроде "дeбил" с латинской e. */
    private static String toCyr(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'a' -> sb.append('а');
                case 'c' -> sb.append('с');
                case 'e' -> sb.append('е');
                case 'o' -> sb.append('о');
                case 'p' -> sb.append('р');
                case 'x' -> sb.append('х');
                case 'y' -> sb.append('у');
                case 'k' -> sb.append('к');
                case 'm' -> sb.append('м');
                case 't' -> sb.append('т');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean similar(String a, String b) {
        if (a.equals(b)) return true;
        int max = Math.max(a.length(), b.length());
        if (max < 4) return false;
        int d = levenshtein(a, b);
        return 1.0 - (double) d / max >= 0.8;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
