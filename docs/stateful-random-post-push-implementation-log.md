# Журнал реализации

Файл фиксирует фактически выполненные шаги по ТЗ [stateful-random-post-push-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_18/docs/stateful-random-post-push-spec.md).

## Этап 1. Формализация ТЗ и архитектуры

Статус: завершён

Сделано:

- оформлено отдельное ТЗ для `stateful` push-контура;
- зафиксировано разделение `common/stateless/stateful`;
- принято решение не дублировать CLI, workflow и agent.

Результат:

- появилась согласованная архитектурная рамка для отдельного push-контура рядом с reference-частью.

## Этап 2. Разделение MCP-слоя

Статус: завершён

Сделано:

- клиентские общие модели и tool-call контракты вынесены в `mcp/client/common`;
- серверная интеграция с `JSONPlaceholder` и общие tool’ы вынесены в `mcp/server/common`;
- текущий reference-контур переведён в `mcp/client/stateless` и `mcp/server/stateless`.

Результат:

- кодовая база подготовлена к сосуществованию двух runtime-подходов без дублирования верхних слоёв.

## Этап 3. Добавление `stateful` runtime

Статус: завершён

Сделано:

- добавлен отдельный endpoint `http://127.0.0.1:3001/mcp`;
- реализованы `StatefulMcpClient`, `StatefulMcpServerApp` и `StatefulMcpServerFactory`;
- добавлен `ClientSessionRegistry`;
- в Gradle добавлены `runStatefulServer` и direct launcher `mcp-stateful-server`.

Важное решение:

- `stateful` контур реализован на классическом SSE transport, а не на Streamable HTTP.

Результат:

- появился отдельный session-aware серверный и клиентский runtime рядом с `stateless`.

## Этап 4. Реализация `start_random_posts`

Статус: завершён

Сделано:

- добавлены `RandomPostSubscription`, `RandomPostSubscriptionRegistry` и `StartRandomPostsTool`;
- закреплена семантика одной подписки на одну сессию;
- повторный start обновляет интервал;
- `intervalMinutes` валидируется как `integer >= 1`.

Результат:

- на `stateful` сервере появился первый и единственный tool: `start_random_posts`.

## Этап 5. Background ticker и push

Статус: завершён

Сделано:

- добавлен `RandomPostTickerService`;
- сервер стал периодически выбирать случайный `postId` и запрашивать пост через `JsonPlaceholderApiClient`;
- добавлена отправка `notifications/random_post`;
- клиентская common-модель уведомления сведена к `message`.

Дополнительное уточнение:

- из-за особенностей SDK payload пока адаптируется через `CustomNotification` и `_meta.message`, но внешний контракт для клиента остаётся стабильным.

Результат:

- `stateful` сервер умеет реально слать push-события в активную клиентскую сессию.

## Этап 6. Интеграция в общий CLI

Статус: завершён

Сделано:

- добавлен `RoutingMcpClient`;
- обновлены `App.kt`, parser, workflow и agent-команды;
- `tool start-random-posts [intervalMinutes]` встроен в общий CLI;
- push-сообщения печатаются в том же терминале.

Отдельный фикс по ходу:

- listener push-уведомлений вынесен из блокирующего CLI-loop в отдельную coroutine на `Dispatchers.IO`, иначе `readlnOrNull()` мешал выводу `[push] ...`.

Результат:

- пользователь работает с одним CLI, а routing между контурами скрыт под капотом.

## Этап 7. Launcher flow и документация

Статус: завершён

Сделано:

- manual-check scripts обновлены под запуск двух серверов и клиента;
- README переписан под фактическую архитектуру;
- `MemoryBank` и связанная документация синхронизированы с реальным transport-подходом.

Результат:

- проект можно запускать и проверять без расхождения между кодом, скриптами и документацией.

## Текущее состояние

Готово:

- `stateless` reference-контур;
- `stateful` SSE push-контур;
- routing через общий CLI;
- `start_random_posts`;
- `notifications/random_post`;
- manual-check flow для двух серверов.

Сознательно не сделано:

- отдельный `stop_random_posts`;
- универсальный scheduler;
- персистентное хранение подписок;
- расширенный structured payload сверх поля `message`.
