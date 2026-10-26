# Tasks

## 1. Публичный workflow-контракт и доменное состояние

- [x] 1.1 Добавить публичные `ReworkWorkflowCommand` и операцию rework в `workflow.api`; проверить валидацию workflow id и непустого feedback unit-тестами.
- [x] 1.2 Реализовать в `Workflow` одноразовый переход Continue с очисткой недоступного результата и переход rework на `ANALYZING_REQUIREMENTS`; проверить успешные и запрещённые переходы в `WorkflowDomainTest`.
- [x] 1.3 Обновить `WorkflowState`/mapping так, чтобы после Continue старый analysis result не был actionable; проверить поля состояния для `DESIGNING_SOLUTION`, rework и повторного результата.

## 2. Повторное Temporal execution и Analyst feedback

- [x] 2.1 Расширить application execution port и Temporal adapter публичной командой повторного запуска с feedback, возвращающей новый execution id; проверить доступный и недоступный Temporal сценарии.
- [x] 2.2 Передать feedback через сериализуемый analysis input в activity-инструкцию Analyst без отдельного большого payload в Temporal history; проверить многострочные замечания activity/agent-тестом.
- [x] 2.3 Реализовать в `WorkflowService` атомарную координацию reset → новый execution → сохранение актуального execution id и не публиковать успешный rework при ошибке запуска; проверить application unit-тестами.
- [x] 2.4 Защитить синхронизацию результата проверкой актуального execution id; проверить race-сценарий, в котором старый результат не заменяет состояние нового анализа.
- [x] 2.5 Проверить, что новый Analyst заново записывает и валидирует `branch.txt`, а прежнее branch name не переносится; покрыть валидное и невалидное имя activity-тестами.
- [x] 2.6 Обновить Temporal integration-тесты для first analysis, rework с feedback, нового execution и отказа stale result.

## 3. Web UI Continue и Исправить

- [x] 3.1 Добавить `POST /workflows/rework` в `WorkflowController`, обработку validation error и вызов только `workflow.api`; проверить MVC-тестами пустой и непустой feedback.
- [x] 3.2 Обновить `workflow-status.html`: показывать Continue/Исправить только для актуального результата, открыть dialog/форму feedback и после Continue показать `DESIGNING_SOLUTION` без старого результата; проверить HTML MVC-сценариями.
- [x] 3.3 Отображать отдельное состояние ожидания следующего незапущенного этапа и повторный loading Analyst; остановить/возобновить polling согласно terminal или running состоянию; проверить web UI state сценарии.

## 4. Сквозная проверка

- [x] 4.1 Выполнить `./mvnw test` и исправить регрессии workflow, Temporal, agent и web, не добавляя новые модули или БД.
- [x] 4.2 Выполнить `openspec validate` и проверить наличие proposal, всех delta-спеков, design и tasks в change `34-continue-and-feedback`.
