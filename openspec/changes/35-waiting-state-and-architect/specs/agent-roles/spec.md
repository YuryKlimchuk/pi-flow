# Spec Delta

## MODIFIED Requirements

### Requirement: Профильная инструкция
Модуль SHALL разрешать профильную инструкцию через поддерживаемый источник профилей и SHALL передавать её выбранному Pi-запуску; профили `ANALYST` и `ARCHITECT` SHALL использовать раздельные ресурсы.

#### Scenario: Профиль Architect
- **WHEN** задан профиль `Architect` с инструкцией
- **THEN** только этот запуск получает соответствующую профильную инструкцию

#### Scenario: Ресурсы Architect
- **WHEN** workflow запускает роль `ARCHITECT`
- **THEN** Pi получает system prompt и skills из `roles/architect/`, а ресурсы Analyst не используются как профиль Architect

### Requirement: Контекст задачи
Модуль SHALL передавать конкретному запуску instruction, input data, context и рабочую директорию из его команды.

#### Scenario: Контекст repository
- **WHEN** запуск получает контекст репозитория и задачу
- **THEN** Pi получает оба значения, а результат содержит идентификатор этого запуска

#### Scenario: Контекст Architect
- **WHEN** Architect получает Issue и подготовленный workspace
- **THEN** Pi получает оба значения и путь workspace, в котором должен сохранить design-артефакт

### Requirement: Изоляция запусков
Каждый запуск SHALL иметь уникальный идентификатор и SHALL NOT читать или изменять входы, инструкции и результаты другого активного запуска.

#### Scenario: Параллельные роли
- **WHEN** Analyst и Reviewer запускаются одновременно с разными контекстами
- **THEN** каждый процесс получает только свои данные и возвращает свой run id

### Requirement: Возврат через публичный контракт
Результат роли SHALL возвращаться через `agent.api` и SHALL содержать технический статус и role/run metadata.

#### Scenario: Получение результата
- **WHEN** Pi завершает запуск роли
- **THEN** workflow получает result через публичный порт без доступа к infrastructure.

### Requirement: Отсутствие workflow-оркестрации
Модуль `agent` SHALL NOT определять порядок ролей, переходы этапов или approval gates.

#### Scenario: Вызов одной роли
- **WHEN** workflow запрашивает запуск роли
- **THEN** agent выполняет только этот запуск и не инициирует следующую роль.
