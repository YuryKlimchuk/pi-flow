# Spec Delta

## MODIFIED Requirements

### Requirement: Модель состояния Workflow
Workflow SHALL предоставлять статус, текущий этап, историю завершённых этапов, контрольные точки и результаты Analyst/Architect, а состояние SHALL отражать единое длительное execution вместо набора execution отдельных этапов.

#### Scenario: Чтение состояния
- **WHEN** вызывающая сторона запрашивает состояние созданного Workflow
- **THEN** получает идентификатор, статус, текущий этап, пустую историю завершённых этапов и контрольные точки через публичный контракт

#### Scenario: Чтение результата
- **WHEN** вызывающая сторона запрашивает состояние workflow после успешного анализа
- **THEN** получает branch name Analyst и текстовый результат анализа через публичный контракт

#### Scenario: Чтение long-running state
- **WHEN** вызывающая сторона запрашивает состояние workflow по стабильному workflow id
- **THEN** получает текущий этап, статус, завершённые этапы, доступные артефакты и тот же execution id

### Requirement: Допустимые переходы
Домен Workflow SHALL разрешать ручное продолжение или повтор текущего этапа только из соответствующего `WAITING_FOR_USER` gate и SHALL отклонять остальные переходы без изменения состояния.

#### Scenario: Переход к следующему этапу
- **WHEN** Workflow в статусе `CREATED` завершает `ANALYZING_REQUIREMENTS`
- **THEN** статус становится `RUNNING`, текущим этапом становится `DESIGNING_SOLUTION`, а `ANALYZING_REQUIREMENTS` добавляется в историю

#### Scenario: Запрещённый переход
- **WHEN** вызывающая сторона запрашивает переход, не разрешённый из текущего состояния
- **THEN** состояние Workflow не изменяется и возвращается ошибка недопустимого перехода

#### Scenario: Продолжение Analyst
- **WHEN** Workflow ожидает решения после `ANALYZING_REQUIREMENTS` и получает `continue`
- **THEN** текущим этапом становится `DESIGNING_SOLUTION`, а `ANALYZING_REQUIREMENTS` добавляется в историю

#### Scenario: Повтор Analyst
- **WHEN** Workflow ожидает решения после `ANALYZING_REQUIREMENTS` и получает непустой feedback
- **THEN** Workflow остаётся на `ANALYZING_REQUIREMENTS`, очищает текущий успешный результат и принимает запрос повторного запуска

#### Scenario: Запрещённое действие
- **WHEN** команда `continue` или `rework` относится к статусу `RUNNING`, `FAILED`, `COMPLETED` или к автоматическому этапу
- **THEN** состояние Workflow не изменяется и возвращается ошибка недопустимого перехода

### Requirement: Публичный запуск Workflow
Публичный API `workflow` SHALL предоставлять команды запуска, запроса состояния и продолжения после анализа, а внешние модули SHALL обращаться к ним без импорта domain, application или infrastructure типов.

#### Scenario: Вызов из web
- **WHEN** web передаёт команду запуска через `workflow.api`
- **THEN** workflow возвращает публичный идентификатор и состояние запуска без доступа web к внутренним пакетам

#### Scenario: Вызов продолжения из web
- **WHEN** web передаёт публичную команду продолжения для workflow с валидным результатом анализа
- **THEN** workflow принимает команду без доступа web к внутренним пакетам

#### Scenario: Продолжение до результата анализа
- **WHEN** команда продолжения относится к workflow без завершённого анализа
- **THEN** команда отклоняется типизированной ошибкой и состояние workflow не изменяется

#### Scenario: Вызов long-running operations
- **WHEN** web передаёт команду запуска, query state, continue или rework через `workflow.api`
- **THEN** workflow возвращает публичное состояние или типизированную ошибку без доступа web к Temporal SDK

### Requirement: Границы ответственности Workflow
Workflow SHALL координировать сигналы и состояние через публичные контракты, SHALL NOT выполнять локальные Git-операции самостоятельно и SHALL NOT определять порядок ролей внутри `agent`.

#### Scenario: Координация зависимостей
- **WHEN** минимальный запуск требует подготовки workspace и технического запуска агента
- **THEN** workflow вызывает `workspace.api` и `agent.api`, а не JGit, `Process`, GitHub HTTP-клиент или Pi напрямую

#### Scenario: Минимальная координация
- **WHEN** валидная команда запуска передана
- **THEN** workflow создаёт состояние и инициирует execution, не вызывая агент и не импортируя Git/JGit реализацию

#### Scenario: Координация long-running execution
- **WHEN** long-running execution выполняет подготовку workspace или роль агента
- **THEN** workflow использует публичные операции workspace/agent, а Temporal orchestration остаётся в `workflow.infrastructure`

## ADDED Requirements

### Requirement: Ручные контрольные точки
Workflow SHALL переводить статус в `WAITING_FOR_USER` после валидного результата Analyst и после валидного результата Architect, а Developer и Reviewer SHALL быть определены как следующие этапы без запуска полноценных activities в этом change.

#### Scenario: Gate Analyst
- **WHEN** Analyst создаёт валидные `analyst.md` и `branch.txt`
- **THEN** query показывает `WAITING_FOR_USER` на `ANALYZING_REQUIREMENTS` и доступные `continue`/`rework`

#### Scenario: Gate Architect
- **WHEN** Architect создаёт валидный `design/architect.md`
- **THEN** query показывает `WAITING_FOR_USER` на `DESIGNING_SOLUTION` и доступные `continue`/`rework`
