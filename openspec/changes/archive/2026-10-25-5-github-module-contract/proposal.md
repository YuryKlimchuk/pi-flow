# Proposal

## Why

Каркас `github` из модульного монолита пока не предоставляет workflow стабильного публичного контракта. Первый последовательный change выделяет модели, порты и application-оркестрацию, чтобы последующие интеграционные изменения не раскрывали HTTP-детали.

## What Changes

- Вводятся публичные модели репозиториев, Issues, публикации ветки и Pull Request в `github.api`.
- Вводятся отдельные порты чтения и публикации и application-реализации, делегирующие внутреннему порту GitHub API.
- Фиксируются границы Spring Modulith и запрет прямой зависимости `workflow` от `github.infrastructure`.

## Capabilities

### New Capabilities
- `github-module-contract`: публичный контракт и изоляция модуля GitHub.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются пакеты `github.api`, `github.application`, `github.domain`; сетевой адаптер и HTTP-зависимости не реализуются. Change 2 SHALL использовать этот контракт.
