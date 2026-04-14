# Agent Preflight

Короткий operational summary для работы в этом проекте.

Использование:

- в новой сессии после ознакомления с локальным и общим `MemoryBank`;
- перед новым смысловым блоком, если дальше ожидаются новые правки в коде или файлах.

## Обязательная проверка перед новыми правками

1. Ознакомиться с локальным `MemoryBank` проекта.
2. Ознакомиться с общим `MemoryBank`.
3. Свериться с этим preflight-документом.
4. Проверить `git status`, если впереди новый смысловой блок с правками.
5. Если есть незакоммиченные изменения и начинается новый смысловой блок с новыми правками, напомнить пользователю про коммит и дождаться явного решения.
6. Перед началом нового крупного смыслового блока коротко отметить в commentary, что выполнена сверка с `Agent Preflight` или `MemoryBank`, если это реально помогает пользователю понять смену контекста. Не повторять это перед каждой маленькой итерацией, локальной проверкой или шагом отладки.

## Критичные правила проекта

- Держать минимальный сценарий `connect -> initialize -> tools/list` рабочим и очевидным.
- Текущий основной стек проекта: Kotlin.
- По умолчанию предпочитать HTTP-based MCP transport, если нет отдельной причины выбрать другой transport.
- Endpoint сервера не зашивать глубоко в код: выносить в конфиг или в явно выделенную настройку.
- Не раздувать стартовый reference лишними абстракциями до появления реальной необходимости.
- Для нового публичного кода и важного orchestration-кода по умолчанию добавлять документацию.

## Коммит-правило

- Если пользователь переключается на новый смысловой блок и впереди новые правки, сначала проверить, есть ли незакоммиченные изменения.
- Если изменения есть, мягко предложить сделать коммит перед продолжением.
- Если пользователь просит только анализ, объяснение, проверку, запуск команд или чтение файлов без новых правок, отдельно не останавливать работу из-за коммита.

## Команды и артефакты проекта

- Базовые команды проекта:
  - сборка: `.\gradlew.bat build`
  - сборка direct launcher-артефактов: `.\gradlew.bat installClientDist installServerDist installStatefulServerDist`
  - запуск `stateless` сервера через direct launcher: `.\build\install\mcp-server\bin\mcp-server.bat`
  - запуск `stateful` сервера через direct launcher: `.\build\install\mcp-stateful-server\bin\mcp-stateful-server.bat`
  - запуск интерактивного клиента через direct launcher: `.\build\install\mcp-client\bin\mcp-client.bat`
  - технический Gradle-запуск `stateless` сервера: `.\gradlew.bat runServer`
  - технический Gradle-запуск `stateful` сервера: `.\gradlew.bat runStatefulServer`
  - технический Gradle-запуск клиента: `.\gradlew.bat runClient`
  - scripted-запуск клиента для smoke/e2e: `powershell -ExecutionPolicy Bypass -File .\scripts\invoke-client-commands.ps1 -Commands help,exit`
  - подготовка ручной проверки: `powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1`
  - запуск уже собранного проекта: `powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild`
  - сквозная проверка: `powershell -ExecutionPolicy Bypass -File .\scripts\check-e2e.ps1`
- Для ручного запуска предпочитать direct launcher-артефакты из `build\install\...`, а не `gradlew run...`: так в консоли не смешиваются вывод приложения и Gradle progress UI.
- Трактовка пользовательской фразы `собери проект`: по умолчанию это запуск `powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1`, а не только `.\gradlew.bat build`.
- Трактовка пользовательской фразы `запусти проект`: по умолчанию это запуск `powershell -ExecutionPolicy Bypass -File .\scripts\start-manual-check.ps1 -SkipBuild`.
- Локальные endpoints:
  - `stateless`: `http://127.0.0.1:3000/mcp`
  - `stateful`: `http://127.0.0.1:3001/mcp`
- Текущий transport-подход:
  - `stateless` контур использует Streamable HTTP;
  - `stateful` контур использует классический SSE transport с server-to-client notifications.
- Если меняется способ запуска или проверочный контур, обновить этот раздел, README и журнал реализации синхронно.

## Что перечитать по необходимости

- Если работа идёт над организацией кода: правила архитектуры из общего `MemoryBank`.
- Если работа идёт над документацией или публичным API: правила документации из общего и локального `MemoryBank`.
- Если работа связана с расширением MCP reference: локальный `MemoryBank` проекта и README.
