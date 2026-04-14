# Журнал реализации

Файл для краткой фиксации фактически выполненных действий по ТЗ `mcp-summary-pipeline-spec.md`.

Формат использования:

- что было целью этапа;
- какие шаги реально выполнены;
- какие решения приняты;
- что проверено;
- какими коммитами это зафиксировано;
- что остаётся следующим шагом.

## Этап 1. Контракт Pipeline И Документация

Статус: завершён

Цель этапа:

- зафиксировать новый пользовательский сценарий pipeline;
- создать отдельные документы для ТЗ и журнала реализации;
- подготовить письменную основу для последующих этапов разработки.

Выполненные действия:

1. Создано отдельное ТЗ `docs/mcp-summary-pipeline-spec.md`.
2. Создан отдельный implementation log `docs/mcp-summary-pipeline-implementation-log.md`.
3. В ТЗ зафиксированы:
   - команды `tool summary posts <count> [strategy]` и `tool summaries`;
   - стратегии `long` и `short`;
   - состав MCP-инструментов;
   - роль агента в выборе 3 публикаций;
   - требование к локальному файловому хранилищу.

Принятые решения:

- pipeline реализуется в orchestration-слое приложения, а не как один монолитный MCP-tool;
- выбор 3 публикаций выполняет агент, а не отдельный MCP-инструмент;
- локальное хранилище summary делается файловым и воспроизводимым для тестов.

Проверка:

- в `docs` появились отдельные документы для pipeline-ветки задачи;
- структура этапов и ожидаемый результат зафиксированы письменно.

Коммиты этапа:

- текущий коммит этапа — создание ТЗ и implementation log для MCP summary pipeline.

Следующий шаг:

- перейти к `Этапу 2` и реализовать server-side инструменты вместе с локальным хранилищем summary.

## Этап 2. MCP-Инструменты И Локальное Хранилище

Статус: завершён

Цель этапа:

- реализовать server-side инструменты для pipeline;
- добавить локальное файловое хранилище summary;
- подтвердить базовую работоспособность server-side тестами.

Выполненные действия:

1. Добавлены serializable-модели pipeline:
   - `SummaryPost`
   - `PostSelection`
   - `SummaryDraft`
   - `SavedSummary`
2. Реализовано файловое хранилище `FileSummaryStorage`.
3. Добавлены новые инструменты:
   - `pick_random_posts`
   - `merge_posts`
   - `save_summary`
   - `list_saved_summaries`
4. В `createStatelessMcpServer(...)` зарегистрированы новые MCP-tools.
5. Добавлены server-side тесты на:
   - файловое хранилище;
   - `pick_random_posts`;
   - `merge_posts`;
   - `save_summary`;
   - `list_saved_summaries`;
   - регистрацию новых инструментов в `stateless` сервере.
6. Выполнен прогон `.\gradlew.bat test`.

Принятые решения:

- structured output новых инструментов отдаётся через `structuredContent`, а человекочитаемая часть — через `TextContent`;
- локальное хранилище реализовано как JSON-файл в `build/tmp/mcp-summary-storage`;
- orchestration по-прежнему остаётся вне MCP server, а инструменты сохраняются атомарными.

Проверка:

- `stateless` server публикует все четыре новых инструмента;
- новые server-side тесты проходят;
- `.\gradlew.bat test` завершается успешно.

Коммиты этапа:

- текущий коммит этапа — server-side реализация summary pipeline tools и локального хранилища.

Следующий шаг:

- перейти к `Этапу 3` и реализовать orchestration pipeline в agent/workflow-слое.

## Этап 3. Agent-Level Pipeline

Статус: завершён

Цель этапа:

- реализовать автоматический pipeline в `agent/workflow`;
- перенести выбор 3 публикаций во внутреннюю логику агента;
- подготовить проектный контракт для передачи structured data между шагами.

Выполненные действия:

1. `McpToolCallResult` расширен полем `structuredContent`.
2. `StatelessMcpClient` начал сохранять `structuredContent` из SDK-результата.
3. В `AgentRequest` добавлен отдельный сценарий `RunSummaryPipeline`.
4. В `DefaultAgent` реализован orchestration pipeline:
   - вызов `pick_random_posts`;
   - выбор 3 публикаций по стратегии `long|short`;
   - вызов `merge_posts`;
   - вызов `save_summary`;
   - возврат итогового результата pipeline.
5. В workflow добавлена команда `ToolSummaryPostsCommand`.
6. `DefaultWorkflowCommandHandler` научился запускать summary-pipeline через агент.
7. Добавлены unit-тесты на:
   - сохранение нового client-side контракта;
   - orchestration pipeline в агенте;
   - workflow-сценарий запуска pipeline;
   - архитектурные контракты новой команды и agent-request.
8. Выполнен прогон `.\gradlew.bat test`.

Принятые решения:

- structured payload передаётся через `structuredContent`, а не через разбор строкового вывода;
- orchestration выполняется в агенте, а не в `workflow` и не в MCP server;
- итог pipeline возвращается как нормализованный `ToolCallSuccess`, чтобы не раздувать дерево response-моделей без необходимости.

Проверка:

- агент действительно вызывает несколько MCP-инструментов подряд;
- стратегия выбора 3 публикаций применяется внутри агента;
- `.\gradlew.bat test` завершается успешно.

Коммиты этапа:

- текущий коммит этапа — agent/workflow orchestration для summary pipeline.

Следующий шаг:

- перейти к `Этапу 4` и сделать pipeline доступным из CLI вместе с командой чтения хранилища.

## Этап 4. CLI-Интеграция

Статус: завершён

Цель этапа:

- сделать summary pipeline доступным из CLI;
- добавить команду просмотра сохранённых summary;
- включить новые сценарии в capability-based help.

Выполненные действия:

1. Добавлены новые user-facing команды:
   - `ToolSummaryPostsCommand`
   - `ToolSummariesCommand`
2. Расширен `DefaultCliCommandParser`:
   - `tool summary posts <count> [strategy]`
   - `tool summaries`
3. Добавлены новые `AgentCommandId`:
   - `TOOL_SUMMARY_POSTS`
   - `TOOL_SUMMARIES`
4. Добавлены новые command definitions:
   - `ToolSummaryPostsAgentCommandDefinition`
   - `ToolSummariesAgentCommandDefinition`
5. Для pipeline-команды добавлена отдельная definition с проверкой наличия всех требуемых MCP-tools.
6. `supportedAgentCommandDefinitions()` обновлён, поэтому после подготовки агента новые команды попадают в доступный help.
7. `DefaultWorkflowCommandHandler` расширен обработкой `tool summaries`.
8. Добавлены тесты на:
   - новый CLI parser;
   - capability resolver;
   - workflow-сценарий `tool summaries`;
   - архитектурные контракты новых команд.
9. Выполнен прогон `.\gradlew.bat test`.

Принятые решения:

- пользовательский pipeline запускается через отдельную workflow-команду, а не через `CallAvailableCommand`;
- при этом команда всё равно появляется в help, потому что её availability проверяется через custom command definition;
- команда `tool summaries` остаётся обычным single-tool сценарием через capability registry.

Проверка:

- parser принимает обе новые команды;
- capability-based help теперь может показывать pipeline и список сохранённых summary;
- `.\gradlew.bat test` завершается успешно.

Коммиты этапа:

- текущий коммит этапа — CLI-интеграция summary pipeline и команды чтения хранилища.

Следующий шаг:

- перейти к `Этапу 5` и обновить e2e-проверку под новый автоматический pipeline.
