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

Статус: в работе

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

- текущий коммит шага — structured contract для `list_posts` как source tool.

Следующий шаг:

- подготовить agent-level capability contract для multi-server availability команды `tool summary posts`.
