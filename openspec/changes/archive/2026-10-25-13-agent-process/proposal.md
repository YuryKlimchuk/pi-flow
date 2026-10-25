# Proposal

## Why

Change 1 определил порт, а Change 2 — безопасную подготовку. Теперь требуется фактическое техническое выполнение задачи и сбор результата без блокировки процесса. Change 3 фиксирует CLI/subprocess как единственный основной механизм; RPC откладывается.

## What Changes

- Реализовать запуск Pi через `ProcessBuilder`.
- Передавать инструкцию и входные данные через поддерживаемый CLI-интерфейс.
- Параллельно читать stdout и stderr, получать exit code и закрывать ресурсы.

## Capabilities

### New Capabilities
- `agent-process`: выполнение одной задачи Pi через CLI subprocess.

### Modified Capabilities
- Нет.

## Impact

Затрагивается `agent.infrastructure` и адаптер внутреннего порта из Change 1; используется конфигурация Change 2. RPC не реализуется в этом change.
