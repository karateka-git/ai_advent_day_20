# ai_advent_day_19

РЈС‡РµР±РЅС‹Р№ MCP sandbox РЅР° Kotlin/Ktor СЃ РґРІСѓРјСЏ РєРѕРЅС‚СѓСЂР°РјРё:

- `stateless` reference РґР»СЏ РѕР±С‹С‡РЅС‹С… `tools/list` Рё `tools/call`;
- `stateful` push-РєРѕРЅС‚СѓСЂ РґР»СЏ СЃРµСЂРІРµСЂРЅС‹С… СѓРІРµРґРѕРјР»РµРЅРёР№ С‡РµСЂРµР· SSE.

РџСЂРѕРµРєС‚ РїРѕРєР°Р·С‹РІР°РµС‚, РєР°Рє РїРѕРІРµСЂС… РѕРґРЅРѕРіРѕ CLI Рё РѕРґРЅРѕРіРѕ agent/workflow-СЃР»РѕСЏ РїРѕРґРґРµСЂР¶РёРІР°С‚СЊ СЂР°Р·РЅС‹Рµ MCP runtime-РїРѕРґС…РѕРґС‹ Р±РµР· РґСѓР±Р»РёСЂРѕРІР°РЅРёСЏ РІСЃРµР№ РІРµСЂС…РЅРµР№ С‡Р°СЃС‚Рё РїСЂРёР»РѕР¶РµРЅРёСЏ.

## Р§С‚Рѕ СѓРјРµРµС‚ РїСЂРѕРµРєС‚

- РїРѕРґРЅРёРјР°С‚СЊ Р»РѕРєР°Р»СЊРЅС‹Р№ `stateless` MCP server;
- РїРѕРґРЅРёРјР°С‚СЊ РѕС‚РґРµР»СЊРЅС‹Р№ `stateful` MCP server;
- РїРѕРґРєР»СЋС‡Р°С‚СЊ РѕР±С‰РёР№ CLI-РєР»РёРµРЅС‚ Рє РѕР±РѕРёРј РєРѕРЅС‚СѓСЂР°Рј;
- РІС‹Р·С‹РІР°С‚СЊ РѕР±С‹С‡РЅС‹Рµ MCP tools РґР»СЏ `JSONPlaceholder`;
- Р·Р°РїСѓСЃРєР°С‚СЊ С„РѕРЅРѕРІС‹Р№ push-СЃС†РµРЅР°СЂРёР№ `start_random_posts`;
- РїРµС‡Р°С‚Р°С‚СЊ `notifications/random_post` РїСЂСЏРјРѕ РІ С‚РѕРј Р¶Рµ CLI.

## РўРµРєСѓС‰Р°СЏ Р°СЂС…РёС‚РµРєС‚СѓСЂР°

РџРѕР»СЊР·РѕРІР°С‚РµР»СЊСЃРєРёР№ РїРѕС‚РѕРє РѕСЃС‚Р°С‘С‚СЃСЏ РѕР±С‰РёРј:

`presentation -> workflow -> agent -> mcp`

Р Р°Р·РґРµР»РµРЅРёРµ РЅР°С‡РёРЅР°РµС‚СЃСЏ РІРЅСѓС‚СЂРё MCP-СЃР»РѕСЏ:

- `mcp/client/common` Рё `mcp/server/common`
  РћР±С‰РёРµ РјРѕРґРµР»Рё, tool-call РєРѕРЅС‚СЂР°РєС‚С‹ Рё РёРЅС‚РµРіСЂР°С†РёСЏ СЃ `JSONPlaceholder`.
- `mcp/client/stateless` Рё `mcp/server/stateless`
  Р‘Р°Р·РѕРІС‹Р№ reference-РєРѕРЅС‚СѓСЂ РЅР° Streamable HTTP.
- `mcp/client/stateful` Рё `mcp/server/stateful`
  РљРѕРЅС‚СѓСЂ СЃ stateful session lifecycle Рё РєР»Р°СЃСЃРёС‡РµСЃРєРёРј SSE transport.

РљР»СЋС‡РµРІР°СЏ РёРґРµСЏ:

- `tool posts` Рё `tool post <postId>` РёРґСѓС‚ РІ `stateless` СЃРµСЂРІРµСЂ;
- `tool start-random-posts [intervalMinutes]` РёРґС‘С‚ РІ `stateful` СЃРµСЂРІРµСЂ;
- CLI, workflow Рё agent РїСЂРё СЌС‚РѕРј РѕСЃС‚Р°СЋС‚СЃСЏ РѕР±С‰РёРјРё.

## Transport-РїРѕРґС…РѕРґ

- `stateless` РєРѕРЅС‚СѓСЂ РёСЃРїРѕР»СЊР·СѓРµС‚ Streamable HTTP.
- `stateful` РєРѕРЅС‚СѓСЂ РёСЃРїРѕР»СЊР·СѓРµС‚ РєР»Р°СЃСЃРёС‡РµСЃРєРёР№ SSE transport:
  - РєР»РёРµРЅС‚ РѕС‚РєСЂС‹РІР°РµС‚ SSE-СЃРµСЃСЃРёСЋ;
  - СЃРµСЂРІРµСЂ РґРµСЂР¶РёС‚ session-aware runtime;
  - push-СЃРѕР±С‹С‚РёСЏ РїСЂРёС…РѕРґСЏС‚ РєР°Рє `notifications/random_post`.

Р­С‚Рѕ СЃРґРµР»Р°РЅРѕ РЅР°РјРµСЂРµРЅРЅРѕ: Р±Р°Р·РѕРІС‹Р№ reference РЅРµ СѓСЃР»РѕР¶РЅСЏРµС‚СЃСЏ, Р° push-СЃС†РµРЅР°СЂРёР№ Р¶РёРІС‘С‚ СЂСЏРґРѕРј РєР°Рє РѕС‚РґРµР»СЊРЅС‹Р№ runtime.

## Р›РѕРєР°Р»СЊРЅС‹Рµ endpoints

- `stateless`: `http://127.0.0.1:3000/mcp`
- `stateful`: `http://127.0.0.1:3001/mcp`

## CLI-РєРѕРјР°РЅРґС‹

РџРѕСЃР»Рµ РїРѕРґРіРѕС‚РѕРІРєРё Р°РіРµРЅС‚Р° РґРѕСЃС‚СѓРїРЅС‹:

- `tool posts`
  РџРѕРєР°Р·Р°С‚СЊ РїРµСЂРІС‹Рµ 10 РїСѓР±Р»РёРєР°С†РёР№ РёР· `JSONPlaceholder`.
- `tool post <postId>`
  РџРѕРєР°Р·Р°С‚СЊ РѕРґРЅСѓ РїСѓР±Р»РёРєР°С†РёСЋ РїРѕ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂСѓ.
- `tool start-random-posts [intervalMinutes]`
  Р’РєР»СЋС‡РёС‚СЊ push СЃР»СѓС‡Р°Р№РЅС‹С… РїСѓР±Р»РёРєР°С†РёР№ РІ С‚РµРєСѓС‰СѓСЋ РєР»РёРµРЅС‚СЃРєСѓСЋ СЃРµСЃСЃРёСЋ.
  Р•СЃР»Рё РєРѕРјР°РЅРґР° РІС‹Р·С‹РІР°РµС‚СЃСЏ РїРѕРІС‚РѕСЂРЅРѕ, РёРЅС‚РµСЂРІР°Р» РѕР±РЅРѕРІР»СЏРµС‚СЃСЏ.
- `help`
  РџРѕРєР°Р·Р°С‚СЊ СЃРїРёСЃРѕРє РґРѕСЃС‚СѓРїРЅС‹С… РєРѕРјР°РЅРґ.
- `exit`
  Р—Р°РІРµСЂС€РёС‚СЊ РєР»РёРµРЅС‚СЃРєСѓСЋ СЃРµСЃСЃРёСЋ.

РћРіСЂР°РЅРёС‡РµРЅРёСЏ С‚РµРєСѓС‰РµРіРѕ СЌС‚Р°РїР°:

- РЅР° `stateful` СЃРµСЂРІРµСЂРµ РµСЃС‚СЊ С‚РѕР»СЊРєРѕ РѕРґРёРЅ tool: `start_random_posts`;
- РѕС‚РґРµР»СЊРЅРѕРіРѕ `stop_random_posts` РїРѕРєР° РЅРµС‚;
- РѕСЃС‚Р°РЅРѕРІРєР° push РїСЂРёРІСЏР·Р°РЅР° Рє Р·Р°РІРµСЂС€РµРЅРёСЋ РєР»РёРµРЅС‚СЃРєРѕР№ СЃРµСЃСЃРёРё.

## Pipeline

РќРёР¶Рµ РїСѓС‚СЊ РїРѕР»СЊР·РѕРІР°С‚РµР»СЊСЃРєРѕР№ РєРѕРјР°РЅРґС‹ С‡РµСЂРµР· РѕР±С‰РёР№ CLI Рё РґРІР° MCP-РєРѕРЅС‚СѓСЂР°.

### 1. РџРѕРґРіРѕС‚РѕРІРєР° Р°РіРµРЅС‚Р°

- [App.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/App.kt)
  Р—Р°РїСѓСЃРєР°РµС‚ `PrepareAgentCommand` РїРµСЂРµРґ РІС…РѕРґРѕРј РІ РёРЅС‚РµСЂР°РєС‚РёРІРЅС‹Р№ СЂРµР¶РёРј.
- [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt)
  Р”РµР»РµРіРёСЂСѓРµС‚ РїРѕРґРіРѕС‚РѕРІРєСѓ Р°РіРµРЅС‚Сѓ.
- [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt)
  РћР±С…РѕРґРёС‚ РёР·РІРµСЃС‚РЅС‹Рµ MCP-СЃРµСЂРІРµСЂС‹ Рё СЃРѕР±РёСЂР°РµС‚ capability snapshot.
- [RoutingMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/RoutingMcpClient.kt)
  РџРѕРґРєР»СЋС‡Р°РµС‚ `stateless` Рё `stateful` РєРѕРЅС‚СѓСЂС‹ С‡РµСЂРµР· СЂР°Р·РЅС‹Рµ client runtime.

Р РµР·СѓР»СЊС‚Р°С‚:

- CLI Р·РЅР°РµС‚, РєР°РєРёРµ РєРѕРјР°РЅРґС‹ СЂРµР°Р»СЊРЅРѕ РґРѕСЃС‚СѓРїРЅС‹;
- РµСЃР»Рё РѕРґРёРЅ РёР· СЃРµСЂРІРµСЂРѕРІ РЅРµРґРѕСЃС‚СѓРїРµРЅ, РєРѕРјР°РЅРґС‹ СЌС‚РѕРіРѕ РєРѕРЅС‚СѓСЂР° СЃРєСЂС‹РІР°СЋС‚СЃСЏ Рё РїРѕСЏРІР»СЏРµС‚СЃСЏ РїРѕРЅСЏС‚РЅРѕРµ РїСЂРµРґСѓРїСЂРµР¶РґРµРЅРёРµ.

### 2. РћР±С‹С‡РЅС‹Р№ stateless СЃС†РµРЅР°СЂРёР№

РџСЂРёРјРµСЂ:

```text
tool post 2
```

РџСѓС‚СЊ:

1. [DefaultCliCommandParser.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/presentation/cli/DefaultCliCommandParser.kt)
   Р Р°Р·Р±РёСЂР°РµС‚ РІРІРѕРґ РІ `ToolPostCommand`.
2. [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt)
   РџСЂРµРІСЂР°С‰Р°РµС‚ РµРіРѕ РІ `AgentRequest.CallAvailableCommand`.
3. [DefaultAgent.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/agent/DefaultAgent.kt)
   РќР°С…РѕРґРёС‚ command definition Рё РїРѕРЅРёРјР°РµС‚, С‡С‚Рѕ РєРѕРјР°РЅРґР° РёРґС‘С‚ РІ `stateless`.
4. [RoutingMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/RoutingMcpClient.kt)
   РћС‚РїСЂР°РІР»СЏРµС‚ РІС‹Р·РѕРІ РІ [StatelessMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/stateless/StatelessMcpClient.kt).
5. `StatelessMcpClient` РІС‹Р·С‹РІР°РµС‚ tool РЅР° [StatelessMcpServerApp.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateless/StatelessMcpServerApp.kt).
6. РћР±С‰РёР№ server-side tool РёР· `mcp/server/common/toolcall/tools/...` РІРѕР·РІСЂР°С‰Р°РµС‚ РѕР±С‹С‡РЅС‹Р№ СЂРµР·СѓР»СЊС‚Р°С‚.

### 3. Stateful push СЃС†РµРЅР°СЂРёР№

РџСЂРёРјРµСЂ:

```text
tool start-random-posts 1
```

РџСѓС‚СЊ:

1. [DefaultCliCommandParser.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/presentation/cli/DefaultCliCommandParser.kt)
   Р Р°Р·Р±РёСЂР°РµС‚ РІРІРѕРґ РІ `ToolStartRandomPostsCommand`.
2. [DefaultWorkflowCommandHandler.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/workflow/service/DefaultWorkflowCommandHandler.kt)
   Р”РµР»РµРіРёСЂСѓРµС‚ РІС‹Р·РѕРІ Р°РіРµРЅС‚Сѓ.
3. [ToolStartRandomPostsAgentCommandDefinition.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/agent/bootstrap/commands/ToolStartRandomPostsAgentCommandDefinition.kt)
   Р—Р°РєСЂРµРїР»СЏРµС‚ РјР°СЂС€СЂСѓС‚ РІ `stateful` РєРѕРЅС‚СѓСЂ.
4. [RoutingMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/RoutingMcpClient.kt)
   РџРµСЂРµРґР°С‘С‚ РІС‹Р·РѕРІ РІ [StatefulMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/stateful/StatefulMcpClient.kt).
5. `StatefulMcpClient` РґРµСЂР¶РёС‚ РґРѕР»РіРѕР¶РёРІСѓС‰СѓСЋ SSE-СЃРµСЃСЃРёСЋ Рё РІС‹Р·С‹РІР°РµС‚ tool РЅР° [StatefulMcpServerApp.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/StatefulMcpServerApp.kt).
6. [StartRandomPostsTool.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/toolcall/tools/startrandomposts/StartRandomPostsTool.kt)
   Р РµРіРёСЃС‚СЂРёСЂСѓРµС‚ РёР»Рё РѕР±РЅРѕРІР»СЏРµС‚ РїРѕРґРїРёСЃРєСѓ С‚РµРєСѓС‰РµР№ СЃРµСЃСЃРёРё.
7. [RandomPostTickerService.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/background/RandomPostTickerService.kt)
   РџРµСЂРёРѕРґРёС‡РµСЃРєРё РїРѕР»СѓС‡Р°РµС‚ СЃР»СѓС‡Р°Р№РЅС‹Р№ РїРѕСЃС‚ Рё С€Р»С‘С‚ `notifications/random_post`.
8. [App.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/App.kt)
   РЎР»СѓС€Р°РµС‚ push-СѓРІРµРґРѕРјР»РµРЅРёСЏ Рё РїРµС‡Р°С‚Р°РµС‚ `[push] ...` РІ С‚РѕС‚ Р¶Рµ CLI.

### 4. РљР»СЋС‡РµРІРѕРµ РѕС‚Р»РёС‡РёРµ РґРІСѓС… РїСѓС‚РµР№

- `stateless` РїСѓС‚СЊ Р·Р°РєР°РЅС‡РёРІР°РµС‚СЃСЏ СЃСЂР°Р·Сѓ РїРѕСЃР»Рµ РѕС‚РІРµС‚Р° РЅР° `tools/call`.
- `stateful` РїСѓС‚СЊ РїРѕСЃР»Рµ СѓСЃРїРµС€РЅРѕРіРѕ `tools/call` РїСЂРѕРґРѕР»Р¶Р°РµС‚ Р¶РёС‚СЊ РІ СЂР°РјРєР°С… РєР»РёРµРЅС‚СЃРєРѕР№ СЃРµСЃСЃРёРё.
- Р’ `stateful` СЃС†РµРЅР°СЂРёРё РІР°Р¶РµРЅ РЅРµ С‚РѕР»СЊРєРѕ СЂРµР·СѓР»СЊС‚Р°С‚ РєРѕРјР°РЅРґС‹, РЅРѕ Рё РїРѕСЃР»РµРґСѓСЋС‰РёРµ server-to-client notifications.

## РЎР±РѕСЂРєР°

Р‘Р°Р·РѕРІР°СЏ СЃР±РѕСЂРєР°:

```powershell
.\gradlew.bat build
```

РЎР±РѕСЂРєР° direct launcher-Р°СЂС‚РµС„Р°РєС‚РѕРІ:

```powershell
.\gradlew.bat installClientDist installServerDist installStatefulServerDist
```

РџРѕСЃР»Рµ СЌС‚РѕРіРѕ Р±СѓРґСѓС‚ РґРѕСЃС‚СѓРїРЅС‹:

- `build\install\mcp-client\bin\mcp-client.bat`
- `build\install\mcp-server\bin\mcp-server.bat`
- `build\install\mcp-stateful-server\bin\mcp-stateful-server.bat`

## РџСЂРµРґРїРѕС‡С‚РёС‚РµР»СЊРЅС‹Р№ Р·Р°РїСѓСЃРє

Р”Р»СЏ СЂСѓС‡РЅРѕР№ РїСЂРѕРІРµСЂРєРё РІ СЌС‚РѕРј РїСЂРѕРµРєС‚Рµ РїСЂРµРґРїРѕС‡РёС‚Р°РµС‚СЃСЏ scripted/direct launcher workflow.

РџРѕРґРіРѕС‚РѕРІРёС‚СЊ РїСЂРѕРµРєС‚ Рє СЂСѓС‡РЅРѕР№ РїСЂРѕРІРµСЂРєРµ:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

Р­С‚РѕС‚ СЃС†РµРЅР°СЂРёР№:

- СЃРѕР±РёСЂР°РµС‚ РїСЂРѕРµРєС‚;
- РїРѕРґРЅРёРјР°РµС‚ `stateless` СЃРµСЂРІРµСЂ;
- РїРѕРґРЅРёРјР°РµС‚ `stateful` СЃРµСЂРІРµСЂ;
- РѕС‚РєСЂС‹РІР°РµС‚ РєР»РёРµРЅС‚СЃРєРёР№ CLI.

Р—Р°РїСѓСЃРє СѓР¶Рµ СЃРѕР±СЂР°РЅРЅРѕРіРѕ РїСЂРѕРµРєС‚Р° Р±РµР· РЅРѕРІРѕРіРѕ build:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild
```

Headless-РІР°СЂРёР°РЅС‚ РґР»СЏ РІРѕСЃРїСЂРѕРёР·РІРѕРґРёРјРѕР№ РїСЂРѕРІРµСЂРєРё:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -Headless
```

РЎРєРІРѕР·РЅР°СЏ e2e-РїСЂРѕРІРµСЂРєР°:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1
```

## Р СѓС‡РЅРѕР№ Р·Р°РїСѓСЃРє РїРѕ РѕС‚РґРµР»СЊРЅРѕСЃС‚Рё

`stateless` СЃРµСЂРІРµСЂ:

```powershell
.\build\install\mcp-server\bin\mcp-server.bat
```

`stateful` СЃРµСЂРІРµСЂ:

```powershell
.\build\install\mcp-stateful-server\bin\mcp-stateful-server.bat
```

РљР»РёРµРЅС‚:

```powershell
.\build\install\mcp-client\bin\mcp-client.bat
```

РўРµС…РЅРёС‡РµСЃРєРёРµ Gradle entrypoint'С‹ С‚РѕР¶Рµ РµСЃС‚СЊ, РЅРѕ РѕРЅРё РІС‚РѕСЂРёС‡РЅС‹:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runStatefulServer
.\gradlew.bat runClient
```

## Р‘С‹СЃС‚СЂС‹Р№ СЃС†РµРЅР°СЂРёР№ РїСЂРѕРІРµСЂРєРё push

1. РџРѕРґРЅРёРјРё РїСЂРѕРµРєС‚ С‡РµСЂРµР·:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1
```

2. Р’ РѕРєРЅРµ РєР»РёРµРЅС‚Р° РІС‹РїРѕР»РЅРё:

```text
help
tool posts
tool post 1
tool start-random-posts 1
```

3. РћР¶РёРґР°РµРјРѕРµ РїРѕРІРµРґРµРЅРёРµ:

- `tool posts` Рё `tool post 1` СЂР°Р±РѕС‚Р°СЋС‚ С‡РµСЂРµР· `stateless` РєРѕРЅС‚СѓСЂ;
- `tool start-random-posts 1` Р°РєС‚РёРІРёСЂСѓРµС‚ push РґР»СЏ С‚РµРєСѓС‰РµР№ СЃРµСЃСЃРёРё;
- РґР°Р»СЊС€Рµ РїСЂРёРјРµСЂРЅРѕ СЂР°Р· РІ РјРёРЅСѓС‚Сѓ РІ С‚РѕРј Р¶Рµ CLI РїРѕСЏРІР»СЏСЋС‚СЃСЏ СЃС‚СЂРѕРєРё РІРёРґР°:

```text
[push] РЎР»СѓС‡Р°Р№РЅР°СЏ РїСѓР±Р»РёРєР°С†РёСЏ #...
```

4. РћСЃС‚Р°РЅРѕРІРєР°:

```text
exit
```

## Р’Р°Р¶РЅС‹Рµ С„Р°Р№Р»С‹

- [App.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/App.kt)
  РћР±С‰РёР№ CLI entrypoint.
- [McpProjectConfig.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/config/McpProjectConfig.kt)
  Р›РѕРєР°Р»СЊРЅС‹Рµ endpoints Рё РєРѕРЅС„РёРі СЃРµСЂРІРµСЂРѕРІ.
- [RoutingMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/RoutingMcpClient.kt)
  РњР°СЂС€СЂСѓС‚РёР·Р°С†РёСЏ РјРµР¶РґСѓ `stateless` Рё `stateful`.
- [StatelessMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/stateless/StatelessMcpClient.kt)
  РљР»РёРµРЅС‚ РґР»СЏ Р±Р°Р·РѕРІРѕРіРѕ reference-РєРѕРЅС‚СѓСЂР°.
- [StatefulMcpClient.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/client/stateful/StatefulMcpClient.kt)
  РљР»РёРµРЅС‚ СЃ РґРѕР»РіРѕР¶РёРІСѓС‰РµР№ SSE-СЃРµСЃСЃРёРµР№.
- [StatelessMcpServerApp.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateless/StatelessMcpServerApp.kt)
  Entry point `stateless` СЃРµСЂРІРµСЂР°.
- [StatefulMcpServerApp.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/StatefulMcpServerApp.kt)
  Entry point `stateful` СЃРµСЂРІРµСЂР°.
- [RandomPostTickerService.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/background/RandomPostTickerService.kt)
  Р¤РѕРЅРѕРІС‹Р№ ticker РґР»СЏ push СЃР»СѓС‡Р°Р№РЅС‹С… РїСѓР±Р»РёРєР°С†РёР№.
- [StartRandomPostsTool.kt](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/src/main/kotlin/ru/compadre/mcp/mcp/server/stateful/toolcall/tools/startrandomposts/StartRandomPostsTool.kt)
  Р•РґРёРЅСЃС‚РІРµРЅРЅС‹Р№ tool `stateful` СЃРµСЂРІРµСЂР°.

## Р”РѕРєСѓРјРµРЅС‚Р°С†РёСЏ

- [MemoryBank/README.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/MemoryBank/README.md)
  Р›РѕРєР°Р»СЊРЅР°СЏ РєР°СЂС‚Р° РїР°РјСЏС‚Рё РїСЂРѕРµРєС‚Р°.
- [MemoryBank/agent-preflight.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/MemoryBank/agent-preflight.md)
  Preflight-РїСЂР°РІРёР»Р° РїРµСЂРµРґ РЅРѕРІС‹РјРё РєСЂСѓРїРЅС‹РјРё Р±Р»РѕРєР°РјРё.
- [docs/technical-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/docs/technical-spec.md)
  Р‘Р°Р·РѕРІРѕРµ РўР— reference-С‡Р°СЃС‚Рё.
- [docs/stateful-random-post-push-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/docs/stateful-random-post-push-spec.md)
  РўР— РґР»СЏ stateful push-РєРѕРЅС‚СѓСЂР°.
- [docs/stateful-random-post-push-implementation-log.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_19/docs/stateful-random-post-push-implementation-log.md)
  Р–СѓСЂРЅР°Р» СЂРµР°Р»РёР·Р°С†РёРё stateful push-СЃС†РµРЅР°СЂРёСЏ.

