# Spec Delta

## Purpose

Предоставляет пользовательский вход для запуска workflow по конкретной GitHub Issue и связывает данные формы с публичным контрактом workflow без использования командной оболочки.

> **Scope**: это **частичный (не полный e2e) сценарий**. Он покрывает выбор Issue и запуск workflow до клонирования репозитория включительно. Запуск Pi/агента и дальнейшие стадии процесса не входят в этот change.

## ADDED Requirements

### Requirement: Запуск workflow по Issue
Web SHALL принимать owner, repository и положительный issue number и передавать их в публичную операцию запуска workflow.

#### Scenario: Валидная форма запуска
- **WHEN** пользователь отправляет owner `acme`, repository `demo` и issue number `42`
- **THEN** web вызывает публичный запуск workflow с контекстом репозитория `acme/demo` и Issue `42` и отображает идентификатор созданного workflow или его состояние

### Requirement: Валидация формы запуска
Web SHALL отклонять пустые owner/repository и issue number меньше 1 без вызова workflow.

#### Scenario: Некорректная Issue
- **WHEN** пользователь отправляет пустой owner или issue number `0`
- **THEN** web отображает ошибку валидации и не создаёт workflow

### Requirement: Отсутствие Shell-входа
Запуск workflow по Issue SHALL быть доступен через web UI и SHALL NOT требовать Spring Shell-команды.

#### Scenario: Запуск без Shell
- **WHEN** приложение запущено с доступным web UI, но без выполнения Shell-команды
- **THEN** пользователь может открыть форму, отправить Issue и получить результат запуска через web
