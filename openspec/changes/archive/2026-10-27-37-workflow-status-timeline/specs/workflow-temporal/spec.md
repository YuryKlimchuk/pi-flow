# Spec Delta

## MODIFIED Requirements

### Requirement: Минимальный сквозной сценарий
Application-сценарий SHALL создавать один Workflow и инициировать один длительный Temporal execution; preparation SHALL предшествовать Analyst, а команды пользователя SHALL продвигать этапы через signals без новых execution, при этом каждый завершённый запуск SHALL быть представлен в timeline своим runId.

#### Scenario: Успешный запуск
- **WHEN** валидная команда запуска передана при доступных workspace, agent и Temporal
- **THEN** Temporal execution выполняет подготовку workspace и анализ, не создавая отдельные execution на этап

#### Scenario: Ошибка подготовки workspace
- **WHEN** workspace отклоняет подготовку
- **THEN** agent не запускается, execution завершается ошибкой и не возвращает успешный результат

#### Scenario: Успешный запуск execution
- **WHEN** валидная команда Issue передана при доступном Temporal
- **THEN** возвращается публичное состояние созданного Workflow с идентификатором Temporal execution, а execution получает workflow id, owner, repository и issue number

#### Scenario: Ошибка запуска Temporal
- **WHEN** Temporal недоступен при старте execution
- **THEN** application-сценарий получает типизированную ошибку запуска, Workflow не помечается успешно запущенным и agent не вызывается

#### Scenario: Gate Analyst
- **WHEN** Analyst завершается успешно и артефакт валиден
- **THEN** execution переходит в `WAITING_FOR_USER`, а состояние содержит результат шага и идентификатор запуска

#### Scenario: Ошибка анализа
- **WHEN** Analyst завершается ошибкой или возвращает невалидный результат
- **THEN** публичное состояние становится `FAILED` и не содержит успешного результата анализа

#### Scenario: Продолжение
- **WHEN** публичная команда продолжения принята на ручном gate
- **THEN** тот же execution запускает следующий этап, состояние сохраняет завершённый результат в timeline и больше не сообщает его как единственный текущий этап

#### Scenario: Rework
- **WHEN** публичная команда rework с feedback принята на ручном gate
- **THEN** тот же execution повторяет текущий этап, а новый результат получает отдельный runId в timeline

### Requirement: Изоляция Temporal
Доменный слой и `workflow.api` SHALL NOT зависеть от пакетов Temporal, Spring, Temporal-конфигурации или `workflow.infrastructure`, включая публичную модель timeline.

#### Scenario: Архитектурная проверка
- **WHEN** анализируются зависимости классов domain и api
- **THEN** среди них отсутствуют Temporal SDK, Spring и инфраструктурные классы workflow
