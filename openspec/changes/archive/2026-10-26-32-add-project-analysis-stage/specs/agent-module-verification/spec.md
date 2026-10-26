# Spec Delta

## ADDED Requirements

### Requirement: Тестирование analysis activity
Тестовый набор SHALL проверять успешный запуск Analyst через публичный API, передачу роли и workspace, преобразование ошибок агента и отклонение пустого или отсутствующего артефакта.

#### Scenario: Успешная activity
- **WHEN** fake agent возвращает успешный результат и создаёт непустой артефакт
- **THEN** activity возвращает путь артефакта и тест подтверждает роль `ANALYST`

#### Scenario: Невалидный результат
- **WHEN** fake agent возвращает FAILED, TIMED_OUT, пустой stdout или не создаёт артефакт
- **THEN** activity завершается ошибкой валидации или выполнения

### Requirement: Тестирование порядка Temporal
Temporal integration-тесты SHALL проверять порядок preparation → analysis и отсутствие успешного результата при ошибке любого этапа.

#### Scenario: Ошибка preparation
- **WHEN** preparation activity завершается исключением
- **THEN** analysis activity не вызывается и workflow завершается ошибкой

#### Scenario: Ошибка analysis
- **WHEN** preparation завершена успешно, но analysis activity завершается исключением
- **THEN** workflow не возвращает успешный результат

### Requirement: Разделение fake и real CLI проверки
Тесты process runner SHALL явно различать проверки `FakePi` и результат локально подтверждённой совместимости с реальным Pi CLI.

#### Scenario: FakePi
- **WHEN** выполняются существующие subprocess-тесты с `FakePi`
- **THEN** они проверяют lifecycle runner, stdout/stderr, exit code, timeout и cancellation без заявления о совместимости реального Pi CLI

#### Scenario: Реальный CLI недоступен
- **WHEN** фактический executable или его контракт недоступен в окружении теста
- **THEN** результат проверки помечается непроверенным, а тесты не объявляют такую совместимость пройденной
