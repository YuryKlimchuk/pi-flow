# Proposal

## Why

После выбора репозитория пользователь должен просматривать его Issues и открывать оригинал на GitHub. Change продолжает 25, используя только публичный `IssueOperations` и сохраняя выбранный owner/repository в URL.

## What Changes

- Загружать Issues через `github.api.IssueOperations.listIssues`.
- Отображать номер, заголовок, состояние и ссылку на GitHub.
- Реализовать переходы между репозиториями и Issues, пустые результаты и ошибки.
- Использовать пагинацию контракта github, если `hasNext` установлен.

## Capabilities

### New Capabilities
- `web-issues`: просмотр Issues выбранного репозитория.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются issue-контроллер, view-модели и Thymeleaf-шаблоны `web`; публичный `IssueOperations` не изменяется. Change зависит от 23–25 и подготавливает 27–28.
