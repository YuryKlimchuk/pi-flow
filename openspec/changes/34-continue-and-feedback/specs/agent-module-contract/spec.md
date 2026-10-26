# Spec Delta

## MODIFIED Requirements

### Requirement: Входная модель запуска
Команда запуска SHALL поддерживать необязательный пользовательский feedback, который передаётся Analyst как данные запуска и не меняет публичную изоляцию agent API.

#### Scenario: Переданы все входные данные
- **WHEN** команда содержит `role`, `instruction`, `inputData` и параметры
- **THEN** каждый элемент доступен application-слою без чтения его из workflow-состояния

#### Scenario: Feedback передан
- **WHEN** workflow запускает Analyst с feedback
- **THEN** `agent.api` предоставляет feedback application-слою процесса как отдельное значение входной команды

#### Scenario: Feedback отсутствует
- **WHEN** workflow запускает первый анализ без feedback
- **THEN** команда остаётся валидной и Analyst запускается без пользовательских замечаний
