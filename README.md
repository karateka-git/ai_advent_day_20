# ai_advent_day_17

Расширяемый sandbox-проект на Kotlin для знакомства с MCP и подготовки базы под будущие агентные сценарии.

Сейчас проект решает базовую задачу:

- поднять локальный MCP server;
- подключить к нему клиент;
- пройти сценарий `initialize -> tools/list`;
- вывести серверную информацию и список доступных инструментов.

## Что Внутри

- локальный MCP server на Kotlin/Ktor;
- HTTP endpoint `http://127.0.0.1:3000/mcp`;
- демонстрационные инструменты `ping` и `echo`;
- интерактивный CLI-клиент;
- scripted smoke/e2e-проверка;
- direct launcher-запуск для Windows без Gradle progress UI.

## Текущая Архитектура

Клиентская часть организована по цепочке:

`presentation -> workflow -> agent -> mcp`

Роли слоёв:

- `presentation` принимает пользовательский ввод и форматирует результат;
- `workflow` работает с командами и результатами сценария;
- `agent` оркестрирует выполнение команды через MCP;
- `mcp` инкапсулирует работу с Kotlin MCP SDK.

Текущий UI — CLI, но архитектура уже не завязана только на него. Это оставляет задел под Web, Android и будущие агентные расширения.

## Транспорт

Проект использует Streamable HTTP transport. На текущем этапе сервер опубликован через stateless-вариант, потому что он проще для минимального reference-сценария и не требует отдельного session lifecycle на стороне сервера.

Главный плюс такого выбора: клиент работает с обычным HTTP endpoint, поэтому при переносе сервера наружу обычно достаточно сменить URL и инфраструктурное окружение, а не переписывать сам подход к подключению.

## Быстрый Старт

Сборка проекта:

```powershell
.\gradlew.bat build
```

Сборка direct launcher-артефактов:

```powershell
.\gradlew.bat installClientDist installServerDist
```

После этого будут доступны:

- `build\install\mcp-client\bin\mcp-client.bat`
- `build\install\mcp-server\bin\mcp-server.bat`

Ручной запуск сервера:

```powershell
.\build\install\mcp-server\bin\mcp-server.bat
```

Ручной запуск клиента:

```powershell
.\build\install\mcp-client\bin\mcp-client.bat
```

Это основной способ ручной проверки. После старта клиент показывает приглашение и ждёт ручного ввода команд, например `connect`.

## Команды Проекта

Технические Gradle entrypoint-ы:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runClient
```

Они сохранены, но для ручного UX предпочтительнее direct launcher-артефакты, потому что в них нет `> Task`, daemon-сообщений и progress bar от Gradle.

Scripted-запуск клиента для smoke/e2e:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\invoke-client-commands.ps1 -Commands connect,exit
```

Подготовка ручной проверки одним запуском:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Эта команда:

- собирает проект и launcher-артефакты;
- поднимает сервер в отдельном окне;
- дожидается готовности endpoint;
- открывает интерактивного клиента в отдельном окне.

Для этого репозитория пользовательская фраза `собери проект` по умолчанию трактуется именно как этот workflow, а не как один только `.\gradlew.bat build`.

Запуск уже собранных артефактов без новой сборки:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild
```

Для этого репозитория пользовательская фраза `запусти проект` по умолчанию трактуется именно как этот вариант.

Сквозная end-to-end проверка:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1
```

Headless-вариант ручной проверки:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -Headless
```

В headless-режиме launcher сам подаёт scripted-команду `connect`, потому что интерактивный ввод в текущей консоли там не предполагается.

## Что Считать Успешным Результатом

После запуска сервера и клиента сценарий должен:

- установить соединение с `http://127.0.0.1:3000/mcp`;
- успешно пройти `initialize`;
- получить список инструментов через `tools/list`;
- вывести серверную информацию;
- показать инструменты `ping` и `echo`.

В успешной e2e-проверке скрипт дополнительно печатает `E2E check passed.`.

## Документация

- проектный `MemoryBank`: [MemoryBank/README.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/MemoryBank/README.md)
- preflight для новых сессий: [MemoryBank/agent-preflight.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/MemoryBank/agent-preflight.md)
- ТЗ по MCP reference-части: [docs/technical-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/technical-spec.md)
- журнал реализации MCP reference-части: [docs/mcp-reference-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-reference-implementation-log.md)
- ТЗ по агентной архитектуре: [docs/agent-architecture-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-architecture-spec.md)
- журнал реализации агентной архитектуры: [docs/agent-architecture-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-architecture-implementation-log.md)
