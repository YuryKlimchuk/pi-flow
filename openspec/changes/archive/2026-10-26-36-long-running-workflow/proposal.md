# Proposal

## Why

Сейчас один пользовательский flow разбит на отдельные Temporal executions: Analyst завершает execution после анализа, а Architect запускается новым execution после Continue. Из-за этого ручная пауза, повторный запуск этапа и история всего процесса разделены между execution и частично восстанавливаются polling-ом и in-memory доменом. Нужно сделать один durable Temporal execution владельцем последовательности этапов, сохранив пользовательские approval/rework gates и границы модулей.

## What Changes

- Заменить отдельные `PiFlowWorkflow` и `PiFlowArchitectWorkflow` на один long-running Temporal workflow с одним workflow id/execution на весь flow.
- Выполнять внутри execution подготовку workspace и этапы в согласованном порядке: Analyst анализирует требования, Architect проектирует решение, Developer реализует, Reviewer проверяет.
- Добавить Temporal signals для ручного управления: `continue` продолжает текущий этап после проверки результата, `rework` повторяет текущий этап с feedback; ожидание реализовать через durable `Workflow.await`.
- Зафиксировать ручные gates после Analyst и Architect; Developer и Reviewer оставить подготовленными для последующих автоматических/ручных этапов, но не реализовывать их полноценные activities в этом change.
- Согласовать понятные имена этапов, ролей, activities и сигналов с правилом `Analyst → анализ`, `Architect → проектирование`, `Developer → реализация`, `Reviewer → ревью`; сохранить доменные стадии, если переименование не требуется для однозначности, и явно сопоставить их с человекочитаемыми названиями.
- Получать актуальное состояние через Temporal query по стабильному workflow id и синхронизировать его с публичным `workflow.api`; polling завершённых child executions и запуск нового execution на Continue/Rework удалить из целевой схемы.
- Сохранить валидацию `.pi-flow/analysis/analyst.md`, `.pi-flow/analysis/branch.txt` и `.pi-flow/design/architect.md`; передавать через Temporal только пути и сериализуемые метаданные, а полный текст читать из workspace.
- Сохранить границы `web → workflow.api`, Temporal SDK только в `workflow.infrastructure`, а запуск Pi-процессов — в `agent`; не добавлять новую базу данных.

## Capabilities

### New Capabilities

- Нет.

### Modified Capabilities

- `workflow-temporal`: длительное single-execution workflow, signals, queries и durable ожидание вместо отдельных Analyst/Architect executions.
- `workflow-lifecycle`: ручные gates, rework/continue внутри одного execution и единое состояние полного flow.
- `workflow-project-analysis`: Analyst activity и результат анализа становятся этапом long-running workflow с сохранением текущей валидации артефактов.
- `agent-roles`: явное сопоставление ролей с этапами и профильными activities без orchestration внутри agent.
- `workflow-runtime`: публичное чтение актуального состояния long-running execution и обработка signal/query через application-порт.
- `web-workflow-analysis`: UI отображает этап, результат и доступные ручные действия одного workflow execution.

## Impact

Затрагиваются domain/application/api контракты `workflow`, Temporal interfaces/implementation и adapter, activity inputs/results, `WorkflowService`, web controller/template, agent role resources и тесты. Удаляется модель выбора `WorkflowExecutionKind` для запуска разных execution и синхронизация результатов через `getResult`/polling; вместо неё появляется стабильный workflow id, query state и signal operations. Developer/Reviewer activities как полная реализация, pull request, approval вне описанных gates, генерация OpenSpec Proposal и новая БД не входят в scope.
