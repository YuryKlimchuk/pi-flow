# Spec Delta

## MODIFIED Requirements

### Requirement: Минимальный сквозной сценарий
Application-сценарий SHALL создавать Workflow и инициировать Temporal execution с контекстом Issue; preparation activity SHALL выполняться до analysis activity, а `agent.run` SHALL вызываться только analysis activity после успешной подготовки.

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

#### Scenario: Ошибка анализа
- **WHEN** Analyst завершается ошибкой или возвращает невалидный результат
- **THEN** Temporal execution завершается ошибкой и не сообщает об успешном workflow

### Requirement: Подготовка workspace в Temporal activity
Temporal execution SHALL выполнять `workspace.prepare` внутри preparation activity до analysis activity и SHALL передавать результат подготовки в следующую activity.

#### Scenario: Реальное клонирование
- **WHEN** Temporal worker выполняет preparation activity для доступного Git-репозитория
- **THEN** workspace создаёт локальную копию и рабочую ветку `pi-flow/<workflowId>`, а analysis activity получает существующий путь этой копии

#### Scenario: Ошибка клонирования
- **WHEN** workspace отклоняет clone или создание ветки
- **THEN** preparation activity завершается ошибкой, analysis activity не запускается и Temporal execution не возвращает успешный результат
