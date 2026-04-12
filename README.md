# ai_advent_day_16

Sandbox-проект для знакомства с MCP и подготовки расширяемой базы под будущие задания.

## Цель текущего этапа

Сделать минимальный working example, который:

- устанавливает MCP-соединение;
- выполняет `initialize`;
- получает список инструментов через `tools/list`;
- выводит этот список в консоль.

## Архитектурный вектор

- основной стек текущего этапа: Kotlin;
- по умолчанию ориентироваться на HTTP-based transport;
- не завязывать клиента на локальный subprocess без явной причины;
- держать endpoint сервера настраиваемым, чтобы позже можно было заменить локальный URL на удалённый.

## Документация

- проектный `MemoryBank`: [MemoryBank/README.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/MemoryBank/README.md)
- preflight для новых сессий: [docs/agent-preflight.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/agent-preflight.md)
- ТЗ на реализацию текущего этапа: [docs/technical-spec.md](/C:/Users/compadre/Downloads/Projects/AiAdvent/day_16/docs/technical-spec.md)
