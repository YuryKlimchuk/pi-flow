# Tasks

## 1. Lock lifecycle

- [x] 1.1 Определить canonical repository lock key и формат `.pi-flow-lock`; проверить одинаковый key для одинакового репозитория.
- [x] 1.2 Реализовать атомарное получение lease с owner/token/expiry; проверить конкурентным тестом только одного победителя.
- [x] 1.3 Реализовать busy-ошибку с owner и expiry; проверить отсутствие Git-вызова при занятом lock.
- [x] 1.4 Реализовать release по owner token после успеха и в `finally` после исключения; проверить оба сценария.
- [x] 1.5 Реализовать повтор владельца, истёкший lease и отсутствие автоматического удаления директории; проверить recovery-тестами.
- [x] 1.6 Интегрировать lock с preparation/branches и проверить `./mvnw -q compile`.

## 2. Проверка

- [x] 2.1 Выполнить `./mvnw -q test` для конкуренции, lease и cleanup-политики.
- [x] 2.2 Выполнить `openspec validate 2026-10-25-21-workspace-locks`.
