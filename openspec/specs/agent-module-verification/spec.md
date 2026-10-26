# agent-module-verification Specification

## Purpose
Определяет независимую проверку контракта, subprocess и lifecycle модуля `agent`, включая большие потоки, ошибки, ресурсы и архитектурные границы.

## Requirements

### Requirement: Тест подготовки запуска
Тестовый набор SHALL проверять executable, argv, рабочую директорию и разрешённое окружение без запуска реального Pi.

#### Scenario: Проверка конфигурации
- **WHEN** тест передаёт аргумент со спецсимволами и секретное окружение
- **THEN** он подтверждает безопасный argv и отсутствие секрета в диагностике.

### Requirement: Тест успешного выполнения
Тестовый набор SHALL проверять через публичный порт успешный результат, текст и exit code.

#### Scenario: Успешный test process
- **WHEN** тестовый процесс завершается с кодом `0` и пишет stdout
- **THEN** тест получает `SUCCEEDED`, текст и `0`.

### Requirement: Тест ошибок запуска
Тестовый набор SHALL проверять отсутствие executable, ошибку создания процесса и ненулевой exit code.

#### Scenario: Неуспешный test process
- **WHEN** test executable завершается с ненулевым кодом
- **THEN** тест подтверждает `FAILED` и сохранённый код.

### Requirement: Тест больших потоков
Тестовый набор SHALL проверять одновременный объём stdout и stderr, превышающий размер pipe buffer.

#### Scenario: Потоки не блокируют
- **WHEN** test process пишет большой объём в оба потока
- **THEN** тест завершается без deadlock и проверяет содержимое обоих результатов.

### Requirement: Тест lifecycle и cleanup
Тестовый набор SHALL проверять timeout, cancellation и отсутствие активного процесса после каждого терминального сценария.

#### Scenario: Cancellation cleanup
- **WHEN** зависший test process отменяется
- **THEN** тест подтверждает `CANCELLED`, завершение процесса и закрытие ресурсов.

### Requirement: Архитектурные границы
Архитектурные тесты SHALL подтверждать, что `agent` не содержит workflow-этапов, локальных Git-операций или прямых GitHub API-вызовов, а `agent` остаётся листовым модулем.

#### Scenario: Modulith verification
- **WHEN** выполняются `ApplicationModules.verify()` и правила импортов
- **THEN** проверки завершаются успешно при направлении `workflow → agent` и отсутствии обратной зависимости.

### Requirement: Сборка и спецификация
Проект SHALL успешно завершать `./mvnw -q compile`, `./mvnw -q test`, а change SHALL проходить `openspec validate 2026-10-25-16-agent-testing`.

#### Scenario: Финальная проверка
- **WHEN** запускаются три команды проверки
- **THEN** каждая завершается с кодом 0 и OpenSpec сообщает `valid`.

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
