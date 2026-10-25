# Tasks

## 3. Получение репозиториев

- [x] 3.1 Реализовать вызов endpoint доступных репозиториев и маппинг owner/name/id/URL в `RepositorySummary`.
- [x] 3.2 Реализовать `page`/`perPage` и обработку Link header с заполнением `hasNext`.
- [x] 3.3 Добавить валидацию page/perPage до HTTP-вызова и тест отсутствия запроса.
- [x] 3.4 Добавить mock HTTP-тесты первой страницы, Link header и отсутствия следующей страницы.
- [x] 3.5 Добавить тесты маппинга 401/403, 429, 5xx и timeout с операцией, статусом/причиной и retryable.
- [x] 3.6 Проверить `./mvnw -q compile`, `./mvnw -q test` и `openspec validate 2026-10-25-7-github-repositories`.
