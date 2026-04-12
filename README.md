# mcp-sandbox

Расширяемый sandbox-проект на Kotlin для знакомства с MCP и подготовки базы под будущие сценарии.

## Что уже реализовано

- локальный MCP server на Kotlin/Ktor;
- HTTP-based MCP endpoint `http://127.0.0.1:3000/mcp`;
- минимальный MCP client на официальном Kotlin SDK;
- базовый lifecycle `initialize -> tools/list`;
- вывод списка доступных инструментов в консоль;
- воспроизводимая e2e-проверка запуска.

Текущий демонстрационный набор инструментов:

- `ping`
- `echo`

## Транспорт

Проект использует Streamable HTTP transport. На текущем этапе сервер опубликован через stateless-вариант, потому что он проще для минимального reference-сценария и не требует отдельного session lifecycle на стороне сервера.

Такой выбор сохраняет главный архитектурный плюс: клиент работает с обычным HTTP endpoint, поэтому при переносе сервера наружу базовый способ подключения не нужно переписывать, обычно достаточно сменить URL и инфраструктурное окружение.

## Команды

Сборка проекта:

```powershell
.\gradlew.bat build
```

Запуск локального MCP server:

```powershell
.\gradlew.bat runServer
```

Запуск MCP client:

```powershell
.\gradlew.bat runClient
```

Внутри проекта эта задача теперь запускает явную клиентскую команду `connect`, которая отвечает за подключение к серверу, `initialize` и получение `tools/list`.

Подготовка ручной проверки одним запуском:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Эта команда:

- собирает проект;
- поднимает сервер в отдельном окне PowerShell;
- дожидается готовности локального endpoint;
- открывает клиента в отдельном окне PowerShell.

Для этого репозитория пользовательская фраза `собери проект` по умолчанию трактуется именно как этот workflow ручной проверки, а не как один только `.\gradlew.bat build`.

Если нужно запустить уже собранные артефакты без новой сборки, можно использовать:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild
```

Для этого репозитория пользовательская фраза `запусти проект` по умолчанию трактуется именно как этот вариант без сборки.

Сквозная end-to-end проверка:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1
```

## Ожидаемый результат

После запуска сервера и клиента базовый сценарий должен:

- установить соединение с `http://127.0.0.1:3000/mcp`;
- успешно пройти `initialize`;
- получить список инструментов через `tools/list`;
- вывести в консоль серверную информацию и инструменты `ping` и `echo`.

В успешной e2e-проверке скрипт дополнительно печатает `E2E check passed.`.

Если нужно прогнать тот же workflow без отдельных окон, можно использовать:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -Headless
```

## Документация

- проектный `MemoryBank`: [MemoryBank/README.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/MemoryBank/README.md)
- preflight для новых сессий: [MemoryBank/agent-preflight.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/MemoryBank/agent-preflight.md)
- ТЗ по MCP reference-части: [docs/technical-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/technical-spec.md)
- журнал реализации MCP reference-части: [docs/mcp-reference-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/mcp-reference-implementation-log.md)
- ТЗ по агентной архитектуре: [docs/agent-architecture-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/agent-architecture-spec.md)
- журнал реализации агентной архитектуры: [docs/agent-architecture-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/agent-architecture-implementation-log.md)
