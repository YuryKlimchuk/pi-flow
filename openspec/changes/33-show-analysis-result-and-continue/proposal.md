# Proposal

## Why

После запуска Workflow web сохраняет только начальное состояние: пользователь не видит, что Analyst действительно выполняется, не получает текст `analyst.md` и не может продолжить процесс из интерфейса. При этом анализ уже сохраняется в подготовленном workspace, поэтому нужно связать фактическое завершение Temporal execution с публичным состоянием workflow и безопасно прочитать артефакт через workspace.

## What Changes

- Добавить публичное чтение актуального состояния workflow, синхронизированного с результатом исполнения Analyst, включая состояние «выполняется», завершение анализа, ошибку и ссылку/идентификатор артефакта.
- Добавить web polling состояния запущенного workflow, чтобы UI показывал индикатор работы Analyst без перехода в Temporal UI.
- После успешного анализа получать текст `.pi-flow/analysis/analyst.md` из workspace через публичный контракт workspace и отображать его в web UI.
- Добавить действие «Продолжить», доступное только после отображения валидного результата анализа, и передавать его в workflow как следующий шаг.
- Расширить результат Analyst предложенным branch name: его SHALL генерировать роль `ANALYST`, а workflow SHALL валидировать его как допустимое латинское Git-имя ветки и показывать пользователю.
- Сохранить модульные границы: web обращается к `workflow.api`, workflow координирует execution и обращается к `workspace.api`, а Temporal-типы остаются в infrastructure.
- Не добавлять новую БД, новые модули, approval-механизм, PR или последующие роли.

## Capabilities

### New Capabilities

- `web-workflow-analysis`: отображение прогресса, текста результата анализа, предложенного branch name и действия продолжения в web UI.

### Modified Capabilities

- `workflow-project-analysis`: результат анализа SHALL включать текстовый артефакт через workspace и предложение branch name от Analyst.
- `workflow-temporal`: execution SHALL публиковать завершение/ошибку анализа в доступное публичному workflow состоянию без утечки Temporal API.
- `workflow-lifecycle`: публичное состояние SHALL отражать выполняемый анализ, результат и возможность продолжения следующего этапа.
- `workspace-preparation`: публичный workspace-контракт SHALL позволять прочитать только ожидаемый артефакт анализа для отображения в UI.
- `web-ui-states`: UI SHALL отображать отдельные loading/running/result/error состояния долгого анализа и периодически обновлять их.
- `workflow-issue-start`: после запуска по Issue web SHALL открывать отслеживание состояния workflow и результата анализа.

## Impact

Затрагиваются `workflow.api` и application-состояние, адаптер Temporal и обработка результата execution, контракт чтения артефактов в `workspace.api`, `WorkflowController` и шаблон workflow-страницы, а также тесты модулей workflow, workspace и web. `agent` изменяется только в части контракта/инструкции Analyst для предложения branch name; web не получает зависимость от agent или Temporal. Хранилище остаётся in-memory, полный текст не читается из Temporal history, а извлекается из workspace. Architect, Developer, Reviewer, QA, PR, approval, генерация OpenSpec Proposal и миграция на новую БД не входят в change.
