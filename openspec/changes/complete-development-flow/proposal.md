# Proposal

## Why

Сквозной workflow сейчас останавливается после Analyst и Architect: роли DEVELOPER и REVIEWER не запускаются, Pull Request не создаётся, а синхронные операции ветки и push вне Temporal оставляют execution в `WAITING_FOR_USER` при доменном статусе `COMPLETED`. Изменение завершит процесс от утверждённого дизайна до проверенного PR и устранит рассогласование между доменным состоянием и Temporal execution.

## What Changes

- Добавить Temporal activities для реализации решения ролью `DEVELOPER`, включая сборку и тесты, с повтором итерации по исправлениям.
- Добавить независимое ревью ролью `REVIEWER` с gate `continue/rework` и возвратом замечаний в реализацию.
- Добавить Temporal-оркестрирование финализации: создание/переключение итоговой ветки, коммит, push и создание Pull Request через публичный контракт `github`.
- Перенести create-branch, commit/push и PR из синхронных действий `WorkflowService` в activities единого Temporal execution либо обеспечить эквивалентное корректное завершение execution.
- Согласовать доменные этапы `IMPLEMENTING`, `REVIEWING`, `READY_FOR_PULL_REQUEST`, терминальные статусы и освобождение workspace lock.
- Ограничить итерации rework максимум тремя для каждого применимого цикла и разделить повторяемые ошибки от фатальных.

## Capabilities

### New Capabilities

- `workflow-implementation`: выполнение реализации Developer и обработка повторяемых ошибок сборки/тестов.
- `workflow-review`: независимое ревью Reviewer и gate approve/rework.
- `workflow-finalization`: финализация ветки, публикация и создание Pull Request с терминальным состоянием.

### Modified Capabilities

- `workflow-lifecycle`: переходы после Architect, состояния реализации/ревью/финализации, лимиты rework и terminal cleanup.
- `workflow-temporal`: единое Temporal execution для всех activities, signals, ошибок и финализации.
- `workflow-branch-publication`: публикация становится частью Temporal finalization и завершается созданием PR, а не статусом `COMPLETED` после одного push.

## Impact

Затрагиваются `workflow` domain/application/Temporal infrastructure, публичные порты `workspace`, `agent` и `github`, web-команды и отображение состояния, а также тесты Temporal workflow/activity и доменных переходов. Новые модули не требуются; Temporal SDK остаётся только в infrastructure/application adapter, а домен и `workflow.api` сохраняют независимость от Temporal.
