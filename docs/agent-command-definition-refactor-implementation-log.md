# Журнал реализации

Файл для краткой фиксации фактически выполненных действий по ТЗ `agent-command-definition-refactor-spec.md`.

Формат использования:

- что было целью этапа;
- какие шаги реально выполнены;
- какие решения приняты;
- что проверено;
- какими коммитами это зафиксировано;
- что остаётся следующим шагом.

## Этап 1. Формализация command-definition модели

Статус: завершён

Цель этапа:

- зафиксировать отдельное ТЗ на refactor capability-mapping;
- описать целевую модель command definitions, resolver и типизированных идентификаторов;
- подготовить отдельный unified implementation log под этот блок изменений.

Выполненные действия:

1. Создано отдельное ТЗ [agent-command-definition-refactor-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-command-definition-refactor-spec.md).
2. В ТЗ зафиксированы:
   - проблема текущего `buildAvailableAgentCommands(...)`;
   - целевая модель command-definition слоя;
   - требования к `agent/bootstrap/commands`;
   - ограничения текущего прохода, включая запрет на правку `README`.
3. В ТЗ выделены 4 последовательных этапа:
   - формализация;
   - внедрение command definitions;
   - типизация идентификаторов и routing;
   - интеграция и проверка.
4. Создан отдельный unified implementation log для этого ТЗ.

Принятые решения:

- вести этот рефакторинг отдельным ТЗ, а не смешивать его с предыдущим capability-bootstrap журналом;
- не трогать `README` на этом проходе;
- считать командные definitions основной точкой роста для capability-mapping.

Проверка:

- в `docs` присутствуют:
  - [agent-command-definition-refactor-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-command-definition-refactor-spec.md)
  - [agent-command-definition-refactor-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/docs/agent-command-definition-refactor-implementation-log.md)

Коммиты этапа:

- текущий коммит этапа — создание ТЗ и unified implementation log.

Следующий шаг:

- перейти к `Этапу 2` и заменить текущий `buildAvailableAgentCommands(...)` на explicit command definitions и resolver.

## Этап 2. Внедрение command-definition слоя

Статус: завершён

Цель этапа:

- вынести описание пользовательских команд из общего builder-а в отдельные command definitions;
- заменить top-level функцию построения команд на explicit resolver.

Выполненные действия:

1. Удалён старый [AgentCommandCatalog.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/AgentCommandCatalog.kt) с top-level builder-ом.
2. Введён новый пакет [agent/bootstrap/commands](</C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands>) со следующими ролями:
   - [AgentCommandDefinition.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/AgentCommandDefinition.kt) — базовый контракт command definition;
   - [ToolPostAgentCommandDefinition.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/ToolPostAgentCommandDefinition.kt) — definition команды `tool post <postId>`;
   - [ToolPostsAgentCommandDefinition.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/ToolPostsAgentCommandDefinition.kt) — definition команды `tool posts`;
   - [AvailableAgentCommandResolver.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/AvailableAgentCommandResolver.kt) — resolver доступных команд;
   - [SupportedAgentCommandDefinitions.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/SupportedAgentCommandDefinitions.kt) — точка сборки поддерживаемых definitions.
3. Добавлен общий support-класс [CommandDefinitionSupport.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/CommandDefinitionSupport.kt), который пока сохраняет текущее простое правило resolve по наличию нужного tool на сервере.
4. [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt) переведён с builder-а на `AvailableAgentCommandResolver`.
5. Добавлен локальный unit-тест [AvailableAgentCommandResolverTest.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/test/kotlin/ru/compadre/mcp/agent/bootstrap/commands/AvailableAgentCommandResolverTest.kt), который проверяет:
   - построение доступных команд из набора definitions;
   - пропуск команд, которые не могут разрешиться.
6. Выполнен прогон `.\gradlew.bat test`.

Принятые решения:

- сначала разделить ответственности между definition и resolver, не меняя ещё формат идентификаторов;
- сохранить текущее фактическое поведение выбора сервера на этом этапе, чтобы следующий этап был посвящён именно типизации и routing policy;
- вынести definitions в отдельный пакет `commands`, чтобы новая модель была видна структурно, а не только по именам классов.

Проверка:

- `DefaultAgent` строит `availableCommands` через `AvailableAgentCommandResolver`;
- top-level builder в старом виде удалён;
- `.\gradlew.bat test` завершается успешно.

Коммиты этапа:

- текущий коммит этапа — внедрение command-definition слоя и resolver.

Следующий шаг:

- перейти к `Этапу 3` и заменить строковые `commandId` и `serverId` на типизированные идентификаторы с явным routing у command definitions.

## Этап 3. Типизация `commandId` и `serverId` и явный routing

Статус: завершён

Цель этапа:

- убрать raw string `commandId` и `serverId` из capability-потока;
- сделать правило выбора сервера явной частью command definition.

Выполненные действия:

1. Добавлены новые типизированные модели:
   - [AgentCommandId.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/models/AgentCommandId.kt)
   - [McpServerId.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/models/McpServerId.kt)
2. Введена модель routing policy:
   - [CommandRouting.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/models/CommandRouting.kt)
3. Capability-модели переведены на типизированные идентификаторы:
   - `AvailableAgentCommand.commandId`
   - `AvailableAgentCommand.serverId`
   - `KnownMcpServer.serverId`
   - `PreparedMcpServer.serverId`
4. `AgentRequest.CallAvailableCommand` и `AgentCapabilityRegistry.availableCommand(...)` переведены на `AgentCommandId`.
5. `ToolBasedAgentCommandDefinition` теперь требует `routing` и разрешает команду через него, а не через неявный поиск первого подходящего сервера.
6. `ToolPostAgentCommandDefinition` и `ToolPostsAgentCommandDefinition` закреплены за `CommandRouting.FixedServer(McpServerId.LOCAL_MCP_SERVER)`.
7. [McpProjectConfig.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/config/McpProjectConfig.kt) теперь создаёт known servers с `McpServerId.LOCAL_MCP_SERVER`.
8. [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt) переведён на `AgentCommandId.TOOL_POST` и `AgentCommandId.TOOL_POSTS`.
9. [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_17/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt) обновлён под типизированные id и формирует прикладные error messages через стабильное строковое представление `AgentCommandId`.
10. Обновлены unit-тесты под новые типы и новое routing-поведение.
11. Выполнен прогон `.\gradlew.bat test`.

Принятые решения:

- использовать `enum class AgentCommandId` для фиксированного набора пользовательских команд;
- использовать `@JvmInline value class McpServerId` для typed server id без жёсткого ограничения только enum-значениями;
- на текущем проходе закрепить команды за `FixedServer`, чтобы перенос команды на другой сервер менялся в одном месте — внутри её definition.

Проверка:

- в agent/workflow capability-потоке больше нет raw string `commandId`;
- definitions используют явный `routing`;
- `.\gradlew.bat test` завершается успешно.

Коммиты этапа:

- текущий коммит этапа — типизация id и внедрение явного routing для command definitions.

Следующий шаг:

- перейти к `Этапу 4` и добить интеграцию, локальные тесты и финальную фиксацию журнала этого ТЗ.
