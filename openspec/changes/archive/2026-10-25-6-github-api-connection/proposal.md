# Proposal

## Why

После `github-module-contract` существует публичная граница, но нет подключения к удалённому GitHub API. Этот change использует контракт из `github-module-contract` и добавляет RestClient, конфигурацию, аутентификацию и безопасные ошибки.

## What Changes

- Добавляется `RestClient` с base URL и тайм-аутами.
- Добавляются typed properties URL, токена и размера страницы из внешней конфигурации.
- Добавляется Authorization и поведение конфигурационной ошибки без токена.
- HTTP-ошибки и сетевые тайм-ауты преобразуются в типизированные ошибки API.

## Capabilities

### New Capabilities
- `github-api-connection`: конфигурация и аутентификация подключения GitHub API.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются зависимости и `github.infrastructure`; публичные модели и порты из `github-module-contract` сохраняются. Change 3 использует готовый адаптер.
