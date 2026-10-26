# workflow-temporal Specification

## Purpose
Определяет прикладной контракт длительного исполнения Workflow и его интеграцию с Temporal без проникновения Temporal-зависимостей в доменную модель.

## Requirements

### Requirement: Порт запуска исполнения
Application-слой `workflow` SHALL предоставлять порт запуска, получения результата и актуального состояния длительного исполнения, не раскрывающий типы Temporal в публичном контракте.

#### Scenario: Запуск через порт
- **WHEN** application-сценарий передаёт валидное состояние Workflow порту исполнения
- **THEN** порт возвращает идентификатор исполнения и состояние принятого запуска типизированной моделью модуля

#### Scenario: Запрос актуального состояния
- **WHEN** вызывающая сторона запрашивает состояние workflow после старта execution
- **THEN** порт возвращает публичное состояние, отражающее фактический прогресс или терминальный результат execution

### Requirement: Temporal-адаптер
Infrastructure-адаптер SHALL преобразовывать публичную команду исполнения в Temporal workflow и SHALL возвращать ошибки подключения или запуска как типизированные ошибки application-контракта.

#### Scenario: Temporal доступен
- **WHEN** приложение подключено к настроенному Temporal endpoint
- **THEN** запуск создаёт одно Temporal execution с заданным workflow id и возвращает его идентификатор

#### Scenario: Temporal недоступен
- **WHEN** endpoint Temporal недоступен при запуске
- **THEN** application-сценарий получает типизированную ошибку запуска, а доменное состояние не переходит в успешное выполнение

### Requirement: Минимальный сквозной сценарий
Application-сценарий SHALL создавать Workflow и инициировать Temporal execution с контекстом Issue; preparation activity SHALL выполняться до analysis activity, а публичное состояние SHALL обновляться после завершения или ошибки Analyst.

#### Scenario: Успешный запуск
- **WHEN** валидная команда запуска передана при доступных workspace, agent и Temporal, а Analyst создаёт валидный артефакт
- **THEN** Temporal execution завершается успешным результатом только после подготовки workspace и анализа

#### Scenario: Ошибка подготовки workspace
- **WHEN** workspace отклоняет подготовку
- **THEN** agent не запускается, Temporal execution завершается ошибкой и не возвращает успешный результат

#### Scenario: Успешный запуск execution
- **WHEN** валидная команда Issue передана при доступном Temporal
- **THEN** возвращается публичное состояние созданного Workflow с идентификатором Temporal execution, а execution получает workflow id, owner, repository и issue number

#### Scenario: Ошибка запуска Temporal
- **WHEN** Temporal недоступен при старте execution
- **THEN** application-сценарий получает типизированную ошибку запуска, Workflow не помечается успешно запущенным и agent не вызывается

#### Scenario: Успешный анализ
- **WHEN** preparation и Analyst завершаются успешно и артефакт валиден
- **THEN** публичное состояние содержит завершённый этап анализа, текст результата и предложенный branch name

#### Scenario: Ошибка анализа
- **WHEN** Analyst завершается ошибкой или возвращает невалидный результат
- **THEN** публичное состояние становится `FAILED` и не содержит успешного результата анализа

#### Scenario: Продолжение
- **WHEN** публичная команда продолжения принята после валидного анализа
- **THEN** workflow запускает следующий этап, а состояние больше не сообщает анализ как единственный текущий этап

### Requirement: Подготовка workspace в Temporal activity
Temporal execution SHALL выполнять `workspace.prepare` внутри preparation activity до analysis activity и SHALL передавать результат подготовки в следующую activity.

#### Scenario: Реальное клонирование
- **WHEN** Temporal worker выполняет preparation activity для доступного Git-репозитория
- **THEN** workspace создаёт локальную копию и рабочую ветку `pi-flow/<workflowId>`, а analysis activity получает существующий путь этой копии

#### Scenario: Ошибка клонирования
- **WHEN** workspace отклоняет clone или создание ветки
- **THEN** preparation activity завершается ошибкой, analysis activity не запускается и Temporal execution не возвращает успешный результат

### Requirement: Изоляция Temporal
Доменный слой и `workflow.api` SHALL NOT зависеть от пакетов Temporal, Spring, Temporal-конфигурации или `workflow.infrastructure`.

#### Scenario: Архитектурная проверка
- **WHEN** анализируются зависимости классов domain и api
- **THEN** среди них отсутствуют Temporal SDK, Spring и инфраструктурные классы workflow
