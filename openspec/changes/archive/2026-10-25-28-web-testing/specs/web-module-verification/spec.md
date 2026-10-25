# Spec Delta

## Purpose

Фиксирует автоматически проверяемое поведение web-модуля, безопасность PAT и архитектурные границы интеграции с github.

## ADDED Requirements

### Requirement: Тесты маршрутов и страниц
Проект SHALL иметь автоматические тесты маршрутов, контроллеров и HTML-страниц главной, репозиториев и Issues.

#### Scenario: Контракт страницы
- **WHEN** запускаются web MVC-тесты
- **THEN** проверяются HTTP-статус, view и ключевые элементы HTML для каждой страницы

### Requirement: Подменяемый github-контракт
Тесты web SHALL подменять `RepositoryOperations` и `IssueOperations` и SHALL проверять отображение успешных результатов без сетевого GitHub.

#### Scenario: Репозитории и Issues
- **WHEN** fake/mock-порт возвращает страницы с данными
- **THEN** тесты подтверждают отображение owner/name и number/title/state/url соответственно

### Requirement: Ошибки и отсутствие PAT
Тесты SHALL проверять configuration, authentication, network и rate-limit ошибки и соответствующие безопасные сообщения UI.

#### Scenario: Безопасная ошибка
- **WHEN** подменённый порт выбрасывает `GithubException`
- **THEN** тест проверяет ожидаемый текст ошибки и отсутствие stack trace и токена

### Requirement: Отсутствие утечки PAT
Автоматическая проверка SHALL подтверждать отсутствие тестового PAT в HTML, логируемых сообщениях и тексте исключений web.

#### Scenario: Поиск секрета
- **WHEN** обработан ответ с ошибкой при тестовом PAT
- **THEN** ни один проверяемый HTML/лог/текст ошибки не содержит значение PAT

### Requirement: Архитектурные границы web
Архитектурные тесты SHALL запрещать web прямые вызовы GitHub REST, импорты `github.infrastructure`, Git-операции и запуск Workflow.

#### Scenario: Проверка зависимостей
- **WHEN** выполняются Modulith/ArchUnit-проверки
- **THEN** web зависит от публичных контрактов, а запрещённые зависимости и циклы отсутствуют

### Requirement: Командная проверка
Change SHALL быть проверяемым командами `./mvnw -q compile`, `./mvnw -q test` и `openspec validate` для каждого web change.

#### Scenario: Полная проверка
- **WHEN** команды запускаются в корне проекта
- **THEN** компиляция, тесты и валидация шести changes завершаются успешно
