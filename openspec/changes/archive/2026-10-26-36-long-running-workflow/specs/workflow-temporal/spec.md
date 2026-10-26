# Spec Delta

## MODIFIED Requirements

### Requirement: Порт запуска исполнения
Application-слой `workflow` SHALL предоставлять порт для запуска одного длительного исполнения, получения его актуального состояния и отправки команд `continue` и `rework`, не раскрывающий типы Temporal в публичном контракте.

#### Scenario: Запуск через порт
- **WHEN** application-сценарий передаёт валидное состояние Workflow порту исполнения
- **THEN** порт возвращает идентификатор исполнения и состояние принятого запуска типизированной моделью модуля

#### Scenario: Запрос актуального состояния
- **WHEN** вызывающая сторона запрашивает состояние workflow после старта execution
- **THEN** порт возвращает публичное состояние, отражающее фактический прогресс или терминальный результат execution

#### Scenario: Управление через порт
- **WHEN** вызывающая сторона отправляет `continue` или `rework` для существующего workflow
- **THEN** порт доставляет соответствующий сигнал в то же execution и не создаёт новое execution

### Requirement: Temporal-адаптер
Infrastructure-адаптер SHALL преобразовывать публичные команды в одно Temporal workflow, SHALL получать состояние через query и SHALL возвращать ошибки подключения, query или signal как типизированные ошибки application-контракта.

#### Scenario: Temporal доступен
- **WHEN** приложение подключено к настроенному Temporal endpoint
- **THEN** запуск создаёт одно Temporal execution с заданным workflow id и возвращает его идентификатор

#### Scenario: Temporal недоступен
- **WHEN** endpoint Temporal недоступен при запуске
- **THEN** application-сценарий получает типизированную ошибку запуска, Workflow не помечается успешно запущенным и agent не вызывается

#### Scenario: Query или signal недоступны
- **WHEN** Temporal не отвечает на query или signal
- **THEN** application-сценарий получает типизированную ошибку операции и не создаёт второе execution

### Requirement: Минимальный сквозной сценарий
Application-сценарий SHALL создавать Workflow и инициировать одно длительное Temporal execution с контекстом Issue; preparation activity SHALL выполняться до analysis activity, а публичное состояние SHALL обновляться через query после этапов и сигналов.

#### Scenario: Успешный запуск
- **WHEN** валидная команда запуска передана при доступных workspace, agent и Temporal, а Analyst создаёт валидный артефакт
- **THEN** одно execution после подготовки и анализа остаётся активным в `WAITING_FOR_USER`, а не завершается после Analyst

#### Scenario: Ошибка подготовки workspace
- **WHEN** workspace отклоняет подготовку
- **THEN** agent не запускается, workflow переходит в ошибочное состояние и не возвращает успешный результат

#### Scenario: Успешный запуск execution
- **WHEN** валидная команда Issue передана при доступном Temporal
- **THEN** возвращается публичное состояние созданного Workflow с идентификатором стабильного execution, а execution получает workflow id, owner, repository и issue number

#### Scenario: Ошибка запуска Temporal
- **WHEN** Temporal недоступен при старте execution
- **THEN** application-сценарий получает типизированную ошибку запуска, Workflow не помечается успешно запущенным и agent не вызывается

#### Scenario: Успешный анализ
- **WHEN** preparation и Analyst завершаются успешно и артефакты валидны
- **THEN** публичное состояние содержит завершённый этап анализа, branch name и доступные ручные действия, а execution продолжает ожидание

#### Scenario: Ошибка анализа
- **WHEN** Analyst завершается ошибкой или возвращает невалидный результат
- **THEN** публичное состояние становится `FAILED` и не содержит успешного результата анализа

#### Scenario: Продолжение
- **WHEN** публичная команда продолжения принята после валидного анализа
- **THEN** workflow запускает следующий этап в том же execution, а состояние больше не сообщает анализ как единственный текущий этап

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

## ADDED Requirements

### Requirement: Длительное последовательное исполнение
Единое Temporal execution SHALL выполнять этапы в порядке `ANALYST_ANALYZE_REQUIREMENTS`, `ARCHITECT_DESIGN_SOLUTION`, `DEVELOPER_IMPLEMENT_SOLUTION`, `REVIEWER_REVIEW_IMPLEMENTATION`, сохраняя прогресс и ручные ожидания внутри execution.

#### Scenario: Один execution на flow
- **WHEN** пользователь запускает workflow и затем продолжает Analyst
- **THEN** подготовка, анализ и проектирование выполняются в истории одного workflow id без запуска второго execution

#### Scenario: Восстановление после worker restart
- **WHEN** Temporal worker перезапускается во время ожидания или между этапами
- **THEN** execution восстанавливает последнее состояние и не повторяет успешно завершённый этап без activity retry semantics

### Requirement: Сигналы и query
Long-running workflow SHALL принимать сигналы `continue` и `rework(feedback)` и SHALL предоставлять query актуального состояния с текущим этапом, статусом и ссылками на валидированные артефакты.

#### Scenario: Continue после Analyst
- **WHEN** execution находится в `WAITING_FOR_USER` после валидного Analyst результата и получает `continue`
- **THEN** оно переходит к `ARCHITECT_DESIGN_SOLUTION` и запускает Architect activity в том же execution

#### Scenario: Rework после Analyst
- **WHEN** execution ожидает пользователя после Analyst и получает непустой feedback
- **THEN** текущий Analyst этап запускается повторно с feedback в том же execution, а старый результат не становится текущим

#### Scenario: Недопустимый сигнал
- **WHEN** execution получает `continue` или `rework` не на ручном gate
- **THEN** состояние execution не изменяется и вызывающая сторона получает типизированную ошибку перехода
