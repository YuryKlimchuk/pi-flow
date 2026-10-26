# Spec Delta

## MODIFIED Requirements

### Requirement: Порт запуска исполнения
Application-слой `workflow` SHALL предоставлять порт запуска, запроса актуального состояния, продолжения, rework, создания итоговой ветки и push единого длительного исполнения, не раскрывающий типы Temporal в публичном контракте.

#### Scenario: Запуск через порт
- **WHEN** application-сценарий передаёт валидное состояние Workflow порту исполнения
- **THEN** порт возвращает идентификатор исполнения и состояние принятого запуска типизированной моделью модуля
#### Scenario: Запрос актуального состояния
- **WHEN** вызывающая сторона запрашивает состояние workflow после старта execution
- **THEN** порт возвращает публичное состояние, отражающее фактический прогресс или терминальный результат execution
#### Scenario: Продолжение сигналом
- **WHEN** application принимает команду продолжения для workflow на ручном gate
- **THEN** порт отправляет signal `continueStage` в тот же execution, не создавая новый execution id
#### Scenario: Rework сигналом
- **WHEN** application принимает команду rework с feedback
- **THEN** порт отправляет signal `reworkStage(feedback)` в тот же execution, а workflow повторяет текущий этап
#### Scenario: Действие создания ветки
- **WHEN** application принимает команду создания ветки для workflow после Architect gate
- **THEN** операция выполняется для того же workflow id и не создаёт новый Temporal execution
#### Scenario: Действие push
- **WHEN** application принимает команду push для workflow с созданной веткой
- **THEN** операция выполняется в контексте того же workflow id, а публичный результат содержит состояние завершения

### Requirement: Минимальный сквозной сценарий
Application-сценарий SHALL создавать один Workflow и инициировать один длительный Temporal execution; preparation SHALL предшествовать Analyst, команды пользователя SHALL продвигать этапы через signals без новых execution, а операции создания ветки и push SHALL выполняться через координацию workflow после Architect gate.

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
#### Scenario: Один execution для завершения
- **WHEN** пользователь последовательно утверждает Analyst и Architect, создаёт ветку и выполняет push
- **THEN** все действия относятся к одному workflow id и не создают новый execution id
#### Scenario: Ошибка операции завершения
- **WHEN** activity создания ветки, локального коммита или публикации завершается ошибкой
- **THEN** публичное состояние становится `FAILED` с безопасным сообщением, а следующий завершающий шаг не объявляется успешным
