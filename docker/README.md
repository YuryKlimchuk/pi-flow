# Локальный запуск Temporal (Docker Compose)

Этот каталог содержит Docker Compose для локального запуска Temporal Server вместе
с его зависимостями. Сам `pi-flow` собирается и запускается отдельно (через Maven),
Temporal используется как внешний сервис.

## Сервисы

| Сервис | Образ | Порт |
|---|---|---|
| `postgresql` | persistence backend Temporal | `5432` |
| `temporal` | `temporalio/auto-setup` (frontend gRPC) | `7233` |
| `temporal-ui` | Temporal Web UI | `8233` |

## Prerequisites

- Docker Engine + Docker Compose v2 (`docker compose`).

## Запуск

```bash
docker compose up -d
```

После старта:

- Temporal gRPC endpoint: `localhost:7233`
- Temporal Web UI: http://localhost:8233

## Переменные окружения

Версии образов и учётные данные БД задаются в `.env`:

| Переменная | Значение по умолчанию |
|---|---|
| `TEMPORAL_VERSION` | `1.29.1` |
| `TEMPORAL_UI_VERSION` | `2.57.0` |
| `POSTGRESQL_VERSION` | `13` |
| `POSTGRES_USER` | `temporal` |
| `POSTGRES_PASSWORD` | `temporal` |

Конфигурация подключения приложения `pi-flow` к Temporal (endpoint, namespace,
task queue) задаётся в `src/main/resources/application.yaml` через свойства
`workflow.temporal.*` и может переопределяться переменными окружения
`WORKFLOW_TEMPORAL_ENDPOINT`, `WORKFLOW_TEMPORAL_NAMESPACE`,
`WORKFLOW_TEMPORAL_TASK_QUEUE`.

## Остановка

```bash
docker compose down       # остановить контейнеры, сохранив данные
docker compose down -v    # остановить и удалить volume с данными PostgreSQL
```

## Проверка сквозного сценария (до клонирования репозитория)

> **Важно:** это **частичный (не полный e2e)** сценарий: от выбора Issue через
> web-интерфейс до клонирования репозитория и старта Temporal execution
> включительно. Запуск Pi-агента и дальнейшие стадии не входят.

1. Поднять Temporal:
   ```bash
   docker compose up -d
   ```

2. Запустить приложение:
   ```bash
   ./mvnw spring-boot:run
   ```
   Приложение доступно на http://localhost:8080.

3. В браузере открыть репозитории и Issues. Для выбранной задачи нажать
   «Взять в работу» — это отправит форму запуска workflow.

4. Ожидаемый результат: страница показывает созданный `workflowId` и
   `executionId` Temporal execution.

5. Проверить, что репозиторий склонирован в рабочую ветку `pi-flow/<workflowId>`:
   ```bash
   ls ~/.pi-flow/workspaces/
   ```
   В каталоге должна появиться локальная копия репозитория с рабочей веткой.

6. Проверить execution в Temporal UI: http://localhost:8233 (namespace `default`).

7. Остановить стек: `docker compose down`.
