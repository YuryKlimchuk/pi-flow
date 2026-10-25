# Proposal

## Why

Шесть этапов реализации agent требуют проверяемых границ: процессный код особенно уязвим к блокировке потоков, зависшим процессам и утечкам секретов. Change 6 закрепляет независимые тесты контракта, subprocess, lifecycle и архитектуры после Changes 1–5.

## What Changes

- Добавить тесты подготовки команды, workspace и окружения.
- Проверить успех, exit code, ошибки запуска и большой stdout/stderr.
- Проверить timeout, cancellation и cleanup.
- Проверить отсутствие workflow/Git/GitHub ответственности и успешную сборку.

## Capabilities

### New Capabilities
- `agent-module-verification`: проверяемость поведения и архитектурных границ agent.

### Modified Capabilities
- Нет.

## Impact

Добавляются unit, contract, process/lifecycle и architecture tests; основной Pi можно заменить тестовым executable. Change зависит от всех предыдущих agent changes и не требует установленного Pi для основной проверки.
