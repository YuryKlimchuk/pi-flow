# Proposal

## Why

Сейчас успешный Analyst фактически завершает отдельное Temporal execution, но доменный Workflow остаётся `RUNNING`, а пользовательский результат не переводится в явное состояние ожидания решения. Кнопка «Продолжить» не запускает следующий этап, а повторный анализ не получает предыдущий текст анализа, поэтому flow не может корректно продолжаться через Architect или управляемо уточняться пользователем.

## What Changes

- После успешной валидации Analyst переводить доменный Workflow в `WAITING_FOR_USER`, сохраняя результат анализа доступным для просмотра.
- Отображать в web явное состояние «ожидание решения пользователя» и предоставлять ровно два действия: «Продолжить» и «Исправить».
- По «Продолжить» выполнять `resume()`, переводить этап в `DESIGNING_SOLUTION` и запускать новое отдельное execution с activity Architect.
- Добавить Architect как роль agent с ресурсами `roles/architect/`, профильной инструкцией и результатом в предсказуемом design-артефакте workspace.
- Возвращать из Architect только путь workspace и сериализуемые метаданные; полный текст design-артефакта читать через workspace, а не передавать через Temporal history.
- По «Исправить» запускать новое Analyst execution с feedback и предыдущим текстом `analyst.md` либо его workspace-путём в контексте инструкции.
- Сохранить валидацию branch name и обязательных артефактов по аналогии с Analyst; Architect SHALL валидировать свой design-артефакт.
- Сохранить границы `web → workflow.api`, orchestration в `workflow`, запуск Pi в `agent`, Temporal только в `workflow.infrastructure`; не добавлять БД и новые модули.

## Capabilities

### New Capabilities

Нет.

### Modified Capabilities

- `workflow-project-analysis`: успешный Analyst завершает execution с результатом, достаточным для перехода в ожидание; rework получает предыдущий анализ; Architect создаёт design-артефакт.
- `workflow-temporal`: отдельные execution используются для Analyst rework и Architect; результаты не содержат большой текст артефактов.
- `workflow-lifecycle`: результат анализа переводит Workflow в `WAITING_FOR_USER`, а действия пользователя возобновляют его или возвращают на Analyst.
- `web-workflow-analysis`: UI отображает ожидание решения и две команды пользователя.
- `agent-roles`: профиль ARCHITECT использует отдельные role resources и не смешивается с Analyst.
- `agent-module-contract`: публичная команда agent поддерживает Architect и контекст предыдущего анализа.

## Impact

Затрагиваются `Workflow`, `WorkflowService`, execution port/adapter, Temporal workflow и activities, публичные workflow/agent DTO, ресурсы ролей, `WorkflowController` и `workflow-status.html`, а также соответствующие unit/MVC/Temporal/architecture tests. Изменение не вводит постоянное хранение, новые модули, Developer/Reviewer/QA, pull request, approval или генерацию OpenSpec Proposal.
