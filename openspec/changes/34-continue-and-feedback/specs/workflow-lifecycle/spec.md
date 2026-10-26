# Spec Delta

## MODIFIED Requirements

### Requirement: Допустимые переходы
Домен Workflow SHALL разрешать переход из успешного `ANALYZING_REQUIREMENTS` в `DESIGNING_SOLUTION` ровно один раз и SHALL разрешать возврат из результата анализа в `ANALYZING_REQUIREMENTS` только через явную команду rework с feedback.

#### Scenario: Переход к следующему этапу
- **WHEN** Workflow имеет успешный результат Analyst и пользователь принимает «Продолжить»
- **THEN** статус остаётся `RUNNING`, текущим этапом становится `DESIGNING_SOLUTION`, а `ANALYZING_REQUIREMENTS` добавляется в историю

#### Scenario: Запрещённый переход
- **WHEN** вызывающая сторона запрашивает переход, не разрешённый из текущего состояния
- **THEN** состояние Workflow не изменяется и возвращается ошибка недопустимого перехода

#### Scenario: Повторное продолжение
- **WHEN** пользователь повторно отправляет команду продолжения для Workflow на `DESIGNING_SOLUTION`
- **THEN** домен отклоняет команду и не добавляет новый этап в историю

#### Scenario: Возврат на rework
- **WHEN** Workflow имеет результат Analyst и получает непустой feedback
- **THEN** текущим этапом становится `ANALYZING_REQUIREMENTS`, опубликованные `analysisText` и `branchName` очищаются, а статус становится `RUNNING`

### Requirement: Публичный запуск Workflow
Публичный API `workflow` SHALL предоставлять команды запуска, запроса состояния, продолжения после анализа и повторного анализа с feedback.

#### Scenario: Вызов из web
- **WHEN** web передаёт команду запуска через `workflow.api`
- **THEN** workflow возвращает публичный идентификатор и состояние запуска без доступа web к внутренним пакетам

#### Scenario: Вызов продолжения из web
- **WHEN** web передаёт публичную команду продолжения для workflow с валидным результатом анализа
- **THEN** workflow принимает команду без доступа web к внутренним пакетам

#### Scenario: Продолжение до результата анализа
- **WHEN** команда продолжения относится к workflow без завершённого анализа
- **THEN** команда отклоняется типизированной ошибкой и состояние workflow не изменяется

#### Scenario: Вызов rework из web
- **WHEN** web передаёт публичную команду rework с workflow id и feedback
- **THEN** workflow принимает команду без доступа web к domain, application или infrastructure типам

#### Scenario: Rework без feedback
- **WHEN** команда rework содержит пустой или состоящий только из пробелов feedback
- **THEN** команда отклоняется типизированной ошибкой валидации и состояние не изменяется

### Requirement: Модель состояния Workflow
Workflow SHALL предоставлять текущий этап и только актуальный результат Analyst; после Continue результат Analyst SHALL NOT считаться доступным результатом текущего этапа.

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
