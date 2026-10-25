# Proposal

## Why

После Change 4 модуль умеет запускать изолированную роль, но не контролирует длительные или досрочно прекращённые процессы. Change 5 добавляет технический lifecycle: timeout, отмену, exit-code ошибки и однозначные статусы.

## What Changes

- Ввести настраиваемый timeout на запуск.
- Добавить отмену активного процесса через `process.destroy()` с необходимым усилением завершения.
- Обрабатывать ненулевой exit code и неожиданное завершение.
- Зафиксировать статусы RUNNING, SUCCEEDED, FAILED, TIMED_OUT и CANCELLED.

## Capabilities

### New Capabilities
- `agent-lifecycle`: ограничения времени, отмена и статусы subprocess.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются `agent.api`, `agent.application` и CLI-адаптер Change 3; используется модель роли Change 4. Повторные попытки и остановка workflow не входят в change.
