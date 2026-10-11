# Moderator Helper (Fabric 1.21.11, клиентский мод)

Сборка: JDK 21 + Gradle 8.14+ → в папке проекта `gradle build` → `build/libs/modhelper-1.2.0.jar`
в `.minecraft/mods` (нужны Fabric Loader 0.18.1+ и Fabric API для 1.21.11).

Если Gradle не находит версию в gradle.properties — возьми актуальные на https://fabricmc.net/develop

Команды: `/modhelper toggle`, `/modhelper reload`, `/modhelper ignore add|remove <ник>`, `/modhelper ignore list`
Настройки: `.minecraft/config/modhelper.json`
