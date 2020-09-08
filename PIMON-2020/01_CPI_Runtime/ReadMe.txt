
Видеозапись выступления: https://youtu.be/IFPZZ6yG1Tc

***** Инструменты
Среда выполнения и основные инструменты
- SAP CPI тенант: SAP CPI Trial в Cloud Foundry (Amazon Web Services - Europe (Frankfurt))
- IDE / редактор кода: IntelliJ IDEA Community Edition, Visual Studio Code
- Shell: Windows Terminal - PowerShell, Windows Subsystem Linux (WSL) - Ubuntu
- HTTP клиент: Postman, cURL

Система сборки Java приложений и управления зависимостями: Gradle
Плагины для Gradle:
- groovy - стандартный
- biz.aQute.bnd.builder (bnd) - для сборки OSGi пакетов

Дополнительные библиотеки:
- Apache Commons CSV - в демо использовалась версия 1.8 (org.apache.commons:commons-csv:1.8)

Дополнительные Linux инструменты:
- tmux - расширенный терминал, несущественно для демо
- curl - вызывается из Bash-скриптов в демо

Локальная инстанция Apache Karaf:
- OSGi контейнер: Apache Karaf (см. https://camel.apache.org/download/)
- Дополнительно установленные features для Apache Camel (см. https://camel.apache.org/camel-karaf/latest/index.html)



***** Вспомогательные Bash-скрипты и iFlow
*** Выполнение OS Shell и Karaf Shell команд в CPI тенанте:
- iFlow: ShellCommandRunner
- Bash-скрипт: sap-cpi-shell
- Конфигурационный файл: ~/.config/sap/sap-cpi-env-shell.conf

Пример содержимого конфигурационного файла:

runtime_node_address="7c177dbatrial.it-cpitrial02-rt.cfapps.eu10-001.hana.ondemand.com"
api_path="/tools/shell-command"
username="{Имя пользователя}"
password="{Пароль}"

Параметры скрипта:
- type - Тип команды (OS / OSGi). Допустимые значения: os, osgi
- command - команда для выполнения

Примеры использования скрипта:
sap-cpi-shell --type=os --command="ps -aux"
sap-cpi-shell --type=os --command="lsb_release -a"
sap-cpi-shell --type=os --command="df -h"
sap-cpi-shell --type=os --command="ls -lha /"
sap-cpi-shell --type=osgi --command="bundle:list -l"
sap-cpi-shell --type=osgi --command="bundle:find-class org.apache.commons.csv.CSVPrinter"
sap-cpi-shell --type=osgi --command="bundle:tree-show 123"
sap-cpi-shell --type=osgi --command="bundle:tree-show 123 | grep -i csv"
sap-cpi-shell --type=osgi --command="bundle:headers 123"
sap-cpi-shell --type=osgi --command="bundle:headers 123 | grep Bundle-ClassPath"
sap-cpi-shell --type=osgi --command="bundle:classes 123"
sap-cpi-shell --type=osgi --command="bundle:classes 123 | grep 'exported: true'"
(где 123 - пример номера задеплоенного OSGi пакета)

Альтернативно, вместо использования Bash-скрипта, можно вызывать iFlow путем отправки HTTP GET запроса на соответствующую конечную точку (одна точка - интеграционного процесса для выполнения OS команд, другая - интеграционного процесса для выполнения OSGi команд) и указывать команду для выполнения с помощью URL параметра 'command'.


*** Динамическое выполнение Groovy скриптов в CPI тенанте:
- iFlow: GroovyScriptRunner
- Bash-скрипт: sap-cpi-script
- Конфигурационный файл: ~/.config/sap/sap-cpi-env-script.conf

Пример содержимого конфигурационного файла:

runtime_node_address="7c177dbatrial.it-cpitrial02-rt.cfapps.eu10-001.hana.ondemand.com"
api_path="/tools/script/groovy"
username="{Имя пользователя}"
password="{Пароль}"

Параметры скрипта:
- script - полный путь к файлу с Groovy скриптом
- function - имя Groovy функции, которую необходимо выполнить в Groovy скрипте. Параметр необязателен - если он не указан, будет использоваться имя функции по умолчанию (processData)

Примеры использования скрипта:
sap-cpi-script --script=/mnt/d/workspace/groovy/script1.groovy
sap-cpi-script --script=/mnt/d/workspace/groovy/script2.groovy --function=processData
sap-cpi-script --script=/mnt/d/workspace/groovy/script3.groovy --function=getCamelEndpoints

Альтернативно, вместо использования Bash-скрипта, можно вызывать iFlow путем отправки HTTP POST запроса на соответствующую конечную точку и указывать:
- URL параметр 'function' - имя Groovy функции
- Тело сообщения - содержимое Groovy скрипта (не ссылка на файл с Groovy скриптом, а исходный код Groovy скрипта!)



***** Демо с Camel Blueprint XML OSGi пакетом, развертывание в локальной инстанции Karaf
Файл с описанием Camel маршрута - demo-camel-http-to-file.xml.
Поместить файл в папку /deploy (по умолчанию) Karaf инстанции для "горячего" деплоя.
Новый маршрут должен быть доступен для работы.
Можно проверить доступность маршрута - Karaf команда camel:route-list.
Также можно проверить доступность конечных точек маршрута (endpoint) - Karaf команда camel:endpoint-list.
Если маршрут недоступен, см. журнал сообщений - Karaf команда log:display.
Маршрут:
- Вызов - отправить HTTP POST запрос на http://localhost:8282/demo/{имя файла} с любым телом запроса.
- Результат: файл будет создан в папке D:\tmp, отправитель получит сообщение об успешном создании файла в HTTP ответе.



***** Демо с двумя JAR OSGi пакетами, развертывание в локальной инстанции Karaf
Сборка пакетов: команда gradle build (или gradle clean build, если нужно удалить артефакты предыдущей сборки). Т.к. сборка мульти-проектная (каждый OSGi пакет - отдельный проект), команды Gradle должны выполняться с уровня "корневого" проекта (с верхнего уровня) - это поможет вызывать сборку одновременно для обоих проектов-модулей.
Результат сборки: два JAR файла с соответствующими OSGi пакетами:
- /one/build/libs/one-1.0.0.jar
- /two/build/libs/two-1.0.0.jar
Перед деплоем JAR файлов, выполнить Karaf команду log:tail для непрерывного просмотра актуальных записей в журнале (опционально, можно перед этим очистить журнал сообщений - Karaf команда log:clear).
Поместить JAR файлы по очереди в папку /deploy (по умолчанию) Karaf инстанции для "горячего" деплоя:
1. Файл one-1.0.0.jar
2. Файл two-1.0.0.jar
В журнале сообщений будут видны записи от сервиса развертывания и записи, сгенерированные вызванными Activator классами пакетов. В т.ч. последствия использования вторым пакетом класса Randomizer, экспортированного первым пакетом и импортированного вторым.



***** Демо с iFlow в CPI
Конечная точка /demo/months (например, отправить HTTP GET запрос) - список месяцев в CSV. Ошибка, если в Groovy скрипте в iFlow используется CSVPrinter.close(true) См. https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVPrinter.html - CSVPrinter.close(Boolean) появился в версии 1.6.
Конечная точка /demo/months/csvprinter-location (например, отправить HTTP GET запрос) - информация о местонахождении ресурса, из которого загрузчик классов демо iFlow загрузил CSVPrinter.
