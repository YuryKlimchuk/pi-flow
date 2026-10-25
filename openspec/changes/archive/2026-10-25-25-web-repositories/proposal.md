# Proposal

## Why

Пользователь должен видеть репозитории, доступные настроенному PAT, и переходить к Issues. Change расширяет основу 23 и безопасную конфигурацию 24 вызовом публичного `RepositoryOperations`, без прямого GitHub API в web.

## What Changes

- Загружать репозитории через `github.api.RepositoryOperations.listRepositories`.
- Отображать владельца и имя, включая пустые результаты и ошибки.
- Добавить переход к Issues и навигацию страницами через `PageRequest`/`hasNext`.
- Зафиксировать отсутствие REST-клиента и доступа к инфраструктуре github.

## Capabilities

### New Capabilities
- `web-repositories`: просмотр доступных репозиториев и навигация к Issues.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются контроллер, view-модели и шаблоны `web`; используется существующий публичный порт `RepositoryOperations`. Change зависит от 23–24 и является входом для 26–28.
