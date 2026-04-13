# ai_advent_day_17

Расширяемый sandbox-проект на Kotlin для знакомства с MCP и подготовки базы под будущие агентные сценарии.

Сейчас проект решает базовую задачу:

- поднять локальный MCP server;
- подключить к нему клиент;
- пройти сценарий `initialize -> tools/list`;
- вывести серверную информацию и список доступных инструментов;
- вызвать прикладные инструменты поверх `JSONPlaceholder`.

Следующий зафиксированный сценарий развития:

- убрать пользовательский `connect` из основного CLI;
- перенести discovery возможностей в lifecycle агента;
- подключать агента к известным MCP-серверам на старте;
- показывать пользователю только те команды, которые агент реально нашёл в доступных MCP `tools`.

## Что внутри

- локальный MCP server на Kotlin/Ktor;
- HTTP endpoint `http://127.0.0.1:3000/mcp`;
- демонстрационные инструменты `ping` и `echo`;
- прикладные инструменты `fetch_post` и `list_posts`;
- интерактивный CLI-клиент;
- scripted smoke/e2e-проверка;
- direct launcher-запуск для Windows без Gradle progress UI.

## Текущая архитектура

Клиентская часть организована по цепочке:

`presentation -> workflow -> agent -> mcp`

Роли слоёв:

- `presentation` принимает пользовательский ввод и форматирует результат;
- `workflow` работает с командами и результатами сценария;
- `agent` оркестрирует выполнение команды через MCP;
- `mcp` инкапсулирует работу с Kotlin MCP SDK.

## Транспорт

Проект использует Streamable HTTP transport. На текущем этапе сервер опубликован через stateless-вариант, потому что он проще для минимального reference-сценария и не требует отдельного session lifecycle на стороне сервера.

## Быстрый старт

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

## Команды проекта

Технические Gradle entrypoint'ы:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runClient
```

Scripted-запуск клиента для smoke/e2e:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\invoke-client-commands.ps1 -Commands connect,exit
```

Подготовка ручной проверки одним запуском:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Для этого репозитория пользовательская фраза `собери проект` по умолчанию трактуется именно как этот workflow.

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

## Документация

- проектный `MemoryBank`: [MemoryBank/README.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/MemoryBank/README.md)
- preflight для новых сессий: [MemoryBank/agent-preflight.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/MemoryBank/agent-preflight.md)
- ТЗ по MCP reference-части: [docs/technical-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/technical-spec.md)
- журнал реализации MCP reference-части: [docs/mcp-reference-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-reference-implementation-log.md)
- ТЗ по агентной архитектуре: [docs/agent-architecture-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-architecture-spec.md)
- журнал реализации агентной архитектуры: [docs/agent-architecture-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-architecture-implementation-log.md)
- ТЗ по интеграции MCP tool: [docs/mcp-tool-integration-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-tool-integration-spec.md)
- журнал реализации интеграции MCP tool: [docs/mcp-tool-integration-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-tool-integration-implementation-log.md)
- контракт первого MCP tool: [docs/mcp-tool-integration-contract.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-tool-integration-contract.md)
- ТЗ по agent capability bootstrap: [docs/mcp-agent-capability-bootstrap-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-agent-capability-bootstrap-spec.md)
- capability model агента: [docs/mcp-agent-capability-model.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-agent-capability-model.md)
- журнал реализации agent capability bootstrap: [docs/mcp-agent-capability-bootstrap-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/mcp-agent-capability-bootstrap-implementation-log.md)
