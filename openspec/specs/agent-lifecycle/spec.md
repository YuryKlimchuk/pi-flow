# agent-lifecycle Specification

## Purpose
Фиксирует технический жизненный цикл одного Pi-процесса: ограничение времени, отмену, обработку кода завершения и однозначные состояния запуска.

## Requirements

### Requirement: Настраиваемый timeout
Запуск SHALL поддерживать положительный настраиваемый timeout и SHALL завершать процесс как `TIMED_OUT`, если deadline истёк до нормального завершения.

#### Scenario: Истёк timeout
- **WHEN** Pi работает дольше заданного timeout
- **THEN** процесс завершается, а результат имеет статус `TIMED_OUT`.

### Requirement: Отмена процесса
Модуль SHALL поддерживать отмену активного запуска через уничтожение процесса и SHALL возвращать статус `CANCELLED` после его завершения.

#### Scenario: Отмена RUNNING
- **WHEN** вызывающая сторона отменяет активный run
- **THEN** вызывается механизм `process.destroy()` (с принудительным fallback при необходимости) и результат имеет `CANCELLED`.

### Requirement: Ненулевой exit code
Модуль SHALL классифицировать любой ненулевой exit code или неожиданное завершение как `FAILED` и SHALL сохранять exit code, если он доступен.

#### Scenario: Pi завершился с кодом 7
- **WHEN** subprocess возвращает exit code `7`
- **THEN** result имеет `FAILED` и содержит `7`.

### Requirement: Технические статусы
Результат SHALL использовать статусы `RUNNING`, `SUCCEEDED`, `FAILED`, `TIMED_OUT` и `CANCELLED` с переходом из `RUNNING` только в одно терминальное состояние.

#### Scenario: Успешное завершение
- **WHEN** Pi завершается с exit code `0` до deadline
- **THEN** статус меняется с `RUNNING` на `SUCCEEDED`.

### Requirement: Cleanup lifecycle
После `SUCCEEDED`, `FAILED`, `TIMED_OUT` или `CANCELLED` модуль SHALL закрыть ресурсы и SHALL NOT оставлять активный принадлежащий ему Pi-процесс.

#### Scenario: Timeout cleanup
- **WHEN** запуск завершается по timeout
- **THEN** проверка процесса показывает, что принадлежащий run subprocess завершён и его ресурсы освобождены.

### Requirement: Граница workflow
Модуль SHALL NOT выполнять повторные попытки или менять этапы workflow при ошибке/timeout/cancel.

#### Scenario: Ошибка агента
- **WHEN** Pi возвращает `FAILED`
- **THEN** agent возвращает технический result, а решение о retry принимает workflow.
