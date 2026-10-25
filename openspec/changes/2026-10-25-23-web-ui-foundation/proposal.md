# Proposal

## Why

Модуль `web` имеет только приветственную страницу, поэтому пользователь не может вручную проверить интеграцию pi-flow с GitHub. Нужен тонкий Spring MVC-адаптер, который создаёт основу интерфейса без переноса бизнес-логики в web.

## What Changes

- Создать завершённую структуру Spring Modulith-модуля `web`.
- Зафиксировать Spring MVC + Thymeleaf как технологию UI.
- Добавить общий layout, главную страницу и маршруты репозиториев и Issues.
- Ввести view-модели web, отделённые от `github.api`.
- Не добавлять запуск Workflow, операции Git или новый GitHub REST-клиент.

## Capabilities

### New Capabilities
- `web-ui-foundation`: браузерная основа интерфейса, страницы и представления GitHub-данных.

### Modified Capabilities
- Нет.

## Impact

Затрагиваются `web`, Thymeleaf-шаблоны и контроллеры; публичные контракты `github.api` не изменяются. Change зависит от реализованного контракта `github` и является базой для changes 24–28.
