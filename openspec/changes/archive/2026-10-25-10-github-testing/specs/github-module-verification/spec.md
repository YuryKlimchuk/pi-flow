# Spec Delta

## Purpose

Фиксирует проверяемость модуля `github` через независимые contract-тесты, локальные HTTP-интеграционные тесты и архитектурные проверки сборки.

## ADDED Requirements

### Requirement: Contract-тесты публичных портов
Тестовый набор SHALL проверять успешные операции публичных портов, валидацию входных данных и типизированные ошибки без обращения к реальному GitHub.

#### Scenario: Контрактный успешный вызов
- **WHEN** contract-тест вызывает публичный порт с валидными данными
- **THEN** тест проверяет ожидаемую публичную модель результата без внешней сети.

### Requirement: Интеграционные тесты адаптера
Тестовый набор SHALL проверять адаптер через локальный mock HTTP server для Authorization, пагинации, Issue/PR distinction и идемпотентности PR.

#### Scenario: Идемпотентность через mock server
- **WHEN** mock server возвращает существующий открытый PR
- **THEN** тест подтверждает возврат его номера/URL и отсутствие POST создания.

### Requirement: Архитектурные направления
Архитектурные проверки SHALL подтверждать направление `web → workflow → {workspace, agent, interaction, github}`, отсутствие циклов и отсутствие прямой зависимости `workflow` от `github.infrastructure`.

#### Scenario: Проверка Modulith
- **WHEN** выполняется `ApplicationModules.of(PiFlowApplication.class).verify()` и направленные правила
- **THEN** проверки завершаются успешно.

### Requirement: Успешная сборка
Проект SHALL успешно завершать `./mvnw -q compile` и `./mvnw -q test` после реализации модуля GitHub.

#### Scenario: Финальная проверка
- **WHEN** запускаются compile и test в чистом рабочем дереве зависимостей
- **THEN** обе команды завершаются с кодом 0.

### Requirement: Валидация OpenSpec
Набор planning-артефактов SHALL проходить `openspec validate 2026-10-25-10-github-testing` с результатом `valid`.

#### Scenario: Валидация change
- **WHEN** запускается указанная команда OpenSpec
- **THEN** результат содержит `valid` и не содержит ошибок структуры или требований.
