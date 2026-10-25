# Design

## Context

`github.api.token` уже настроен как `${GITHUB_PI_FLOW_TOKEN:}` и публичные операции выбрасывают `GithubException` с типом ошибки без токена в сообщении. Web не должен читать секрет или знать инфраструктуру github.

## Goals / Non-Goals

**Goals:** безопасно отразить конфигурацию, доступность и ошибки PAT.

**Non-Goals:** хранение PAT в БД, пользовательская аутентификация и самостоятельная валидация токена.

## Decisions

### D1: Источник конфигурации
**Решение:** `GITHUB_PI_FLOW_TOKEN` или локальная конфигурация вне Git задают `github.api.token`; web только вызывает порт. **Почему:** единый источник истины уже реализован. **Альтернатива:** отдельное свойство web — отклонено как дублирование.

### D2: Отображение ошибки
**Решение:** безопасные сообщения строятся по `GithubException.ErrorType`, без `getCause()` в HTML. **Почему:** исключение гарантирует отсутствие PAT. **Альтернатива:** показывать exception message/stack trace — отклонено.

### D3: Поток диагностики
```mermaid
flowchart LR
 browser --> controller
 controller --> port[github.api port]
 port --> adapter[github adapter]
 adapter --> api[GitHub API]
 port -. GithubException.ErrorType .-> controller
 controller --> safe_ui[безопасное UI-сообщение]
```

## Risks / Trade-offs

- Нельзя отличить каждую причину без вызова github → состояние определяется при обращении к функции.
- Локальный файл конфигурации требует исключения из Git → секрет остаётся во внешнем окружении.

## Open Questions

Нет.
