# ai_advent_day_20

Учебный проект по теме `Orchestration MCP`.

Проект демонстрирует длинный пользовательский flow, в котором агент:

- подготавливает capabilities нескольких MCP-серверов;
- выбирает нужные инструменты;
- маршрутизирует вызовы между серверами;
- выполняет один прикладной сценарий end-to-end.

Базовая архитектурная цепочка:

`presentation -> workflow -> agent -> mcp`

## Что показывает проект

В проекте есть два MCP-контура:

- `stateless` сервер для обычных request/response tools;
- `stateful` сервер для session-based сценариев, push-уведомлений и локального хранения summary.

Главный пользовательский сценарий:

`tool summary posts <count> [strategy]`

Этот сценарий теперь является именно multi-server flow:

1. агент вызывает `list_posts` на `stateless` сервере;
2. агент получает source data и выбирает 3 публикации по стратегии;
3. агент вызывает `merge_posts` на `stateful` сервере;
4. агент вызывает `save_summary` на `stateful` сервере;
5. пользователь получает единый итог pipeline.

После этого можно отдельно проверить сохранённый результат через:

- `tool summaries`
- `tool summary saved <summaryId>`

## Сервера и инструменты

`stateless` сервер публикует:

- `ping`
- `echo`
- `fetch_post`
- `list_posts`

`stateful` сервер публикует:

- `start_random_posts`
- `merge_posts`
- `save_summary`
- `list_saved_summaries`
- `get_saved_summary`

## Пользовательские команды

- `tool posts`
  Показать первые публикации из `JSONPlaceholder`.
- `tool post <postId>`
  Показать одну публикацию по идентификатору.
- `tool summary posts <count> [strategy]`
  Запустить cross-server summary pipeline.
  По умолчанию стратегия — `long`.
- `tool summaries`
  Показать все сохранённые summary.
- `tool summary saved <summaryId>`
  Показать одну сохранённую summary.
- `tool start-random-posts [intervalMinutes]`
  Включить push случайных публикаций в текущую клиентскую сессию.
- `help`
  Показать список доступных команд.
- `exit`
  Завершить клиентскую сессию.

## Как работает pipeline

Команда:

```text
tool summary posts 10 long
```

Выполнение:

1. CLI parser создаёт workflow-команду.
2. Workflow запускает pipeline через agent.
3. Agent вызывает `list_posts` на `stateless` сервере с `limit = count`.
4. Agent выбирает 3 публикации по стратегии:
   - `long` — 3 самых длинных;
   - `short` — 3 самых коротких.
5. Agent вызывает `merge_posts` на `stateful` сервере.
6. Agent вызывает `save_summary` на `stateful` сервере.
7. CLI показывает:
   - стратегию;
   - выбранные post ids;
   - заголовок summary;
   - short id сохранённой записи;
   - время сохранения.

Follow-up команды:

```text
tool summaries
tool summary saved summary-1
```

## Архитектурная идея

Роли слоёв:

- `presentation`
  Разбирает команды и форматирует результат.
- `workflow`
  Запускает прикладной сценарий.
- `agent`
  Оркестрирует flow, выбирает tools и определяет порядок вызовов.
- `mcp`
  Даёт атомарные инструменты и не содержит пользовательского orchestration.

Ключевая идея текущего решения:

- пользователь не выбирает сервер вручную;
- availability команды `tool summary posts` зависит от capabilities двух серверов;
- длинный flow использует инструменты с разных серверов в одном сценарии.

## Хранилище

Summary сохраняются в локальный JSON-файл:

`build/tmp/mcp-summary-storage/saved-summaries.json`

Это делает сценарий:

- воспроизводимым;
- удобным для ручной проверки;
- пригодным для scripted e2e.

## Локальные endpoints

- `stateless`: `http://127.0.0.1:3000/mcp`
- `stateful`: `http://127.0.0.1:3001/mcp`

## Сборка

Базовая сборка:

```powershell
.\gradlew.bat build
```

Полный прогон тестов:

```powershell
.\gradlew.bat test
```

Сборка direct launcher-артефактов:

```powershell
.\gradlew.bat installClientDist installServerDist installStatefulServerDist
```

После этого доступны:

- `build\install\mcp-client\bin\mcp-client.bat`
- `build\install\mcp-server\bin\mcp-server.bat`
- `build\install\mcp-stateful-server\bin\mcp-stateful-server.bat`

## Предпочтительный запуск

Для ручной проверки:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Для запуска уже собранного проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild
```

Особенность сценария:

- server windows открываются в PowerShell;
- client window открывается в `cmd` с `chcp 65001`, чтобы русский CLI-вывод на Windows отображался корректно.

## Ручной сценарий проверки

После запуска manual-check окружения:

1. `help`
2. `tool summary posts 10 long`
3. `tool summaries`
4. `tool summary saved summary-1`
5. `tool summary posts 10 short`
6. при желании `tool start-random-posts 1`
7. `exit`

## Автоматическая проверка

Полная headless e2e-проверка:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1
```

Что проверяет скрипт:

- сборку launcher-артефактов;
- запуск `stateless` и `stateful` серверов;
- выполнение `tool summary posts 10 long`;
- выполнение `tool summaries`;
- выполнение `tool summary saved <id>`;
- создание локального summary storage;
- чтение сохранённого результата.

## Ключевые файлы

- [DefaultCliCommandParser.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/presentation/cli/DefaultCliCommandParser.kt)
  Парсинг CLI-команд.
- [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt)
  Запуск пользовательских workflow-сценариев.
- [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt)
  Cross-server orchestration pipeline и выбор 3 публикаций.
- [StatelessMcpServerFactory.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/mcp/server/stateless/StatelessMcpServerFactory.kt)
  Регистрация `stateless` source tools.
- [StatefulMcpServerFactory.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/StatefulMcpServerFactory.kt)
  Регистрация `stateful` processing/persistence tools.
- [SummaryStorage.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/mcp/server/common/summarypipeline/storage/SummaryStorage.kt)
  Контракт локального хранилища.
- [check-e2e.ps1](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/scripts/check-e2e.ps1)
  Сквозная scripted-проверка multi-server сценария.

## Документация

- [orchestration-mcp-multi-server-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/docs/orchestration-mcp-multi-server-spec.md)
- [orchestration-mcp-multi-server-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/docs/orchestration-mcp-multi-server-implementation-log.md)
- [mcp-agent-capability-bootstrap-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/docs/mcp-agent-capability-bootstrap-spec.md)

## Ограничения текущего варианта

- выбор 3 публикаций остаётся rule-based, а не LLM-based;
- для source data используется `JSONPlaceholder`, поэтому сценарий зависит от доступности внешнего API;
- отдельной команды удаления summary пока нет;
- `stateful` контур совмещает push-сценарий и summary persistence в одном сервере.
