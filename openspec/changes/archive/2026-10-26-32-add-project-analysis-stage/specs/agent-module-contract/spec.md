# Spec Delta

## ADDED Requirements

### Requirement: Контракт запуска Analyst из workflow
Публичный API `agent` SHALL принимать сериализуемый контекст Analyst через существующую модель команды и возвращать технический результат, достаточный для проверки успешного завершения и сохранённого артефакта.

#### Scenario: Workflow вызывает Analyst
- **WHEN** activity передаёт роль `ANALYST`, инструкцию, Issue-контекст, путь workspace и параметры результата
- **THEN** `agent.api` возвращает `AgentRunResult` без раскрытия инфраструктурной реализации процесса

### Requirement: Изоляция Temporal
Модуль `agent` SHALL NOT импортировать Temporal API или зависеть от реализации Temporal activity.

#### Scenario: Архитектурная проверка
- **WHEN** проверяются зависимости agent API и реализации agent
- **THEN** Temporal-типы отсутствуют в модуле `agent`
