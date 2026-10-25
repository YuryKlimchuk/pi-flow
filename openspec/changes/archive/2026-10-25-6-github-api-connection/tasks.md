# Tasks

## 2. Подключение к GitHub API

- [x] 2.1 Добавить HTTP-зависимость и конфигурацию `RestClient` с base URL, connect timeout и read timeout.
- [x] 2.2 Добавить typed properties токена, URL и размера страницы с внешним placeholder для токена.
- [x] 2.3 Реализовать Authorization и санитизированное маппирование ответов и ошибок.
- [x] 2.4 Добавить mock HTTP-тест наличия заголовка Authorization и отсутствия токена в DTO/логируемом сообщении.
- [x] 2.5 Добавить тест отсутствия токена: конфигурационная ошибка до отправки запроса.
- [x] 2.6 Добавить маппинг 401/403, 404, 409/422, 429, 5xx и timeout в типизированные ошибки.
- [x] 2.7 Проверить `./mvnw -q compile`, `./mvnw -q test` и `openspec validate 2026-10-25-6-github-api-connection`.
