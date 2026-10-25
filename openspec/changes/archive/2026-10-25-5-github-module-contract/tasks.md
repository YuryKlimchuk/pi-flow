# Tasks

## 1. Каркас модуля и публичный контракт

- [x] 1.1 Создать модели `RepositorySummary`, страницы репозиториев, `IssueSummary`, `IssueDetails`, `BranchPublication`, `CreatePullRequestCommand` и `PullRequestResult` в `github.api`.
- [x] 1.2 Создать отдельные публичные порты репозиториев, Issues, публикации ветки и PR с типизированными результатами и ошибками.
- [x] 1.3 Создать внутренний порт GitHub API и application-оркестраторы, делегирующие операции и преобразующие результаты.
- [x] 1.4 Добавить unit-тесты делегирования и валидации публичных команд.
- [x] 1.5 Добавить архитектурную проверку `ApplicationModules.of(PiFlowApplication.class).verify()` и отсутствие импорта `github.infrastructure` из `workflow`.
- [x] 1.6 Проверить `./mvnw -q compile` и `openspec validate 2026-10-25-5-github-module-contract`.
