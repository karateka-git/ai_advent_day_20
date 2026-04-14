# ai_advent_day_19

Учебный MCP sandbox на Kotlin/Ktor с двумя контурами:

- `stateless` reference для обычных `tools/list` и `tools/call`;
- `stateful` push-контур для серверных уведомлений через SSE.

Проект показывает, как поверх одного CLI и одного `presentation -> workflow -> agent -> mcp` слоя поддерживать:

- обычные MCP tools;
- stateful push-сценарий;
- автоматический pipeline композиции нескольких MCP-инструментов.

## Что умеет проект

- поднимать локальный `stateless` MCP server;
- поднимать отдельный `stateful` MCP server;
- подключать общий CLI-клиент к обоим контурам;
- вызывать обычные MCP tools для публикаций;
- запускать фоновый push-сценарий `start_random_posts`;
- выполнять автоматический summary-pipeline из нескольких MCP-инструментов;
- сохранять результат pipeline в локальное файловое хранилище;
- выводить сохранённые summary обратно через отдельную CLI-команду.

## Текущая архитектура

Пользовательский поток остаётся общим:

`presentation -> workflow -> agent -> mcp`

Разделение начинается внутри MCP-слоя:

- `mcp/client/common` и `mcp/server/common`
  Общие модели, tool-call контракты и прикладная логика.
- `mcp/client/stateless` и `mcp/server/stateless`
  Базовый reference-контур на Streamable HTTP.
- `mcp/client/stateful` и `mcp/server/stateful`
  Контур с stateful session lifecycle и SSE transport.

Ключевая идея:

- `tool posts` и `tool post <postId>` идут в `stateless` сервер;
- `tool start-random-posts [intervalMinutes]`, `tool summary posts <count> [strategy]`, `tool summaries` и `tool summary saved <summaryId>` идут в `stateful` сервер;
- CLI, workflow и agent при этом остаются общими.

## Summary Pipeline

Новый пользовательский сценарий:

```text
tool summary posts 10 long
```

Pipeline выполняется автоматически:

1. MCP tool `pick_random_posts` получает `n` случайных публикаций из локального русскоязычного mock-каталога.
2. Агент по внутренней логике выбирает 3 публикации:
   - `long` — 3 самых длинных текста;
   - `short` — 3 самых коротких текста.
3. MCP tool `merge_posts` объединяет выбранные публикации в один summary.
4. MCP tool `save_summary` сохраняет summary в локальное файловое хранилище.
5. Команда `tool summaries` вызывает MCP tool `list_saved_summaries` и показывает всё сохранённое.

Это и есть демонстрация композиции MCP-инструментов:

- первый инструмент получает данные;
- агент принимает промежуточное решение;
- второй инструмент обрабатывает данные;
- третий инструмент сохраняет результат;
- четвёртый инструмент читает локальное хранилище.

## Локальные endpoints

- `stateless`: `http://127.0.0.1:3000/mcp`
- `stateful`: `http://127.0.0.1:3001/mcp`

## CLI-команды

После подготовки агента доступны:

- `tool posts`
  Показать первые 10 публикаций из `JSONPlaceholder`.
- `tool post <postId>`
  Показать одну публикацию по идентификатору.
- `tool summary posts <count> [strategy]`
  Запустить автоматический pipeline summary.
  По умолчанию стратегия — `long`.
- `tool summaries`
  Показать все summary, сохранённые в локальном хранилище.
- `tool summary saved <summaryId>`
  Показать одну сохранённую summary по короткому идентификатору вроде `summary-1`.
- `tool start-random-posts [intervalMinutes]`
  Включить push случайных публикаций в текущую клиентскую сессию.
- `help`
  Показать список доступных команд.
- `exit`
  Завершить клиентскую сессию.

## Pipeline-путь в коде

Основная summary-команда проходит по цепочке:

1. [DefaultCliCommandParser.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/presentation/cli/DefaultCliCommandParser.kt)
   Разбирает `tool summary posts <count> [strategy]`.
2. [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt)
   Запускает workflow-сценарий pipeline.
3. [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt)
   Оркестрирует вызовы MCP-tools и сам выбирает 3 публикации.
4. [StatefulMcpServerFactory.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/StatefulMcpServerFactory.kt)
   Регистрирует `start_random_posts`, `pick_random_posts`, `merge_posts`, `save_summary`, `list_saved_summaries`, `get_saved_summary`.
5. [SummaryStorage.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/common/summarypipeline/storage/SummaryStorage.kt)
   Сохраняет summary в локальный JSON-файл.

## Сборка

Базовая сборка:

```powershell
.\gradlew.bat build
```

Сборка direct launcher-артефактов:

```powershell
.\gradlew.bat installClientDist installServerDist installStatefulServerDist
```

После этого будут доступны:

- `build\install\mcp-client\bin\mcp-client.bat`
- `build\install\mcp-server\bin\mcp-server.bat`
- `build\install\mcp-stateful-server\bin\mcp-stateful-server.bat`

## Предпочтительный запуск

Для ручной проверки:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Headless e2e-проверка:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1
```

Этот сценарий:

- собирает свежие launcher distributions;
- поднимает `stateless` сервер;
- поднимает `stateful` сервер;
- выполняет `tool summary posts 10 long`;
- выполняет `tool summaries`;
- проверяет, что summary сохранён и читается из локального хранилища.

## Ручной запуск по отдельности

`stateless` сервер:

```powershell
.\build\install\mcp-server\bin\mcp-server.bat
```

`stateful` сервер:

```powershell
.\build\install\mcp-stateful-server\bin\mcp-stateful-server.bat
```

Клиент:

```powershell
.\build\install\mcp-client\bin\mcp-client.bat
```

Также доступны Gradle entrypoint'ы:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runStatefulServer
.\gradlew.bat runClient
```

## Документация по задаче

- [mcp-summary-pipeline-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/docs/mcp-summary-pipeline-spec.md)
- [mcp-summary-pipeline-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/docs/mcp-summary-pipeline-implementation-log.md)

## Ограничения текущего варианта

- summary-pipeline использует локальный mock-каталог, а не внешний API;
- отдельной команды удаления summary пока нет;
- `stateful` контур теперь совмещает push-сценарий `start_random_posts` и summary pipeline;
- push-сценарий останавливается при завершении клиентской сессии.
