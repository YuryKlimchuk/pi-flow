# Proposal

## Why

Сейчас запуск `workflow` принимает только URL репозитория, синхронно подготавливает workspace и запускает `agent`, а Temporal activity остаётся заглушкой. Из-за этого пользователь не может начать процесс по конкретной Issue через web UI, а минимальный сценарий не проверяет реальное клонирование репозитория в Temporal execution.

## What Changes

Этот change реализует **не полный end-to-end сценарий**, а только его начальный фрагмент: от выбора Issue через UI до клонирования репозитория и старта Temporal execution включительно. Запуск Pi/агента, продвижение по стадиям разработки, review loops и пользовательские согласования в этот change НЕ входят.

- Добавить web-форму запуска workflow по `owner`, `repository` и положительному номеру Issue без Spring Shell.
- Изменить публичную команду запуска так, чтобы она формировала контекст Issue и URL репозитория из выбранных owner/repository.
- Изменить минимальный порядок `WorkflowService.start` на `create → execution.start`; `agent.run` не вызывается.
- Передавать в Temporal execution workflow id, owner, repository, Issue number и параметры подготовки workspace.
- Реализовать `WorkflowPreparationActivitiesImpl.prepare` через существующий `workspace.api.WorkspaceOperations.prepare`; activity SHALL клонировать репозиторий и создавать/переключать ветку `pi-flow/<workflowId>`.
- Сохранить существующие публичные контракты `WorkflowOperations`, `WorkspaceOperations` и `TemporalWorkflowAdapter`, расширяя их только при необходимости для данного сценария.
- Добавить unit-тесты сервиса, тест activity с локальным JGit-репозиторием, архитектурные тесты и e2e-тест «Issue UI → clone → Temporal execution».
- Документировать проверку e2e против Temporal, запущенного Docker Compose; для изолированных тестов допускается `TestWorkflowEnvironment`.

## Capabilities

### New Capabilities

- `workflow-issue-start`: запуск workflow через web UI по конкретной Issue с публичным контрактом входных данных.

### Modified Capabilities

- `workflow-lifecycle`: изменить входной контекст запуска и исключить запуск агента из минимального сценария.
- `workflow-temporal`: перенести подготовку workspace внутрь Temporal activity и передавать в execution полный контекст Issue.
- `workflow-runtime`: уточнить Docker Temporal как обязательную среду проверки сквозного сценария.
- `web-issues`: добавить действие запуска workflow для выбранной Issue.
- `workspace-preparation`: зафиксировать подготовку ветки `pi-flow/<workflowId>` как результат workflow activity при clone.

## Impact

Изменение покрывает **частичный (не полный e2e) сценарий**: «Issue → web-форма → Temporal execution → клонирование репозитория → ветка `pi-flow/<workflowId>`». Дальнейшие стадии и запуск Pi остаются за пределами изменения.

Затрагиваются web-контроллеры, Thymeleaf-шаблоны и view-модели, `workflow.api/application/infrastructure/temporal`, интеграция с `workspace.api`, тесты и документация Docker/e2e. Модуль `agent` не изменяется и не вызывается в новом минимальном сценарии. Git/JGit-операции остаются ответственностью `workspace`; workflow не дублирует GitHub или Pi-функциональность. Изменение не добавляет Spring Shell-команд, новые бизнес-модули или запуск Pi.
