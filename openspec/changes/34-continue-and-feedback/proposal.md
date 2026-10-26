# Proposal

## Why

После успешного анализа домен переводится на `DESIGNING_SOLUTION`, но web продолжает показывать карточку уже завершённого Analyst и позволяет повторно вызывать «Продолжить». Кроме того, пользователь не может передать замечания Analyst и запустить анализ заново, поэтому исправления требуют нового workflow вручную.

## What Changes

- Сделать продолжение после анализа одноразовым переходом: после принятия команды UI перестаёт показывать результат Analyst как текущий и отображает состояние `DESIGNING_SOLUTION`.
- Зафиксировать, что в текущем инкременте «следующий шаг» означает только переход состояния workflow в `DESIGNING_SOLUTION`; Architect и фактическая работа этого этапа не реализуются.
- Добавить публичную команду rework анализа с пользовательским feedback через `workflow.api`.
- При rework сбрасывать опубликованный результат анализа, возвращать workflow на `ANALYZING_REQUIREMENTS` и запускать новое Temporal execution с новым run/execution id.
- Передавать feedback в инструкцию Analyst/activity; feedback не добавлять в Temporal history как отдельный большой документ.
- Пересчитывать branch name при каждом новом запуске Analyst и повторно валидировать его как латинское Git-имя.
- Добавить в web кнопку «Исправить», форму/окно ввода замечаний и действие отправки; после отправки показывать выполнение нового анализа.
- Сохранить границы `web → workflow.api`, координацию в `workflow`, запуск процессов в `agent` и Temporal только в `workflow.infrastructure`; не добавлять БД или новые модули.

## Capabilities

### New Capabilities

Нет.

### Modified Capabilities

- `workflow-project-analysis`: Analyst SHALL принимать feedback при rework и создавать новый результат/branch suggestion.
- `workflow-temporal`: workflow SHALL запускать повторное execution анализа с feedback без передачи большого текста через Temporal history.
- `workflow-lifecycle`: домен SHALL поддерживать одноразовое продолжение к `DESIGNING_SOLUTION` и возврат к анализу для rework.
- `web-workflow-analysis`: UI SHALL скрывать устаревший результат после Continue и предоставлять исправление с feedback.
- `web-ui-states`: UI SHALL различать состояние следующего незапущенного этапа и повторный анализ.
- `agent-process`: процессный запуск SHALL передавать feedback Analyst как данные инструкции, а не shell-код.
- `agent-module-contract`: публичная команда Analyst SHALL поддерживать feedback в контексте запуска.

## Impact

Затрагиваются публичные модели и операции `workflow.api`, переходы агрегата `Workflow`, `WorkflowService` и Temporal adapter/workflow/activity; web-контроллер и `workflow-status.html`; инструкция Analyst и тесты workflow, Temporal, agent и MVC. Persistence остаётся in-memory. Architect/Design activity, остальные роли, PR, approval, БД и новые модули не входят в change.