# Tasks

## 1. Состояния и ошибки
- [x] 1.1 Добавить loading/success/empty/error представления для репозиториев и Issues; проверить MVC-сценарии.
- [x] 1.2 Реализовать единый mapper `GithubException.ErrorType` → безопасное русское сообщение; проверить configuration/auth/not-found/rate-limit/network/server.
- [x] 1.3 Добавить retry для retryable-ошибок с сохранением исходных параметров; проверить повторный вызов fake-порта.

## 2. Контекст и адаптивность
- [x] 2.1 Проверить сохранение owner/repository/page/perPage во всех retry и pagination-ссылках.
- [x] 2.2 Обновить layout/CSS для viewport 320 px и проверить отсутствие горизонтального overflow.
- [x] 2.3 Выполнить `./mvnw -q compile` и `./mvnw -q test`.

## 3. Проверка
- [x] 3.1 Выполнить `openspec validate 2026-10-25-27-web-ui-states`.
