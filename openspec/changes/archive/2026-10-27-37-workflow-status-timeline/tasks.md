# Tasks

## 1. Domain timeline and public contracts

- [x] 1.1 Добавить value object записи завершённого шага с stage, role, action, result/artifact, timestamp и runId; покрыть unit-тестами сериализацию/неизменяемость и порядок записей.
- [x] 1.2 Изменить `Workflow` так, чтобы record/continue/rework append-only сохраняли результаты и не дублировали итерации; проверить domain-тестами Continue, Rework и запрещённые повторные команды.
- [x] 1.3 Расширить `WorkflowState` timeline DTO и добавить summary DTO/операцию списка в `workflow.api`; проверить, что API-модели не импортируют Spring/Temporal.

## 2. Repository and application orchestration

- [x] 2.1 Расширить `WorkflowRepository` операцией `findAll`/`list` и обновить in-memory реализацию со снимком списка; проверить тестом порядок/содержимое списка и пустой результат нового процесса.
- [x] 2.2 Обновить `WorkflowService` mapping и синхронизацию execution, чтобы preparation/analysis/design результаты получали role/action/timestamp/runId и попадали в timeline; проверить application-тестами snapshot состояния после Continue и Rework.
- [x] 2.3 Реализовать `listWorkflows` с компактными полями id, repository, issue, status, currentStage; проверить сервисным тестом и зафиксировать отсутствие persistence после пересоздания in-memory repository.

## 3. Architect proposal artifact

- [x] 3.1 Обновить Architect system prompt и skill в `src/main/resources/roles/architect`, задав обязательные разделы `Why`, `What Changes`, `Capabilities`, `Impact`, `Non-goals`; проверить ресурсным тестом наличие требований.
- [x] 3.2 Добавить workspace/application validation proposal-разделов при чтении `.pi-flow/design/architect.md`; проверить валидный proposal, отсутствующий heading и пустую секцию тестами.
- [x] 3.3 Протянуть структурированный proposal result через Temporal activity/application без зависимости домена от Temporal; проверить integration/unit-тестом, что невалидный artifact переводит workflow в FAILED.

## 4. Timeline status web UI

- [x] 4.1 Обновить `WorkflowController` и `workflow-status.html` для передачи и отрисовки всех timeline entries в хронологическом порядке; проверить MVC-тестом наличие всех блоков и proposal-разделов.
- [x] 4.2 Оставить Continue/Rework только возле актуального ручного gate и добавить поддержку gate Architect; проверить MVC-тестами, что исторические блоки не содержат форм.
- [x] 4.3 Сохранить loading/error/retry-поведение текущего status page и обновить web-тесты для rework, ошибок artifact и завершённого timeline; выполнить `mvn test` для web slice.

## 5. Workflow list page

- [x] 5.1 Добавить GET `/workflows` в web-адаптер и шаблон `workflow-list.html` с id, repository, issue, status, current stage и ссылками на `/workflows/track?workflowId=...`; проверить MVC-тестом успешный список.
- [x] 5.2 Добавить empty/error состояния страницы списка без stack trace; проверить MVC-тестами пустой результат и ошибку workflow.api.
- [x] 5.3 Добавить ссылку на список из status page и проверить навигацию на viewport 320px существующим UI-тестом/HTML assertion.

## 6. Integration and architecture verification

- [x] 6.1 Обновить workflow/Temporal tests для одного execution id, сигналов Continue/Rework и уникальных runId timeline; выполнить полный `mvn test`.
- [x] 6.2 Обновить архитектурные проверки границ `web → workflow.api`, `workflow → agent/workspace`, Temporal только в infrastructure; выполнить `mvn test` и architecture tests.
- [x] 6.3 Выполнить `openspec validate --change 37-workflow-status-timeline` и вручную сверить все сценарии proposal/specs с реализованными endpoint и DTO перед apply.
