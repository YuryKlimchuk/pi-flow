# Tasks

## 1. Domain state and public contracts

- [x] 1.1 Расширить domain-модель Workflow данными реализации, ревью, PR и счётчиками итераций; проверить unit-тестами допустимые переходы `DESIGNING_SOLUTION → IMPLEMENTING → REVIEWING → READY_FOR_PULL_REQUEST`.
- [x] 1.2 Добавить доменные правила approve/rework, лимит 3 и классификацию terminal failure; проверить unit-тестами повторный signal, четвёртый rework и неизменность состояния после запрещённого перехода.
- [x] 1.3 Расширить `workflow.api` командами/состояниями для implementation, review, finalization и PR; проверить сериализацию состояния и отсутствие импортов Temporal/Spring в domain/api.

## 2. Developer and Reviewer activities

- [x] 2.1 Создать activity contract и input/result модели для Developer, включая workspace, design context, feedback, run id и build/test diagnostics; проверить компиляцию и валидацию обязательных полей.
- [x] 2.2 Реализовать Developer activity через публичный `agent.api` с ролью `DEVELOPER` и проверкой успешного результата; проверить unit-тестами success, non-zero exit, timeout и missing workspace.
- [x] 2.3 Добавить activity contract и реализацию Reviewer через публичный `agent.api` с ролью `REVIEWER`; проверить unit-тестами запуск после реализации и отказ при невалидном результате.
- [x] 2.4 Настроить отдельные timeout/retry policy для Developer и Reviewer и классификацию повторяемых/фатальных ошибок; проверить activity-тестами retryable failure и fatal failure.

## 3. Temporal orchestration

- [x] 3.1 Обновить `PiFlowWorkflow` signals/query и `PiFlowWorkflowImpl` для Developer → Reviewer → finalization gate без создания нового execution; проверить workflow-тестом один workflow id на всём пути.
- [x] 3.2 Реализовать циклы implementation failure/rework и reviewer rework с максимумом 3 итераций; проверить workflow-тестами возврат Reviewer → Developer, сохранение timeline и FAILED после лимита.
- [x] 3.3 Обновить worker registration и activity stubs для новых activity; проверить запуск Temporal test environment с успешными и ошибочными сценариями.

## 4. Finalization and lock cleanup

- [x] 4.1 Создать finalization activity/activities, делегирующие branch/commit/push в `workspace`/GitHub публичные контракты; проверить unit-тестами порядок операций и отсутствие прямых Git/GitHub calls в workflow.
- [x] 4.2 Подключить `PullRequestOperations.createPullRequest`, сохранить PR number/URL и сделать финализацию идемпотентной; проверить unit-тестами успешный PR и повторный вызов без дубликата.
- [x] 4.3 Добавить идемпотентное освобождение lock для `COMPLETED`, `FAILED`, `CANCELLED`, включая cancellation/failure paths; проверить тестами lock held в `WAITING_FOR_USER` и released в каждом terminal state.
- [x] 4.4 Удалить из `WorkflowService` синхронное завершение branch/push как источник terminal state и обновить `syncExecution`; проверить application-тестом совпадение domain status и Temporal query status.

## 5. Integration and API verification

- [x] 5.1 Обновить web/API mapping и отображение stages, diagnostics, branch и PR; проверить контрактными тестами состояния Analyst/Architect/Developer/Reviewer/Finalizing и terminal errors.
- [x] 5.2 Добавить end-to-end тест полного пути Issue → preparation → Analyst gate → Architect gate → Developer → Reviewer → branch/push/PR → COMPLETED; проверить один execution, созданный PR и освобождённый lock.
- [x] 5.3 Добавить negative integration tests для build failure, reviewer rework, GitHub failure, lost lock и cancellation; проверить `FAILED`/`CANCELLED`, отсутствие ложного PR и безопасные причины ошибок.
- [x] 5.4 Запустить `mvn test` и архитектурную проверку модульных границ; проверить отсутствие циклических зависимостей и Temporal leakage в domain/api.
