# Development Workflow

Диаграмма сквозного процесса разработки ПО от подготовки репозитория до создания Pull Request.

## 1. Последовательность этапов

```mermaid
flowchart TD
    START([Запуск workflow]) --> PREP[PREPARING_REPOSITORY<br/>Проверка репозитория<br/>Получение блокировки<br/>Подготовка ветки]

    PREP -->|репозиторий готов| ANALYZE[ANALYZING_REQUIREMENTS<br/>Роль: Analyst<br/>Сформировать требования<br/>Подтверждение пользователя]

    ANALYZE -->|требования утверждены| DESIGN[DESIGNING_SOLUTION<br/>Роль: Architect<br/>Подготовить OpenSpec Proposal<br/>Проверка пользователем]

    DESIGN -->|утверждено| IMPL[IMPLEMENTING<br/>Роль: Developer<br/>Реализация, сборка, тесты]

    IMPL -->|сборка и тесты пройдены| REVIEW[REVIEWING<br/>Роль: Reviewer<br/>Независимое ревью]

    REVIEW -->|приемлемо| FINAL[FINALIZING<br/>Отчёт, публикация ветки<br/>Создание PR, уведомление<br/>Освобождение блокировки]

    FINAL --> END([COMPLETED])

    %% Циклы исправлений (лимит 3)
    DESIGN -.->|замечания, до 3 итераций| DESIGN
    REVIEW -.->|замечания, до 3 итераций| IMPL
    IMPL -.->|ошибки сборки/тестов| IMPL
    ANALYZE -.->|не хватает данных| ANALYZE

    %% Контрольные точки пользователя
    ANALYZE -.->|запрос сведений| USER_AN{{WAITING_FOR_USER<br/>контрольная точка}}
    DESIGN -.->|проверка результата| USER_DS{{WAITING_FOR_USER<br/>контрольная точка}}
    USER_AN -->|ответ получен| ANALYZE
    USER_DS -->|решение получено| DESIGN

    %% Ошибки
    PREP -->|репозиторий занят| FAILED[FAILED]
    DESIGN -->|лимит итераций превышен| FAILED
    REVIEW -->|лимит итераций превышен| FAILED
    FINAL -->|ошибка уведомления/освобождения| FAILED
    FAILED --> END_FAIL([FAILED])
```

## 2. Жизненный цикл состояний

```mermaid
stateDiagram-v2
    [*] --> RUNNING

    RUNNING --> WAITING_FOR_USER: требуется ответ/подтверждение
    WAITING_FOR_USER --> RUNNING: ответ получен

    RUNNING --> PAUSED: приостановка
    PAUSED --> RUNNING: возобновление

    RUNNING --> COMPLETED: PR создан
    RUNNING --> FAILED: фатальная ошибка
    RUNNING --> CANCELLED: отмена

    WAITING_FOR_USER --> CANCELLED: отмена
    PAUSED --> CANCELLED: отмена

    COMPLETED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]

    note right of WAITING_FOR_USER
      Блокировка репозитория удерживается
    end note

    note right of PAUSED
      Блокировка репозитория удерживается
    end note

    note right of COMPLETED
      Блокировка освобождается
    end note

    note right of FAILED
      Блокировка освобождается (best-effort)
    end note

    note right of CANCELLED
      Блокировка освобождается
    end note
```

## 3. Управление блокировкой репозитория

```mermaid
sequenceDiagram
    participant W as Workflow
    participant R as Repo Lock

    W->>R: Получить блокировку (PREPARING_REPOSITORY)
    R-->>W: Блокировка получена (или отказ, если занята)

    Note over W,R: Блокировка удерживается весь процесс

    alt COMPLETED / FAILED / CANCELLED
        W->>R: Освободить блокировку
        R-->>W: Освобождена
    else WAITING_FOR_USER / PAUSED
        Note over W,R: Блокировка продолжает удерживаться
    end
```

## 4. Классификация ошибок

| Тип | Примеры | Результат |
|---|---|---|
| Повторяемая | ошибки сборки/тестов, замечания ревью | повторная итерация (лимит 3 в `DESIGNING_SOLUTION` и `REVIEWING`) |
| Фатальная | недоступность Git/GitHub, потеря блокировки, нарушение целостности | `FAILED` |
