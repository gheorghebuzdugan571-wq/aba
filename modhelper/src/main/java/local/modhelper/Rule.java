package local.modhelper;

/** Одно правило из списка + наказание по умолчанию. minutes — длительность в минутах. */
public record Rule(String id, String title, boolean ban, int minutes, int severity, String detail) {

    public Rule(String id, String title, boolean ban, int minutes, int severity) {
        this(id, title, ban, minutes, severity, "");
    }


    public static final Rule R_3_1  = new Rule("3.1",  "Флуд",                       false, 30,    10, "похожие сообщения");
    public static final Rule R_3_1R = new Rule("3.1",  "Флуд",                       false, 30,    11, "повтор слов/фраз");
    public static final Rule R_3_4  = new Rule("3.4",  "Флуд символами",             false, 120,   20);
    public static final Rule R_3_5  = new Rule("3.5",  "Капс",                       false, 120,   20);
    public static final Rule R_3_7  = new Rule("3.7",  "Мат / аморальные высказывания", false, 120, 30);
    public static final Rule R_3_8  = new Rule("3.8",  "Оскорбления",                false, 120,   31);
    public static final Rule R_3_9  = new Rule("3.9",  "Призыв к суициду",           false, 360,   60);
    public static final Rule R_3_10 = new Rule("3.10", "Оскорбление проекта",        false, 72000, 100);
    public static final Rule R_4_1  = new Rule("4.1",  "Выпрашивание наказаний",     false, 120,   15);
    public static final Rule R_4_2  = new Rule("4.2",  "Выпрашивание ресурсов",      false, 60,    12);
    public static final Rule R_4_4  = new Rule("4.4",  "Обман (Alt+F4)",             false, 180,   25);
    public static final Rule R_4_5  = new Rule("4.5",  "Упоминание сторонних проектов", false, 4320, 46);
    public static final Rule R_4_6  = new Rule("4.6",  "Discord-контакты",           true,  2880,  50);
    public static final Rule R_5_1  = new Rule("5.1",  "Реклама сторонних проектов", true,  72000, 110);
    public static final Rule R_5_4  = new Rule("5.4",  "Оскорбление родителей",      false, 7200,  70);
    public static final Rule R_6_1  = new Rule("6.1",  "Политика и агитация",        false, 4320,  45);
    public static final Rule R_6_3  = new Rule("6.3",  "Угрозы мутом/баном",         true,  300,   35);
    public static final Rule R_6_4  = new Rule("6.4",  "Оскорбление администрации",  true,  7200,  80);
    public static final Rule R_6_6  = new Rule("6.6",  "Оскорбление ютубера проекта", true, 1440,  40);

    /** Время в формате сервера: 30m / 2h / 3d (суффиксы настраиваются в конфиге). */
    public String time(Config c) {
        if (minutes % 1440 == 0) return (minutes / 1440) + c.daySuffix;
        if (minutes % 60 == 0) return (minutes / 60) + c.hourSuffix;
        return minutes + c.minuteSuffix;
    }

    /** Причина для команды мута. */
    public String reason() {
        return id + " " + title;
    }

    /** Текст для тега в чате. */
    public String display() {
        String d = detail.isEmpty() ? "" : " (" + detail + ")";
        return id + " " + title + d + (ban ? " — БАН" : "");
    }
}
