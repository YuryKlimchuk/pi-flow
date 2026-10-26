# Proposal

## Why

Страница статуса сейчас показывает только актуальный результат и теряет контекст после Continue или Rework, поэтому пользователь не может восстановить ход workflow и увидеть результаты предыдущих этапов. Архитектор также публикует произвольный Markdown вместо проверяемого proposal, а закрытие страницы статуса лишает пользователя навигации обратно к запущенным workflow.

## What Changes

- Добавить в публичное состояние workflow накопительную timeline завершённых шагов с ролью, действием, результатом/артефактом, временем и идентификатором запуска.
- Изменить доменную фиксацию результатов и переходы так, чтобы каждый завершённый этап и каждый результат rework сохранялись в истории и не исчезали после Continue.
- Перерисовать страницу статуса как timeline: показывать все завершённые блоки, текущий выполняемый шаг и действия Continue/Rework только для актуального ручного gate.
- Формализовать результат Architect как proposal с обязательными разделами `Why`, `What Changes`, `Capabilities`, `Impact` и `Non-goals`; обновить инструкцию роли, артефакт и валидацию.
- Добавить публичную операцию получения списка запущенных workflow, repository-порт `findAll`/`list`, endpoint списка и шаблон со ссылками на `/workflows/track`.
- Сохранить in-memory хранение как ограничение: список и timeline не переживают рестарт приложения.

## Capabilities

### New Capabilities
- `workflow-status-timeline`: накопительная история шагов и результатов, доступная публичному API и отображаемая web timeline.
- `workflow-list`: получение списка запущенных workflow и навигация к их страницам статуса.
- `architect-proposal`: формальный proposal-артефакт Architect и его представление в состоянии workflow.

### Modified Capabilities
- `workflow-lifecycle`: состояние workflow SHALL хранить результаты завершённых шагов и сохранять их при переходах Continue/Rework.
- `workflow-temporal`: execution SHALL передавать идентификаторы запусков и результаты этапов, не меняя модель одного длительного Temporal workflow.
- `web-workflow-analysis`: страница статуса SHALL отображать timeline и действия для актуального этапа вместо только текущего результата.
- `web-ui-states`: web SHALL предоставлять страницу списка workflow и состояния loading/error для неё.
- `agent-roles`: Architect SHALL создавать proposal фиксированного формата.
- `workflow-project-analysis`: чтение результата Architect SHALL валидировать proposal-структуру.

## Impact

Затрагиваются `workflow` domain/api/application и in-memory infrastructure, Temporal-адаптер и DTO, `web` controller/templates, `agent` resources и Architect role prompt, а также `workspace` чтение/валидация артефактов. Публичные `WorkflowState` и `WorkflowOperations` расширяются; добавляется операция списка. Temporal, агент и workspace остаются за своими границами модулей. Персистентное хранилище, восстановление после рестарта и изменение жизненного цикла одного длительного execution в scope не входят.
