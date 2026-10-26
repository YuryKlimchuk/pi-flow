# Spec Delta

## MODIFIED Requirements

### Requirement: Входная модель запуска
Команда запуска SHALL содержать роль или профиль, инструкцию, входные данные и параметры запуска, необходимые для одного изолированного запуска; `ARCHITECT` SHALL поддерживаться тем же контрактом, что и `ANALYST`.

#### Scenario: Переданы все входные данные
- **WHEN** команда содержит `role`, `instruction`, `inputData` и параметры
- **THEN** каждый элемент доступен application-слою без чтения его из workflow-состояния

#### Scenario: Передан Architect
- **WHEN** workflow передаёт роль `ARCHITECT`, инструкцию, Issue-контекст и workspace
- **THEN** agent принимает команду через `agent.api` без специального workflow-сервиса Architect

### Requirement: Контракт запуска Analyst из workflow
Публичный API `agent` SHALL принимать сериализуемый контекст Analyst, включая feedback и предыдущий результат анализа, через существующую модель команды и возвращать технический результат, достаточный для проверки сохранённого артефакта.

#### Scenario: Workflow вызывает Analyst
- **WHEN** activity передаёт роль `ANALYST`, инструкцию, Issue-контекст, путь workspace и параметры результата
- **THEN** `agent.api` возвращает `AgentRunResult` без раскрытия инфраструктурной реализации процесса

#### Scenario: Rework Analyst
- **WHEN** activity передаёт роль `ANALYST`, feedback и путь либо текст предыдущего `analyst.md`
- **THEN** agent возвращает `AgentRunResult` через `agent.api`, не раскрывая детали процесса

## ADDED Requirements

### Requirement: Контракт запуска Architect
Публичный API `agent` SHALL принимать роль `ARCHITECT` и возвращать технический результат запуска, по которому workflow может проверить сохранённый design-артефакт.

#### Scenario: Успешный Architect
- **WHEN** Pi завершает Architect с успешным статусом
- **THEN** вызывающая сторона получает run id, статус и exit code через `agent.api`, а содержимое design-артефакта остаётся в workspace
