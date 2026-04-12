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

Подготовка ручной проверки одним запуском:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Эта команда:

- собирает проект;
- поднимает сервер в отдельном окне PowerShell;
- дожидается готовности локального endpoint;
- открывает клиента в отдельном окне PowerShell.

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
- ТЗ на реализацию текущего этапа: [docs/technical-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/technical-spec.md)
- журнал выполненных шагов: [docs/implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/implementation-log.md)
