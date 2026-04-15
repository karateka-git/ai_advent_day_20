# ai_advent_day_20

Учебный проект по теме "День 19. Композиция MCP-инструментов".

Здесь реализован автоматический pipeline из нескольких MCP-tools, который запускается одной CLI-командой и проходит через общую цепочку:

`presentation -> workflow -> agent -> mcp`

## Задача

Нужно было показать композицию нескольких MCP-инструментов:

- первый инструмент получает данные;
- второй обрабатывает их;
- третий сохраняет результат;
- вся цепочка выполняется автоматически;
- данные корректно передаются между шагами.

В проекте это реализовано как summary-pipeline по случайным публикациям.

## Что реализовано

В проекте есть два MCP-контура:

- `stateless` сервер для обычных request/response tools;
- `stateful` сервер для session-based сценариев, push-уведомлений и summary-pipeline.

Основной пользовательский сценарий:

`tool summary posts <count> [strategy]`

Что делает команда:

1. MCP tool `pick_random_posts` получает `count` случайных публикаций из локального русскоязычного mock-каталога.
2. Агент по внутренней логике выбирает 3 публикации:
   - `long` — 3 самых длинных;
   - `short` — 3 самых коротких.
3. MCP tool `merge_posts` объединяет выбранные публикации в один summary.
4. MCP tool `save_summary` сохраняет summary в локальное файловое хранилище.

Дополнительные команды:

- `tool summaries` — показать все сохранённые summary;
- `tool summary saved <summaryId>` — показать одну сохранённую summary по короткому id, например `summary-1`;
- `tool start-random-posts [intervalMinutes]` — включить stateful push случайных публикаций;
- `tool posts` и `tool post <postId>` — reference-команды для stateless-контура.

## Архитектура

Пользовательский поток остаётся единым:

`presentation -> workflow -> agent -> mcp`

Роли слоёв:

- `presentation`
  Разбирает CLI-команды и форматирует результат.
- `workflow`
  Запускает пользовательский сценарий.
- `agent`
  Оркестрирует pipeline и принимает промежуточное решение о выборе 3 публикаций.
- `mcp`
  Предоставляет атомарные инструменты и не берёт на себя orchestration.

Разделение по серверам:

- `stateless` сервер
  Публикует:
  - `ping`
  - `echo`
  - `fetch_post`
  - `list_posts`
- `stateful` сервер
  Публикует:
  - `start_random_posts`
  - `pick_random_posts`
  - `merge_posts`
  - `save_summary`
  - `list_saved_summaries`
  - `get_saved_summary`

Маршрутизация команд:

- `tool posts`, `tool post <postId>` идут в `stateless` сервер;
- `tool summary posts <count> [strategy]`, `tool summaries`, `tool summary saved <summaryId>`, `tool start-random-posts [intervalMinutes]` идут в `stateful` сервер.

## Pipeline по шагам

Команда:

```text
tool summary posts 10 long
```

Выполнение:

1. CLI parser создаёт workflow-команду.
2. Workflow запускает pipeline.
3. Agent вызывает `pick_random_posts`.
4. Agent выбирает 3 публикации по стратегии `long` или `short`.
5. Agent вызывает `merge_posts`.
6. Agent вызывает `save_summary`.
7. CLI получает итоговый результат и показывает:
   - стратегию;
   - выбранные post ids;
   - заголовок summary;
   - short id сохранённой записи;
   - время сохранения.

Отдельная команда чтения хранилища:

```text
tool summaries
```

Отдельная команда чтения одной записи:

```text
tool summary saved summary-1
```

## Хранилище

Summary сохраняются в локальный JSON-файл:

`build/tmp/mcp-summary-storage/saved-summaries.json`

Это делает сценарий:

- воспроизводимым;
- независимым от внешней базы;
- удобным для ручной проверки и e2e.

## Локальные endpoints

- `stateless`: `http://127.0.0.1:3000/mcp`
- `stateful`: `http://127.0.0.1:3001/mcp`

## CLI-команды

- `tool posts`
  Показать первые 10 публикаций из `JSONPlaceholder`.
- `tool post <postId>`
  Показать одну публикацию по идентификатору.
- `tool summary posts <count> [strategy]`
  Запустить summary-pipeline.
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

## Сборка

Базовая сборка:

```powershell
.\gradlew.bat build
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

Особенность сценария:

- server windows открываются в PowerShell;
- client window открывается в `cmd` с `chcp 65001`, чтобы русский CLI-вывод на Windows отображался корректно.

Для запуска уже собранного проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild
```

## Ручной сценарий проверки

После запуска manual-check окружения можно проверить так:

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
- создание локального summary storage;
- чтение сохранённого результата.

## Ключевые файлы

- [DefaultCliCommandParser.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/presentation/cli/DefaultCliCommandParser.kt)
  Парсинг CLI-команд.
- [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt)
  Запуск пользовательских workflow-сценариев.
- [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt)
  Orchestration pipeline и выбор 3 публикаций.
- [StatefulMcpServerFactory.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/StatefulMcpServerFactory.kt)
  Регистрация stateful MCP-tools, включая summary-pipeline.
- [StatelessMcpServerFactory.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/mcp/server/stateless/StatelessMcpServerFactory.kt)
  Reference-набор stateless tools.
- [SummaryStorage.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/src/main/kotlin/ru/compadre/mcp/mcp/server/common/summarypipeline/storage/SummaryStorage.kt)
  Контракт локального хранилища.

## Документация

- [mcp-summary-pipeline-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/docs/mcp-summary-pipeline-spec.md)
- [mcp-summary-pipeline-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_20/docs/mcp-summary-pipeline-implementation-log.md)

## Ограничения текущего варианта

- summary-pipeline использует локальный mock-каталог, а не внешний API;
- отдельной команды удаления summary пока нет;
- выбор 3 публикаций детерминированный и rule-based, а не LLM-based;
- `stateful` контур совмещает push-сценарий и summary-pipeline, чтобы ручная проверка шла в одной живой серверной session.
