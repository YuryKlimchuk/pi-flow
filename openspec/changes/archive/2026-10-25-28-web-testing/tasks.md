# Tasks

## 1. MVC и контрактные тесты
- [x] 1.1 Добавить MVC-тесты маршрутов, controller/view и главной страницы; проверить status, view и ключевой HTML.
- [x] 1.2 Подменить `RepositoryOperations` и `IssueOperations`; проверить успешные репозитории, Issues, пустые результаты и пагинацию.
- [x] 1.3 Проверить configuration/authentication/network/rate-limit сценарии и retry; выполнить тесты без сети.

## 2. Безопасность и архитектура
- [x] 2.1 Добавить sentinel PAT и проверить его отсутствие в HTML, captured logs и error text.
- [x] 2.2 Добавить архитектурные проверки запрета RestClient/GitHub REST, `github.infrastructure`, workspace/agent и Workflow в web.
- [x] 2.3 Проверить Modulith-направления и отсутствие циклов.

## 3. Интеграционная проверка
- [x] 3.1 Выполнить `./mvnw -q compile`.
- [x] 3.2 Выполнить `./mvnw -q test`.
- [x] 3.3 Выполнить `openspec validate 2026-10-25-23-web-ui-foundation`.
- [x] 3.4 Выполнить `openspec validate 2026-10-25-24-web-github-configuration`.
- [x] 3.5 Выполнить `openspec validate 2026-10-25-25-web-repositories`.
- [x] 3.6 Выполнить `openspec validate 2026-10-25-26-web-issues`.
- [x] 3.7 Выполнить `openspec validate 2026-10-25-27-web-ui-states`.
- [x] 3.8 Выполнить `openspec validate 2026-10-25-28-web-testing`.
