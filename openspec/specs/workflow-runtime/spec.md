# workflow-runtime Specification

## Purpose
Определяет локальную конфигурацию и воспроизводимый запуск pi-flow вместе с Temporal и необходимыми сервисами.

## Requirements

### Requirement: Конфигурация Temporal
Приложение SHALL получать endpoint, namespace и task queue Temporal из конфигурации с безопасными локальными значениями по умолчанию и возможностью переопределения переменными окружения.

#### Scenario: Локальная конфигурация
- **WHEN** приложение запускается через локальный Compose без дополнительных переменных
- **THEN** Temporal-адаптер использует endpoint, namespace и task queue, совпадающие с настройками Compose

### Requirement: Контейнерный запуск
Docker Compose SHALL запускать приложение, Temporal, PostgreSQL и Temporal UI в одной сети с заданными healthcheck или эквивалентными условиями готовности для зависимостей.

#### Scenario: Запуск стека
- **WHEN** пользователь выполняет `docker compose up --build`
- **THEN** приложение получает доступ к Temporal по имени сервиса, а Temporal использует PostgreSQL как persistence backend

### Requirement: Документированный локальный запуск
Документация SHALL описывать запуск Temporal через существующий Docker Compose, адрес приложения, адрес Temporal UI, endpoint `7233`, необходимые переменные окружения и способ проверки сквозного сценария Issue → clone → execution.

#### Scenario: Новый локальный запуск
- **WHEN** разработчик выполняет шаги документации на чистом окружении с Docker Compose
- **THEN** он может запустить стек, открыть приложение и Temporal UI и остановить стек указанной командой

#### Scenario: Проверка против Docker Temporal
- **WHEN** разработчик запускает Temporal стек Docker Compose и выполняет e2e-сценарий из документации
- **THEN** приложение подключается к Temporal по настроенному endpoint, а проверка подтверждает создание execution и клонирование репозитория
