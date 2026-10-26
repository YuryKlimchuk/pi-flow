# Spec Delta

## ADDED Requirements

### Requirement: Страница списка workflow
Web SHALL предоставлять GET-страницу списка сохранённых workflow с колонками workflow id, repository, issue, status и current stage.

#### Scenario: Список доступен
- **WHEN** пользователь открывает страницу списка workflow
- **THEN** web отображает сохранённые workflow и ссылку перехода к каждому статусу

### Requirement: Пустой список
Web SHALL показывать явно обозначенное пустое состояние, если список workflow не содержит элементов.

#### Scenario: Нет workflow
- **WHEN** операция списка возвращает пустой список
- **THEN** страница сообщает, что запущенные workflow отсутствуют, без ошибки сервера

### Requirement: Ошибка списка
Web SHALL показывать безопасное сообщение об ошибке, если операция списка workflow завершается ошибкой.

#### Scenario: Ошибка repository
- **WHEN** получение списка завершается ошибкой
- **THEN** страница показывает сообщение без stack trace и не отображает неполные строки
