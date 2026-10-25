# Spec Delta

> **Scope**: этот change покрывает **частичный (не полный e2e) сценарий** — от запуска по Issue до клонирования репозитория внутри Temporal activity включительно. Запуск Pi/агента и дальнейшие стадии не входят.

## MODIFIED Requirements

### Requirement: Минимальный сквозной сценарий
Application-сценарий SHALL создавать Workflow и инициировать Temporal execution с контекстом Issue; подготовка workspace SHALL выполняться в рамках Temporal execution, а `agent.run` SHALL NOT вызываться в этом сценарии.

#### Scenario: Успешный запуск
- **WHEN** валидная команда запуска передана при доступных workspace, agent и Temporal
- **THEN** возвращается публичный Workflow state со статусом `RUNNING`, этапом `ANALYZING_REQUIREMENTS` и идентификатором исполнения

#### Scenario: Ошибка подготовки workspace
- **WHEN** workspace отклоняет подготовку
- **THEN** agent не запускается, Temporal execution не считается успешно стартовавшим, а Workflow получает состояние ошибки с причиной

#### Scenario: Успешный запуск execution
- **WHEN** валидная команда Issue передана при доступном Temporal
- **THEN** возвращается публичное состояние созданного Workflow с идентификатором Temporal execution, а execution получает workflow id, owner, repository и issue number

#### Scenario: Ошибка запуска Temporal
- **WHEN** Temporal недоступен при старте execution
- **THEN** application-сценарий получает типизированную ошибку запуска, Workflow не помечается успешно запущенным и agent не вызывается

## ADDED Requirements

### Requirement: Подготовка workspace в Temporal activity
Temporal execution SHALL выполнять `workspace.prepare` внутри activity до завершения минимального execution.

#### Scenario: Реальное клонирование
- **WHEN** Temporal worker выполняет activity для доступного Git-репозитория
- **THEN** workspace создаёт локальную копию репозитория и рабочую ветку `pi-flow/<workflowId>`, а activity возвращает результат подготовки

#### Scenario: Ошибка клонирования
- **WHEN** workspace отклоняет clone или создание ветки
- **THEN** activity завершается ошибкой, Temporal execution не возвращает успешный результат подготовки и agent не запускается
