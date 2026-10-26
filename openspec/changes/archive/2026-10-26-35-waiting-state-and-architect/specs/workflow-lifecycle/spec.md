# Spec Delta

## MODIFIED Requirements

### Requirement: Модель состояния Workflow
Workflow SHALL предоставлять статус, текущий этап, историю завершённых этапов, контрольные точки и доступный результат Analyst без зависимости от Temporal SDK, Spring или инфраструктурного API. После успешной записи результата Analyst статус SHALL быть `WAITING_FOR_USER`, а результат SHALL оставаться доступным до выбора действия пользователя.

#### Scenario: Чтение состояния
- **WHEN** вызывающая сторона запрашивает состояние созданного Workflow
- **THEN** получает идентификатор, статус, текущий этап, пустую историю завершённых этапов и контрольные точки через публичный контракт

#### Scenario: Чтение результата
- **WHEN** вызывающая сторона запрашивает состояние workflow после успешного анализа
- **THEN** получает branch name Analyst и текстовый результат анализа через публичный контракт

#### Scenario: Состояние следующего этапа
- **WHEN** Workflow успешно продолжен после анализа
- **THEN** состояние содержит `currentStage=DESIGNING_SOLUTION`, а UI-контракт не помечает старый analysis result как actionable

#### Scenario: Состояние rework
- **WHEN** rework принят до завершения нового анализа
- **THEN** состояние содержит `currentStage=ANALYZING_REQUIREMENTS` и не содержит старых `analysisText` и `branchName`

#### Scenario: Анализ ожидает решения
- **WHEN** Analyst завершён успешно и артефакт анализа валиден
- **THEN** публичное состояние содержит `WAITING_FOR_USER`, текущий этап `ANALYZING_REQUIREMENTS`, branch name и текст анализа

#### Scenario: Результат сохраняется в ожидании
- **WHEN** вызывающая сторона запрашивает состояние Workflow до выбора «Продолжить» или «Исправить»
- **THEN** Workflow возвращает тот же результат Analyst и не сообщает `COMPLETED`

### Requirement: Допустимые переходы
Домен Workflow SHALL разрешать только переходы, определённые для текущего статуса и этапа, включая действия из `WAITING_FOR_USER`: продолжение переводит workflow в `RUNNING` на `DESIGNING_SOLUTION`, а rework переводит его в `RUNNING` на `ANALYZING_REQUIREMENTS`.

#### Scenario: Переход к следующему этапу
- **WHEN** Workflow в статусе `CREATED` завершает `ANALYZING_REQUIREMENTS`
- **THEN** статус становится `RUNNING`, текущим этапом становится `DESIGNING_SOLUTION`, а `ANALYZING_REQUIREMENTS` добавляется в историю

#### Scenario: Запрещённый переход
- **WHEN** вызывающая сторона запрашивает переход, не разрешённый из текущего состояния
- **THEN** состояние Workflow не изменяется и возвращается ошибка недопустимого перехода

#### Scenario: Продолжение к Architect
- **WHEN** Workflow имеет статус `WAITING_FOR_USER`, текущий этап `ANALYZING_REQUIREMENTS` и валидный результат Analyst
- **THEN** Workflow возобновляется, текущим этапом становится `DESIGNING_SOLUTION`, а действие продолжения больше не доступно для этого результата

#### Scenario: Исправление анализа
- **WHEN** Workflow имеет статус `WAITING_FOR_USER` и пользователь передаёт непустые замечания
- **THEN** Workflow возобновляется на этапе `ANALYZING_REQUIREMENTS`, а новый запуск получает предыдущий результат и замечания

#### Scenario: Повторное решение запрещено
- **WHEN** Workflow уже переведён в `RUNNING`, `FAILED` или `COMPLETED`
- **THEN** повторная команда продолжения или исправления отклоняется без изменения состояния

### Requirement: Публичный запуск Workflow
Публичный API `workflow` SHALL предоставлять команды запуска, запроса состояния, продолжения после анализа и rework Analyst после ожидания решения пользователя.

#### Scenario: Вызов из web
- **WHEN** web передаёт команду запуска через `workflow.api`
- **THEN** workflow возвращает публичный идентификатор и состояние запуска без доступа web к внутренним пакетам

#### Scenario: Вызов продолжения из web
- **WHEN** web передаёт публичную команду продолжения для workflow с валидным результатом анализа
- **THEN** workflow принимает команду без доступа web к внутренним пакетам

#### Scenario: Продолжение до результата анализа
- **WHEN** команда продолжения относится к workflow без завершённого анализа
- **THEN** команда отклоняется типизированной ошибкой и состояние workflow не изменяется

#### Scenario: Continue из ожидания
- **WHEN** web передаёт `ContinueWorkflowCommand` для Workflow в `WAITING_FOR_USER`
- **THEN** workflow принимает команду через `workflow.api` и инициирует Architect execution

#### Scenario: Rework из ожидания
- **WHEN** web передаёт `ReworkWorkflowCommand` с непустым feedback для Workflow в `WAITING_FOR_USER`
- **THEN** workflow принимает команду через `workflow.api` и инициирует новое Analyst execution

## ADDED Requirements

### Requirement: Сохранение Architect результата
Workflow SHALL публиковать результат Architect как непустой текст design-артефакта и путь workspace после успешного Architect execution, не передавая полный текст через Temporal history.

#### Scenario: Architect завершён
- **WHEN** Architect успешно создаёт валидный design-артефакт
- **THEN** состояние содержит завершённый `DESIGNING_SOLUTION`, путь workspace и доступный через workspace текст артефакта

#### Scenario: Architect артефакт невалиден
- **WHEN** Architect завершается успешно, но design-артефакт отсутствует, пуст или недоступен
- **THEN** execution завершается ошибкой, а Workflow не публикует успешный design-результат
