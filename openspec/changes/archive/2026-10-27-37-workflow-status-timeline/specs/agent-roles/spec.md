# Spec Delta

## MODIFIED Requirements

### Requirement: Профильная инструкция
Модуль SHALL разрешать профильную инструкцию через поддерживаемый источник профилей, SHALL передавать её выбранному Pi-запуску и SHALL требовать от профиля Architect фиксированный proposal-формат.

#### Scenario: Профиль Architect
- **WHEN** задан профиль `Architect` с инструкцией
- **THEN** только этот запуск получает соответствующую профильную инструкцию, требующую разделы `Why`, `What Changes`, `Capabilities`, `Impact` и `Non-goals`

#### Scenario: Профиль Analyst
- **WHEN** задан профиль `Analyst` с инструкцией
- **THEN** запуск получает инструкцию Analyst без требований Architect proposal

## ADDED Requirements

### Requirement: Proposal Architect
Результат успешного запуска роли Architect SHALL быть proposal-артефактом с обязательными разделами `Why`, `What Changes`, `Capabilities`, `Impact` и `Non-goals`.

#### Scenario: Завершение Architect
- **WHEN** Architect завершает запуск с кодом успеха
- **THEN** его артефакт содержит все обязательные proposal-разделы и непустое содержимое каждого раздела
