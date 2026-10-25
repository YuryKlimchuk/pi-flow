# Tasks

## 1. Конфигурация и безопасность
- [x] 1.1 Проверить `github.api.token: ${GITHUB_PI_FLOW_TOKEN:}` и документировать локальный внешний источник без добавления секрета в Git; подтвердить `./mvnw -q compile`.
- [x] 1.2 Подключить обработку configuration/authentication через `GithubException`; проверить отсутствие PAT в HTML и exception text.
- [x] 1.3 Реализовать безопасное состояние подключения; проверить success и missing-token MVC-сценарии.

## 2. Проверка
- [x] 2.1 Проверить, что web не читает PAT напрямую и не импортирует `github.infrastructure`; выполнить архитектурный тест.
- [x] 2.2 Выполнить `./mvnw -q test`.
- [x] 2.3 Выполнить `openspec validate 2026-10-25-24-web-github-configuration`.
