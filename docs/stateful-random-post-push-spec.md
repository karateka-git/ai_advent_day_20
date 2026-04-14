# Техническое задание

## Название

Добавление параллельного `stateful` MCP-контура с SSE push-сценарием `start_random_posts`.

## Цель

Добавить в проект отдельный `stateful` runtime рядом с существующим `stateless` reference, чтобы пользователь мог через общий CLI:

1. оставить базовый MCP reference рабочим и простым;
2. запускать фоновый push-сценарий через `start_random_posts`;
3. получать `notifications/random_post` прямо в тот же клиентский CLI;
4. автоматически останавливать push при завершении клиентской сессии.

## Ключевая идея

Проект развивает не один универсальный transport, а два соседних контура:

- `stateless`
  Reference-контур для обычных `tools/list` и `tools/call`.
- `stateful`
  Отдельный session-aware контур для server push.

При этом:

- `presentation/cli`, `workflow` и `agent` остаются общими;
- различия между подходами живут в `mcp/client` и `mcp/server`;
- общие сущности выносятся в `common`.

## Граница текущего этапа

В этот проход входят:

- отдельный `stateful` server/client runtime;
- `stateful` endpoint на отдельном локальном порту;
- классический SSE transport для server-to-client notifications;
- tool `start_random_posts(intervalMinutes = 5)`;
- одна активная random-post подписка на одну клиентскую сессию;
- JSON payload для push-события;
- вывод push-событий в тот же CLI.

В этот проход не входят:

- полный перевод всего проекта на `stateful`;
- отдельный `stop_random_posts`;
- универсальный scheduler;
- персистентное хранилище задач;
- несколько параллельных подписок в одной сессии.

## Transport

- `stateless` контур остаётся на Streamable HTTP.
- `stateful` контур реализуется через классический SSE transport.

Причина такого решения:

- не ломаем минимальный reference-путь;
- получаем честный server push;
- не смешиваем учебный `stateless` сценарий и session-aware runtime.

## Routing-правило

Маршрутизация задаётся на уровне команды:

- `tool posts` и `tool post <postId>` идут в `stateless`;
- `tool start-random-posts [intervalMinutes]` идёт в `stateful`.

Выбор runtime не должен строиться на неявном fallback по доступности серверов.

## Push-контракт

Notification method:

```text
notifications/random_post
```

Payload первого этапа:

```json
{
  "message": "Случайная публикация #17: ..."
}
```

Требования:

- payload остаётся JSON-моделью, а не голым текстом;
- поле `message` обязательно;
- модель должна быть расширяемой без смены общей transport-схемы.

## Сессионная модель

- одна активная random-post подписка на одну клиентскую сессию;
- повторный `start_random_posts` не создаёт вторую подписку, а обновляет интервал;
- завершение клиентской сессии останавливает связанный ticker;
- reconnect трактуется как новая сессия, если отдельное восстановление не реализовано.

## Источник данных

Random post выбирается из `JSONPlaceholder`:

- диапазон `postId`: `1..100`;
- запрос выполняется через существующий `JsonPlaceholderApiClient`.

## Целевая структура

```text
src/main/kotlin/ru/compadre/mcp/
  presentation/
    cli/

  workflow/
    ...

  agent/
    ...

  mcp/
    client/
      McpClient.kt
      RoutingMcpClient.kt
      common/
      stateless/
      stateful/

    server/
      common/
      stateless/
      stateful/
```

Принцип для `common`:

- туда попадает только реально общее для `stateless` и `stateful`;
- stateful-specific session runtime, subscriptions и SSE wiring в `common` не выносятся.

## Этапы реализации

### Этап 1. Зафиксировать архитектуру и структуру

- оформить отдельное ТЗ;
- закрепить разделение `common/stateless/stateful`;
- зафиксировать общий CLI и отдельные runtime-контуры.

### Этап 2. Разложить MCP-слой

- перенести общие модели и tool-call контракты в `common`;
- выделить `stateless` и `stateful` пакеты;
- не сломать существующий reference.

### Этап 3. Поднять `stateful` runtime

- добавить `stateful` endpoint;
- реализовать `StatefulMcpClient`;
- реализовать `StatefulMcpServerApp`;
- добавить session registry.

### Этап 4. Добавить `start_random_posts`

- один tool на `stateful` сервере;
- `intervalMinutes` как `integer >= 1`;
- повторный start обновляет подписку.

### Этап 5. Добавить background ticker и push

- coroutine-based ticker на сервере;
- выбор случайного поста;
- отправка `notifications/random_post`;
- cleanup при завершении сессии.

### Этап 6. Встроить в общий CLI

- routing между `stateless` и `stateful`;
- команда `tool start-random-posts [intervalMinutes]`;
- вывод `[push] ...` в том же клиенте.

### Этап 7. Синхронизировать docs и launcher flow

- README;
- `MemoryBank`;
- manual-check scripts;
- implementation log.

## Acceptance criteria

- `stateless` контур продолжает работать как раньше;
- `stateful` сервер поднимается отдельно;
- `tool start-random-posts 1` активирует push в текущей клиентской сессии;
- в том же CLI появляются `notifications/random_post`;
- повторный `start_random_posts` обновляет интервал;
- при завершении клиентской сессии ticker останавливается;
- manual-check scripts поднимают оба сервера и клиент;
- документация описывает фактическую архитектуру и transport.
