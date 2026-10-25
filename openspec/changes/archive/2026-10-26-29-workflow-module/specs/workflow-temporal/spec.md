# Spec Delta

## Purpose
Определяет прикладной контракт длительного исполнения Workflow и его интеграцию с Temporal без проникновения Temporal-зависимостей в доменную модель.

## ADDED Requirements

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
Application-сценарий SHALL запускать Workflow, подготавливать его рабочее пространство через `workspace.api`, инициировать техническую работу через `agent.api` и фиксировать результат в состоянии Workflow.

#### Scenario: Успешный запуск
- **WHEN** валидная команда запуска передана при доступных workspace, agent и Temporal
- **THEN** возвращается публичный Workflow state со статусом `RUNNING`, этапом `ANALYZING_REQUIREMENTS` и идентификатором исполнения

#### Scenario: Ошибка подготовки workspace
- **WHEN** workspace отклоняет подготовку
- **THEN** agent не запускается, Temporal execution не считается успешно стартовавшим, а Workflow получает состояние ошибки с причиной

### Requirement: Изоляция Temporal
Доменный слой и `workflow.api` SHALL NOT зависеть от пакетов Temporal, Spring, Temporal-конфигурации или `workflow.infrastructure`.

#### Scenario: Архитектурная проверка
- **WHEN** анализируются зависимости классов domain и api
- **THEN** среди них отсутствуют Temporal SDK, Spring и инфраструктурные классы workflow
