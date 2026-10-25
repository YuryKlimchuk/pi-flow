# workflow-temporal Specification

## Purpose
Определяет прикладной контракт длительного исполнения Workflow и его интеграцию с Temporal без проникновения Temporal-зависимостей в доменную модель.

## Requirements

### Requirement: Порт запуска исполнения
Application-слой `workflow` SHALL предоставлять порт запуска и получения результата длительного исполнения, не раскрывающий типы Temporal в публичном контракте.

#### Scenario: Запуск через порт
- **WHEN** application-сценарий передаёт валидное состояние Workflow порту исполнения
- **THEN** порт возвращает идентификатор исполнения и состояние принятого запуска типизированной моделью модуля

### Requirement: Temporal-адаптер
Infrastructure-адаптер SHALL преобразовывать публичную команду исполнения в Temporal workflow и SHALL возвращать ошибки подключения или запуска как типизированные ошибки application-контракта.

#### Scenario: Temporal доступен
- **WHEN** приложение подключено к настроенному Temporal endpoint
- **THEN** запуск создаёт одно Temporal execution с заданным workflow id и возвращает его идентификатор

#### Scenario: Temporal недоступен
- **WHEN** endpoint Temporal недоступен при запуске
- **THEN** application-сценарий получает типизированную ошибку запуска, а доменное состояние не переходит в успешное выполнение

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

### Requirement: Подготовка workspace в Temporal activity
Temporal execution SHALL выполнять `workspace.prepare` внутри activity до завершения минимального execution.

#### Scenario: Реальное клонирование
- **WHEN** Temporal worker выполняет activity для доступного Git-репозитория
- **THEN** workspace создаёт локальную копию репозитория и рабочую ветку `pi-flow/<workflowId>`, а activity возвращает результат подготовки

#### Scenario: Ошибка клонирования
- **WHEN** workspace отклоняет clone или создание ветки
- **THEN** activity завершается ошибкой, Temporal execution не возвращает успешный результат подготовки и agent не запускается

### Requirement: Изоляция Temporal
Доменный слой и `workflow.api` SHALL NOT зависеть от пакетов Temporal, Spring, Temporal-конфигурации или `workflow.infrastructure`.

#### Scenario: Архитектурная проверка
- **WHEN** анализируются зависимости классов domain и api
- **THEN** среди них отсутствуют Temporal SDK, Spring и инфраструктурные классы workflow
