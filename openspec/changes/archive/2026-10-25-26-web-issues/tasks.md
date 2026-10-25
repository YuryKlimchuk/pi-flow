# Tasks

## 1. Issues controller
- [x] 1.1 Добавить вызов `IssueOperations.listIssues` с owner/repository/filter/page; проверить fake-портом.
- [x] 1.2 Создать issue view-модель и маппинг номера/title/state/url; проверить HTML MVC-тестом.
- [x] 1.3 Реализовать пустое состояние и обработку GithubException; проверить empty/error сценарии.

## 2. Навигация и пагинация
- [x] 2.1 Добавить возврат к репозиториям и ссылки страниц Issues с сохранением owner/repository.
- [x] 2.2 Реализовать next/previous по `IssuePage.hasNext` и page; проверить URL-параметры.
- [x] 2.3 Подтвердить отсутствие нового REST-клиента и прямого доступа к github.infrastructure; выполнить `./mvnw -q compile`.

## 3. Проверка
- [x] 3.1 Выполнить `./mvnw -q test`.
- [x] 3.2 Выполнить `openspec validate 2026-10-25-26-web-issues`.
