# Tasks

## 1. Загрузка и отображение
- [x] 1.1 Добавить web-сервис/controller с `RepositoryOperations` и `PageRequest`; проверить вызов fake-портом.
- [x] 1.2 Реализовать маппинг `RepositorySummary` в repository view-модель и Thymeleaf-таблицу owner/name; проверить HTML MVC-тестом.
- [x] 1.3 Добавить пустое состояние и безопасную ветку GithubException; проверить empty/error сценарии.

## 2. Навигация и страницы
- [x] 2.1 Добавить ссылки repository → Issues с owner/repository; проверить URL-контекст.
- [x] 2.2 Добавить next/previous из `hasNext` и page; проверить граничные страницы.
- [x] 2.3 Убедиться в отсутствии RestClient и импортов infrastructure/workspace/agent; выполнить `./mvnw -q compile`.

## 3. Проверка
- [x] 3.1 Выполнить `./mvnw -q test`.
- [x] 3.2 Выполнить `openspec validate 2026-10-25-25-web-repositories`.
