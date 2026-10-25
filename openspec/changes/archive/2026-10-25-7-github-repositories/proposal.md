# Proposal

## Why

После `github-api-connection` модуль умеет безопасно обращаться к GitHub, но не предоставляет список доступных репозиториев. Этот change использует публичный контракт `github-module-contract` и подключённый адаптер из предыдущего change для пагинированного получения репозиториев.

## What Changes

- Реализуется endpoint доступных репозиториев и маппинг в `RepositorySummary`.
- Добавляются page/perPage, Link header и `hasNext`.
- Добавляется валидация параметров до HTTP и typed mapping 401/403, 429, 5xx и timeout.

## Capabilities

### New Capabilities
- `github-repositories`: пагинированное получение репозиториев.

### Modified Capabilities
- Нет.

## Impact

Затрагивается реализация чтения репозиториев в `github.application`/`github.infrastructure`; change зависит от `github-api-connection`. Остальные GitHub-операции не изменяются.
