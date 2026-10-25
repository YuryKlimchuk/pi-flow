# web-issues Specification

## Purpose
Позволяет пользователю просматривать Issues выбранного репозитория с навигацией и ссылкой на исходную страницу GitHub.

## Requirements

### Requirement: Получение Issues
Web SHALL получать Issues только вызовом `github.api.IssueOperations.listIssues(owner, repository, filter, PageRequest)`.

#### Scenario: Открытие Issues
- **WHEN** пользователь открывает Issues для заданных owner и repository
- **THEN** web передаёт эти значения и параметры страницы в публичный порт github

### Requirement: Отображение Issue
Страница Issues SHALL отображать номер, заголовок, состояние и внешнюю ссылку каждого `IssueSummary`.

#### Scenario: Непустой список
- **WHEN** github возвращает Issues
- **THEN** HTML содержит перечисленные поля и ссылку `IssueSummary.url` для каждого Issue

### Requirement: Навигация репозитория
Страница Issues SHALL сохранять owner и repository в ссылках возврата к репозиториям и перехода между страницами Issues.

#### Scenario: Возврат
- **WHEN** пользователь нажимает ссылку к репозиториям
- **THEN** открывается страница репозиториев, а контекст выбранного репозитория присутствует в ссылках Issues

### Requirement: Пустой результат
Страница Issues SHALL показывать отдельное сообщение при пустом `IssuePage.items`.

#### Scenario: Нет Issues
- **WHEN** github возвращает пустой список
- **THEN** пользователь видит сообщение, что Issues отсутствуют или недоступны для выбранного репозитория

### Requirement: Пагинация Issues
Если `IssuePage.hasNext` равен true, страница SHALL показывать next; при page > 1 SHALL показывать previous с сохранением owner и repository.

#### Scenario: Следующая страница Issues
- **WHEN** результат имеет `hasNext=true`
- **THEN** next URL содержит те же owner/repository и page+1
