# Spec Delta

## MODIFIED Requirements

### Requirement: Модель роли
Команда запуска SHALL представлять `ANALYST`, `ARCHITECT`, `DEVELOPER` и `REVIEWER` устойчивыми идентификаторами, связанными соответственно с анализом требований, проектированием решения, реализацией и ревью.

#### Scenario: Запуск Reviewer
- **WHEN** вызывающая сторона указывает роль `Reviewer`
- **THEN** роль попадает в metadata запуска и используется для выбора инструкции.

#### Scenario: Сопоставление Architect
- **WHEN** workflow запускает этап проектирования
- **THEN** agent получает роль `ARCHITECT`, а запуск не выполняет Analyst, Developer или Reviewer orchestration

### Requirement: Отсутствие workflow-оркестрации
Модуль `agent` SHALL выполнять только запрошенный запуск роли и SHALL NOT определять порядок этапов, ручные gates, переходы или обработку сигналов.

#### Scenario: Вызов одной роли
- **WHEN** long-running workflow запрашивает запуск `ANALYST` или `ARCHITECT`
- **THEN** agent выполняет только этот запуск и возвращает технический статус/run metadata через `agent.api`
