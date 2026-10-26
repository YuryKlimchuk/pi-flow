# Spec Delta

## Purpose
Формализует результат Architect как proposal, который можно проверить, отобразить в timeline и использовать как договорённость о проектном решении.

## ADDED Requirements

### Requirement: Формат Architect proposal
Architect SHALL создавать непустой Markdown-артефакт proposal с разделами `Why`, `What Changes`, `Capabilities`, `Impact` и `Non-goals`.

#### Scenario: Валидный proposal
- **WHEN** Architect завершает проектирование
- **THEN** артефакт содержит каждый обязательный раздел и текстовое содержимое каждого раздела

### Requirement: Валидация proposal
Workflow SHALL отклонять результат Architect, если отсутствует обязательный раздел или его содержимое пусто.

#### Scenario: Невалидный артефакт
- **WHEN** Architect записывает Markdown без `Capabilities` или с пустым `Why`
- **THEN** этап завершается ошибкой, а workflow не публикует proposal как успешный результат

### Requirement: Отображение proposal
Публичное состояние и web SHALL передавать и отображать Architect result как proposal, сохраняя его разделы и Markdown-содержимое.

#### Scenario: Proposal на статусной странице
- **WHEN** Architect завершён и proposal валиден
- **THEN** timeline показывает proposal отдельным блоком с указанными разделами
