# Proposal

## Why

Текущий Temporal workflow завершается после подготовки workspace и не выполняет ни одного содержательного этапа анализа. Поэтому последующие этапы не получают проверяемый артефакт понимания Issue и проекта, а workflow может сообщить об успешном завершении без результата работы Analyst.

## What Changes

- Добавить в Temporal execution отдельную activity анализа проекта, выполняемую только после успешной подготовки workspace.
- Передавать activity исходный контекст GitHub Issue и путь к подготовленному workspace через явные сериализуемые модели.
- Запускать Analyst через публичный `agent.api`, используя существующую роль `ANALYST`, без зависимости модуля `agent` от Temporal.
- Ввести минимальный предсказуемый контракт артефакта анализа: сохраняемый файл в workspace и результат activity с путём и необходимыми метаданными.
- Валидировать статус запуска агента, наличие и содержимое артефакта; ошибки запуска, ненулевой exit code, timeout, пустой/нечитаемый результат SHALL препятствовать успешному завершению workflow.
- Сохранить существующую точку входа из GitHub Issue и порядок подготовки workspace; не переносить Git-операции или запуск Pi в реализацию workflow.
- Перевести `ProcessAgentRunner` на подтверждённый CLI-контракт Pi v1.4.0 (`--print`, `--append-system-prompt`, `@path`/сообщения, exit code) и передавать описание роли Analyst из ресурсов `roles/analyst/` (системный промпт и скилы, на первом этапе — упрощённые описания); сохранить порт process runner и поддержку `FakePi`.
- Разрешать базовую ветку подготовки workspace: предпочтительная ветка (например, `master`), если она отсутствует в remote — `main`.
- Добавить unit-тесты activity, Temporal integration-тесты порядка и отказов, а также тесты подтверждённого контракта process runner.

## Capabilities

### New Capabilities

- `workflow-project-analysis`: выполнение Analyst после подготовки workspace, сохранение и валидация артефакта анализа.

### Modified Capabilities

- `workflow-temporal`: Temporal workflow SHALL координировать preparation activity и analysis activity, возвращая успешный результат только после валидного анализа.
- `agent-module-contract`: публичный запуск Analyst SHALL использоваться с контекстом Issue/workspace и проверяемым результатом, не раскрывая инфраструктуру агентского процесса.
- `agent-process`: процессный адаптер SHALL сохранять подтверждённую совместимость с CLI-контрактом и существующей fake-process тестируемостью; неподтверждённые детали SHALL оставаться изолированными.
- `agent-module-verification`: тесты SHALL покрывать запуск Analyst из workflow activity и различать fake-process проверки и проверку реального CLI.
- `workspace-preparation`: подготовка workspace SHALL разрешать базовую ветку — предпочтительную, если она есть в remote, иначе `main`.

## Impact

Затрагиваются Temporal workflow и activity-адаптеры модуля `workflow`, модели входов/результатов, интеграция с публичным `agent.api`, сохранение артефакта в подготовленном workspace, ресурсы роли Analyst (`src/main/resources/roles/analyst/`), минимальная правка резолва базовой ветки в `workspace` и связанные unit/integration-тесты. `ProcessAgentRunner` переводится на подтверждённый CLI-контракт Pi v1.4.0 — локальная проверка выявила несовместимость текущих `--role`/`--run-id`/stdin с реальным CLI. Модули `github`, `web` и доменная модель не получают новой ответственности; новая база данных, UI, approval-механизм и последующие роли не добавляются.
