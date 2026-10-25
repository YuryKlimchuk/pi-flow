# Proposal

## Why

После `github-repositories` доступен контекст репозиториев, но workflow не может читать Issues и отличать их от Pull Requests. Этот change использует контракт из `github-module-contract` и REST-подключение из `github-api-connection` для списка и деталей Issues.

## What Changes

- Реализуется пагинированный список Issues с фильтрами и `IssueSummary`.
- Реализуется `pullRequest` по данным GitHub, чтобы PR не считался обычной задачей.
- Реализуются детали Issue и маппинг 404 в not found.
- Добавляются тесты пагинации и ошибок 401/403/422/429/5xx.

## Capabilities

### New Capabilities
- `github-issues`: список, детали и distinction Issue/PR.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются application/infrastructure операции Issues; change последователен после `github-repositories` и использует его модели/конфигурацию, не меняя публичные границы workflow.
