# Tasks

## 1. Структура и UI
- [x] 1.1 Проверить package-info и границу `web`, затем добавить только необходимые web-пакеты; подтвердить `ApplicationModules.verify()`.
- [x] 1.2 Зафиксировать Spring MVC + Thymeleaf без новых UI-зависимостей; подтвердить `./mvnw -q compile`.
- [x] 1.3 Создать layout и главную страницу в `src/main/resources/templates/`; проверить `GET /` и наличие навигации MVC-тестом.

## 2. Маршруты и view-модели
- [x] 2.1 Создать маршруты/контроллеры репозиториев и Issues; проверить HTTP status и имена view через MVC-тест.
- [x] 2.2 Создать собственные record view-модели и маппинг из `RepositorySummary`/`IssueSummary`; проверить отсутствие api-моделей в model attributes.
- [x] 2.3 Проверить отсутствие Workflow/Git/REST-логики в web и выполнить `./mvnw -q test`.
- [x] 2.4 Выполнить `openspec validate 2026-10-25-23-web-ui-foundation`.
