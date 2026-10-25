# Spec Delta

## ADDED Requirements

### Requirement: Действие запуска из Issue
Страница Issues SHALL предоставлять действие запуска workflow для конкретной Issue с передачей owner, repository и номера Issue.

#### Scenario: Выбор Issue для запуска
- **WHEN** пользователь открывает Issue с номером `42` в репозитории `acme/demo` и выбирает запуск workflow
- **THEN** открывается форма или выполняется запрос запуска с предзаполненными owner `acme`, repository `demo` и issue number `42`

### Requirement: Результат запуска из Issues
Web SHALL отображать успешный идентификатор workflow или ошибку запуска после отправки выбранной Issue.

#### Scenario: Успешный запуск из списка
- **WHEN** публичный workflow-контракт возвращает состояние с execution id
- **THEN** web отображает пользователю workflow id и execution id без обращения к внутренним пакетам workflow
