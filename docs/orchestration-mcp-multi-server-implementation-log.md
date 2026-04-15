# Журнал реализации

Файл для краткой фиксации фактически выполненных действий по ТЗ `orchestration-mcp-multi-server-spec.md`.

Формат использования:

- что было целью этапа;
- какие шаги реально выполнены;
- какие решения приняты;
- что проверено;
- какими коммитами это зафиксировано;
- что остаётся следующим шагом.

## Этап 1. Спецификация и фиксация контракта

Статус: завершён

Цель этапа:

- зафиксировать целевой multi-server сценарий orchestration;
- определить роли серверов, агента и пользовательского flow;
- подготовить проектную документационную базу для следующих этапов реализации.

Выполненные действия:

1. Создано отдельное ТЗ `docs/orchestration-mcp-multi-server-spec.md`.
2. Создан отдельный implementation log `docs/orchestration-mcp-multi-server-implementation-log.md`.
3. В ТЗ зафиксированы:
   - целевая multi-server модель;
   - длинный flow с `stateless` и `stateful` серверами;
   - требования к выбору tools и маршрутизации;
   - этапы реализации, сценарий проверки и критерии готовности.
4. В качестве source tool для cross-server flow зафиксирован `list_posts` на `stateless` сервере.
5. Отдельно зафиксировано, что `list_posts` на следующем этапе должен получить structured response для orchestration.

Принятые решения:

- основной демонстрационный сценарий строится вокруг команды `tool summary posts <count> [strategy]`;
- source tool должен жить на `stateless` сервере, а merge/save/read tools — на `stateful`;
- для роли source tool выбран существующий `list_posts`, а не новый отдельный инструмент;
- агент остаётся единственной точкой orchestration и выбора инструментов.

Проверка:

- в `docs` появились отдельные spec и implementation log для новой ветки работ;
- структура этапов и ожидаемый результат зафиксированы письменно;
- контракт `list_posts -> merge_posts -> save_summary` зафиксирован на уровне документации.

Коммиты этапа:

- `ae731ca` — создание spec и implementation log для multi-server orchestration;
- текущий коммит шага — фиксация source tool и expected cross-server order в документации.

Следующий шаг:

- перейти к этапу 2 и подготовить data/capability contracts для `list_posts` как source tool в cross-server flow.

## Этап 2. Контракты данных и capability discovery для flow

Статус: завершён

Цель этапа:

- подготовить data contract для source tool на `stateless` сервере;
- сделать `list_posts` пригодным для orchestration без парсинга текстового вывода;
- зафиксировать базу для capability-driven cross-server pipeline.

Выполненные действия:

1. `list_posts` расширен output schema для machine-readable ответа.
2. `list_posts` начал возвращать `structuredContent` со списком публикаций.
3. `StatelessMcpServerFactory` начал публиковать `outputSchema` для `list_posts`.
4. Обновлён unit-тест `ListPostsToolTest` с проверкой `structuredContent`.
5. `list_posts` получил необязательный аргумент `limit`, чтобы agent мог запрашивать нужный объём source data.
6. Обновлены server-side тесты для сценариев:
   - default limit;
   - explicit limit;
   - invalid limit.
7. Прогнаны релевантные тесты:
   - `ListPostsToolTest`
   - `StatelessMcpServerTest`
   - `StatelessMcpClientTest`

Принятые решения:

- source tool остаётся существующим `list_posts`, а не новым отдельным инструментом;
- для управления объёмом source data расширяется существующий tool, а не добавляется новый дублирующий endpoint;
- человекочитаемый текст у `list_posts` сохраняется для CLI, а orchestration опирается на `structuredContent`.

Проверка:

- targeted Gradle test run завершился успешно;
- `list_posts` теперь может использоваться как source tool для следующего шага cross-server orchestration;
- agent сможет передавать в source tool `count` как `limit`, не ломая обычную команду `tool posts`.

Коммиты этапа:

- `d773109` — structured contract для `list_posts` как source tool;
- `a9615b9` — поддержка `limit` у `list_posts` для orchestration flow.

Следующий шаг:

- перейти к этапу 3 и перевести `summary pipeline` на реальный cross-server flow.

## Этап 3. Реализация cross-server orchestration в agent/workflow

Статус: завершён

Цель этапа:

- перевести `summary pipeline` с single-server сценария на multi-server flow;
- сделать `list_posts` источником данных на `stateless` сервере;
- сохранить merge/save шаги на `stateful` сервере.

Выполненные действия:

1. `DefaultAgent` переведён на flow:
   - `list_posts` на `stateless`;
   - agent-side selection;
   - `merge_posts` на `stateful`;
   - `save_summary` на `stateful`.
2. Для source step агент передаёт `count` как `limit` в `list_posts`.
3. Добавлена проверка порядка вызовов и серверов в `DefaultAgentTest`.
4. Прогнаны тесты:
   - `DefaultAgentTest`
   - `DefaultWorkflowCommandHandlerTest`

Принятые решения:

- cross-server routing пока реализуется прямо в orchestration flow агента;
- availability команды будет донастроена отдельным этапом через command definitions и resolver.

Проверка:

- targeted Gradle test run завершился успешно;
- pipeline действительно использует `stateless` сервер как source и `stateful` как processing/persistence контур.

Коммиты этапа:

- `ac74c3b` — cross-server orchestration в `DefaultAgent`.

Следующий шаг:

- перейти к этапу 4 и обновить capability-based availability команды `tool summary posts`.

## Этап 4. Обновление routing-модели и command definitions

Статус: завершён

Цель этапа:

- сделать доступность `tool summary posts` зависимой от связки двух серверов;
- синхронизировать capability-based help с новым multi-server flow;
- сохранить обратную совместимость остальных команд.

Выполненные действия:

1. `ToolSummaryPostsAgentCommandDefinition` переведён на custom resolve вместо single-server `RequiredToolsAgentCommandDefinition`.
2. Команда `tool summary posts` теперь доступна только если:
   - на `stateless` сервере найден `list_posts`;
   - на `stateful` сервере найдены `merge_posts` и `save_summary`.
3. Обновлён `AvailableAgentCommandResolverTest`:
   - команда остаётся доступной при полном наборе cross-server capabilities;
   - команда скрывается при неполном pipeline-наборе tools.
4. Прогнаны тесты:
   - `AvailableAgentCommandResolverTest`
   - `DefaultAgentTest`

Принятые решения:

- для cross-server команды используется custom definition, а не усложнение общей routing-модели на этом шаге;
- `AvailableAgentCommand` для `tool summary posts` остаётся репрезентативной записью для help/availability, а не полным описанием всего orchestration path.

Проверка:

- targeted Gradle test run завершился успешно;
- capability-based availability теперь соответствует фактическому multi-server сценарию.

Коммиты этапа:

- `9f948f9` — multi-server availability для `tool summary posts`.

Следующий шаг:

- перейти к этапу 5 и усилить тесты на порядок вызовов, ошибки шагов и частично недоступные capabilities.

## Этап 5. Тесты на выбор и порядок вызовов

Статус: завершён

Цель этапа:

- подтвердить корректный порядок вызовов в multi-server pipeline;
- проверить негативные сценарии source-step и отсутствующих capabilities;
- зафиксировать поведение pipeline тестами до обновления e2e.

Выполненные действия:

1. В `DefaultAgentTest` уже проверяется порядок вызовов:
   - `list_posts` на `stateless`;
   - `merge_posts` на `stateful`;
   - `save_summary` на `stateful`.
2. Добавлен тест на остановку pipeline при source error.
3. Добавлен тест на понятную failure при отсутствии `stateful` pipeline-сервера.
4. Повторно прогнаны тесты:
   - `DefaultAgentTest`
   - `AvailableAgentCommandResolverTest`

Принятые решения:

- source-error сценарий завершается `ToolCallSuccess` с `isError = true`, чтобы сохранить единый контракт pipeline-result;
- отсутствие обязательного сервера трактуется как агентная `Failure`, а не как частичный tool result.

Проверка:

- targeted Gradle test run завершился успешно;
- тестовый контур теперь покрывает и happy path, и ключевые negative paths multi-server flow.

Коммиты этапа:

- `35eba6b` — negative-path тесты для multi-server pipeline.

Следующий шаг:

- перейти к этапу 6 и обновить scripted e2e так, чтобы он подтверждал новый cross-server сценарий end-to-end.

## Этап 6. Сквозная e2e-проверка

Статус: завершён

Цель этапа:

- подтвердить multi-server сценарий на реально поднятых серверах;
- усилить scripted e2e проверкой чтения конкретной сохранённой записи;
- стабилизировать полный тестовый контур проекта.

Выполненные действия:

1. `scripts/check-e2e.ps1` расширен третьим клиентским вызовом:
   - после `tool summary posts ...`;
   - после `tool summaries`;
   - теперь ещё выполняется `tool summary saved <summaryId>`.
2. В e2e добавлено извлечение `summaryId` из вывода pipeline-команды.
3. В e2e добавлены проверки вывода сохранённой записи.
4. `StatefulMcpClientTest` стабилизирован небольшим ожиданием перед tool-call, чтобы убрать race при подписке на `SharedFlow`.
5. Выполнены полные проверки:
   - `.\gradlew.bat test`
   - `powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1`

Принятые решения:

- e2e подтверждает не только создание summary, но и последующее чтение одной сохранённой записи;
- стабилизация flaky-теста оформлена как тестовая правка, а не как изменение production-кода.

Проверка:

- полный `test` завершился успешно;
- обновлённый e2e завершился успешно;
- end-to-end подтверждён сценарий:
  - `list_posts` на `stateless`;
  - `merge_posts` и `save_summary` на `stateful`;
  - последующее чтение `summary saved`.

Коммиты этапа:

- текущий коммит шага — усиление e2e и стабилизация полного тестового контура.

Следующий шаг:

- перейти к этапу 7 и синхронизировать README и документацию с фактическим multi-server сценарием.
