# Tasks

## 1. Доменное ожидание и публичный контракт

- [x] 1.1 Изменить обработку успешного Analyst так, чтобы `Workflow` вызывал `waitForUser()` после сохранения результата; проверить `WorkflowDomainTest` для `WAITING_FOR_USER` и сохранения текста/branch.
- [x] 1.2 Ограничить `continue` и `rework` состоянием `WAITING_FOR_USER`, реализовать доменные переходы `resume` + `DESIGNING_SOLUTION` и `resume` + `ANALYZING_REQUIREMENTS`; проверить запрещённые повторные команды.
- [x] 1.3 Расширить публичные workflow state/commands и execution DTO для stage-aware Architect и предыдущего результата без импорта Temporal; проверить API contract tests.
- [x] 1.4 Обновить `WorkflowService` для запуска отдельного Architect execution после Continue и нового Analyst execution после Rework; проверить application tests для успешного старта и ошибки запуска.

## 2. Temporal orchestration и результаты этапов

- [x] 2.1 Реализовать уникальные execution identifiers и stage-aware запуск в `WorkflowExecutionPort`/Temporal adapter; проверить, что Analyst и Architect получают разные execution id.
- [x] 2.2 Изменить Temporal workflow/activity contracts так, чтобы Architect запускался отдельным execution после Continue, а payload содержал только пути и метаданные артефактов; проверить adapter/integration tests.
- [x] 2.3 Добавить Architect activity с `AgentRole.ARCHITECT`, Issue/workspace context и проверкой `.pi-flow/design/architect.md`; покрыть agent failure, missing, empty и unreadable artifact tests.
- [x] 2.4 Передать в rework Analyst feedback и предыдущий `analyst.md` текст или путь; проверить activity test с различимыми предыдущим результатом и замечаниями.
- [x] 2.5 Сохранить проверку branch name и защиту от stale execution при смене этапа; проверить race test, в котором старый Analyst result не перезаписывает Architect или новый Analyst state.

## 3. Ресурсы ролей и workspace artifacts

- [x] 3.1 Добавить `src/main/resources/roles/architect/system-prompt.md` и `roles/architect/skills/` с инструкцией проектирования; проверить загрузку ресурсов через role-resource tests.
- [x] 3.2 Определить предсказуемый design artifact contract и публичное чтение architect result через workspace; проверить отсутствие полного текста в Temporal result.
- [x] 3.3 Обновить Analyst instructions для rework и Architect instructions для design output; проверить, что чужие workspace files не удаляются.

## 4. Web ожидание и действия пользователя

- [x] 4.1 Обновить `WorkflowController`/формы для Continue и Rework из `WAITING_FOR_USER`, сохранив вызовы только `workflow.api`; проверить MVC tests для valid/invalid feedback.
- [x] 4.2 Обновить `workflow-status.html` для явного ожидания решения, двух кнопок и скрытия действий вне `WAITING_FOR_USER`; проверить rendered HTML scenarios.
- [x] 4.3 Добавить отображение running/success/error состояния Architect и корректное polling без повторного Analyst action; проверить web state tests.

## 5. Сквозная проверка

- [x] 5.1 Выполнить `./mvnw test` и исправить регрессии domain, workflow, Temporal, agent, workspace и web без добавления БД или новых модулей.
- [x] 5.2 Выполнить `openspec validate` и проверить наличие всех proposal, delta-specs, design и tasks в `openspec/changes/35-waiting-state-and-architect`.
