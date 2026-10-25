# github-issues Specification

## Purpose
Предоставляет получение списка и деталей задач (Issues) GitHub с различением pull request.

## Requirements

### Requirement: Список Issues
Публичная операция SHALL возвращать страницу Issues репозитория с номером, заголовком, состоянием, автором, URL и признаком `pullRequest`.

#### Scenario: Список задач
- **WHEN** запрашивается список Issues с репозиторием, фильтрами, `page` и `perPage`
- **THEN** каждый элемент содержит замаппленные поля и результат содержит признак следующей страницы.

### Requirement: Различение Issue и Pull Request
Операция SHALL устанавливать `pullRequest=true`, если элемент GitHub содержит ссылку на Pull Request, и SHALL NOT считать такой элемент обычной задачей.

#### Scenario: Элемент с pull request ссылкой
- **WHEN** ответ содержит поле `pull_request`
- **THEN** элемент возвращается как Pull Request с `pullRequest=true`.

### Requirement: Детали Issue
Операция SHALL возвращать номер, заголовок, описание, состояние, автора, URL и признак Pull Request для указанного номера.

#### Scenario: Issue найден
- **WHEN** GitHub возвращает 200 для `/repos/{owner}/{repo}/issues/{number}`
- **THEN** возвращается полная `IssueDetails` без потери описания.

### Requirement: Issue не найден
Операция SHALL преобразовывать 404 при получении деталей Issue в типизированную ошибку `not found`.

#### Scenario: Неверный номер
- **WHEN** GitHub возвращает 404
- **THEN** возвращается `not found`, а не пустая или частичная модель.

### Requirement: Ошибки и пагинация Issues
Операция SHALL поддерживать пагинацию и SHALL классифицировать 401/403/422/429/5xx как типизированные ошибки с доступным статусом и повторяемостью согласно классу ошибки.

#### Scenario: Некорректный фильтр
- **WHEN** GitHub возвращает 422
- **THEN** вызывающий получает ошибку валидации GitHub со статусом 422.
