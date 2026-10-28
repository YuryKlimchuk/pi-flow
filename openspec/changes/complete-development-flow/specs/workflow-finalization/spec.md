# Spec Delta

## Purpose

Завершает утверждённое решение публикацией рабочей ветки и созданием Pull Request с согласованным терминальным состоянием workflow.

## ADDED Requirements

### Requirement: Temporal финализация
Единый Temporal execution SHALL выполнять создание итоговой ветки, коммит, push и создание Pull Request как последовательные действия финализации.

#### Scenario: Успешная финализация
- **WHEN** Reviewer подтверждён и workspace доступен
- **THEN** execution создаёт или выбирает итоговую ветку, фиксирует изменения, публикует ветку и создаёт Pull Request

### Requirement: Pull Request
Финализация SHALL вызывать публичный контракт `github` для создания Pull Request и SHALL сохранять номер и URL PR в публичном состоянии workflow.

#### Scenario: PR создан
- **WHEN** опубликованная ветка доступна GitHub и запрос создания PR принят
- **THEN** состояние содержит непустые идентификаторы PR и URL, а workflow становится `COMPLETED`

### Requirement: Ошибка финализации
Ошибка создания ветки, коммита, push или PR SHALL переводить workflow в `FAILED` и SHALL предотвращать объявление PR созданным.

#### Scenario: Ошибка PR
- **WHEN** GitHub отклоняет создание Pull Request после успешного push
- **THEN** workflow становится `FAILED`, состояние не содержит успешного результата PR, а lock освобождается

### Requirement: Завершение execution и lock
После `COMPLETED`, `FAILED` или `CANCELLED` workflow SHALL завершать Temporal execution и SHALL освобождать workspace lock.

#### Scenario: Терминальное завершение
- **WHEN** финализация завершается успешно либо workflow получает фатальную ошибку или отмену
- **THEN** Temporal execution имеет терминальный статус, доменное состояние имеет тот же терминальный результат, и lock больше не удерживается

### Requirement: Повторяемые и фатальные ошибки
Workflow SHALL повторять только классифицированные повторяемые ошибки в пределах настроенного лимита и SHALL переводить фатальные ошибки в `FAILED` без перехода к следующему этапу.

#### Scenario: Фатальная ошибка интеграции
- **WHEN** GitHub недоступен или обнаружена потеря целостности workspace
- **THEN** workflow становится `FAILED`, следующий activity не запускается, а безопасная причина сохраняется в состоянии
