# Spec Delta

## Purpose
Определяет локальную конфигурацию и воспроизводимый запуск pi-flow вместе с Temporal и необходимыми сервисами.

## ADDED Requirements

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
Документация SHALL описывать prerequisites, команду запуска, адрес приложения, адрес Temporal UI, необходимые переменные окружения и способ остановки стека.

#### Scenario: Новый локальный запуск
- **WHEN** разработчик выполняет шаги документации на чистом окружении с Docker Compose
- **THEN** он может запустить стек, открыть приложение и Temporal UI и остановить стек указанной командой
