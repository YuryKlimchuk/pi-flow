# Tasks

## 5. Завершение, отмена и ограничения выполнения

- [x] 5.1 Добавить настраиваемый timeout в параметры запуска и проверять его до старта.
- [x] 5.2 Реализовать отмену активного запуска через `process.destroy()`, ожидание и fallback `destroyForcibly()`.
- [x] 5.3 Преобразовать ненулевой exit code и неожиданное завершение в `FAILED` с сохранением технических данных.
- [x] 5.4 Реализовать переходы `RUNNING`, `SUCCEEDED`, `FAILED`, `TIMED_OUT`, `CANCELLED` без переходов из завершённых состояний.
- [x] 5.5 Проверить cleanup при timeout/cancel и отсутствие активного процесса после завершения lifecycle.
- [x] 5.6 Добавить тесты timeout/cancel/statuses и выполнить `./mvnw -q compile`, `./mvnw -q test`, `openspec validate 2026-10-25-15-agent-lifecycle`.
