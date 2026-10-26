# Tasks

## 1. Public contracts and domain state

- [x] 1.1 Добавить в `workspace.api` типизированные команды и результаты создания/переключения ветки; проверить валидацию имени и отсутствие зависимостей от JGit/Spring через unit и architecture tests.
- [x] 1.2 Добавить в `workflow.api` команды `createBranch` и `push` и расширить `WorkflowState` полями branch/commit/publication result; проверить компиляцию API и сериализуемость публичных моделей.
- [x] 1.3 Расширить домен Workflow переходами `Architect gate → branch creation → push → COMPLETED`, запретить ранние/повторные действия и сохранять timeline entries; проверить domain tests для успешных и запрещённых переходов.

## 2. Workspace branch and commit implementation

- [x] 2.1 Реализовать локальное создание итоговой ветки в application/domain/infrastructure workspace с сохранением существующей технической подготовки; проверить JGit-тестом имя `feature/issue-42-login`, повторное переключение и Git-ошибку.
- [x] 2.2 Обновить commit orchestration так, чтобы push принимал allowlist `.pi-flow/analysis/analyst.md` и `.pi-flow/design/architect.md`; проверить тестами точный набор путей, commit id и отказ при отсутствующем артефакте.
- [x] 2.3 Обновить workspace module contract и публичные Javadocs для новых операций; проверить `ModularArchitectureTest` и `mvn test` для workspace.

## 3. Workflow orchestration and publication

- [x] 3.1 Расширить `WorkflowOperations`/`WorkflowService` операциями создания ветки и push, вызвать workspace create/commit и `BranchOperations.publishBranch` в указанном порядке; проверить application tests, что commit failure не вызывает publish.
- [x] 3.2 Сохранить результаты branch name, commit id, ref/url и безопасные ошибки в in-memory workflow state/timeline; проверить повторное чтение state и отсутствие stack trace.
- [x] 3.3 Интегрировать завершающие действия с единым execution/query state без запуска нового Temporal execution; проверить Temporal integration test на неизменность execution id и `COMPLETED` после push.
- [x] 3.4 Обновить workflow lifecycle/temporal/project-analysis и github contract tests под итоговую ветку, фиксированные артефакты и отсутствие вызова PR creation; проверить соответствующие тестовые классы.

## 4. Web actions and presentation

- [x] 4.1 Добавить в `WorkflowController` POST-действия «Создать ветку» и «Запушить изменения», принимающие только workflow id и возвращающие status page; проверить MVC tests для успешных вызовов и ошибок.
- [x] 4.2 Обновить `workflow-status.html` для условий доступности действий, отображения branch name/commit id/URL и безопасного FAILED-сообщения; проверить HTML assertions для Architect-approved, created-branch, completed и failed states.
- [x] 4.3 Обновить web-документацию/labels и не добавлять PR/approval/developer/reviewer controls; проверить, что в шаблоне отсутствуют эти действия и web зависит только от `workflow.api`.

## 5. End-to-end verification

- [x] 5.1 Добавить сквозной тест prepare → Analyst gate → Architect gate → create branch → push, проверяющий два artifact paths, commit id, branch URL и `COMPLETED`.
- [x] 5.2 Добавить тесты ошибок Git create/commit/publish и повторных нажатий, проверяющие `FAILED`, безопасное сообщение, отсутствие повторного успешного timeline result и отсутствие лишнего execution.
- [x] 5.3 Зафиксировать in-memory limitation в публичной документации/design comments и проверить новым экземпляром репозитория, что после рестарта state не доступен.
- [x] 5.4 Запустить `mvn test` и `openspec validate --change 38-create-branch-and-push`; исправить все нарушения границ модулей и несоответствия сценариям.
