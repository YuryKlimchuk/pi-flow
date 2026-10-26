# Proposal

## Why

После утверждения proposal Architect текущий workflow переводится в неподдержанный этап `IMPLEMENTING` и не предоставляет пользователю способ создать ветку с предложенным Analyst именем, закоммитить результаты или опубликовать ветку. Поэтому сквозной сценарий подготовки изменений заканчивается до фактической передачи изменений в GitHub.

## What Changes

- Добавить в единый workflow пользовательские операции «Создать ветку» и «Запушить изменения» через публичный `workflow.api`.
- Создавать рабочую ветку с именем `branchName`, предложенным Analyst, а не использовать техническую ветку `pi-flow/<workflowId>` как итоговую.
- После создания ветки коммитить `.pi-flow/analysis/analyst.md` и `.pi-flow/design/architect.md` через публичный workspace-контракт.
- Публиковать подготовленную ветку через `github.api.BranchOperations.publishBranch` и сохранять branch name, commit id и URL в состоянии и timeline.
- Добавить в web кнопки и отображение результата; ошибки создания ветки, коммита и публикации SHALL переводить workflow в `FAILED` с безопасным сообщением без stack trace.
- Определить успешное конечное состояние после push как `COMPLETED`; создание Pull Request, approval и последующие Developer/Reviewer этапы не входят в change.
- Сохранить in-memory хранилище workflow как ограничение: результаты операций не переживают рестарт приложения.

## Capabilities

### New Capabilities

- `workflow-branch-publication`: пользовательское создание итоговой ветки, коммит артефактов и публикация ветки с результатами в workflow state.

### Modified Capabilities

- `workflow-lifecycle`: добавить допустимые переходы создания ветки, push и успешного завершения.
- `workflow-temporal`: связать операции завершения с одним длительным execution и публичным query/state без раскрытия Temporal в API.
- `workflow-project-analysis`: использовать имя ветки Analyst и фиксированные пути версионируемых артефактов при завершении workflow.
- `workspace-branches`: разрешить создание и переключение ветки по имени, предложенному Analyst, и определить коммит итоговых артефактов.
- `workspace-module-contract`: расширить публичный контракт операциями создания итоговой ветки и локального коммита.
- `github-pull-request`: уточнить публикацию подготовленной ветки через `BranchOperations.publishBranch` без создания PR.
- `web-workflow-analysis`: добавить действия создания ветки и push, а также отображение branch name, commit id и URL.

## Impact

Затрагиваются `workflow.api/application/domain/infrastructure/temporal`, публичные API и реализации `workspace` и `github`, `WorkflowController` и шаблон статуса. Потребуются новые DTO/команды и timeline-результаты, координация операций через workflow, а также unit/integration/MVC/architecture tests. Temporal остаётся только в `workflow.infrastructure`; web не получает доступ к workspace, github или Temporal напрямую. Внешняя база данных, восстановление in-memory состояния, создание PR, approval, Developer и Reviewer не затрагиваются.
