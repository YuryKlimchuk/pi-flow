# Spec Delta

## Purpose

Проверяет реализованное решение независимой ролью Reviewer и управляет подтверждением результата либо возвратом замечаний в реализацию.

## ADDED Requirements

### Requirement: Независимое ревью
После успешной реализации workflow SHALL запускать роль `REVIEWER` с контекстом Issue и рабочим состоянием, не используя результат Developer как решение о приемлемости.

#### Scenario: Запуск ревью
- **WHEN** этап реализации завершён успешно
- **THEN** execution запускает Reviewer на этапе `REVIEWING` и сохраняет результат ревью

### Requirement: Gate ревью
Workflow SHALL предоставлять gate `approve/rework` для результата Reviewer.

#### Scenario: Одобрение ревью
- **WHEN** пользователь подтверждает результат Reviewer
- **THEN** workflow переходит на этап `READY_FOR_PULL_REQUEST` и продолжает финализацию

#### Scenario: Замечания ревью
- **WHEN** пользователь отправляет непустые замечания Reviewer при доступном rework
- **THEN** workflow возвращается на `IMPLEMENTING`, запускает Developer с этими замечаниями и сохраняет результат ревью в timeline

### Requirement: Лимит ревью
Workflow SHALL ограничивать цикл `REVIEWING` тремя итерациями и SHALL переводить workflow в `FAILED` после превышения лимита.

#### Scenario: Исчерпание итераций ревью
- **WHEN** запрошен rework после третьего результата Reviewer
- **THEN** новый Reviewer run не запускается, workflow становится `FAILED`, а workspace lock освобождается
