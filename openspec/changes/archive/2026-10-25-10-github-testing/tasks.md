# Tasks

## 6. Тестирование и завершение реализации

- [x] 6.1 Добавить contract-тесты публичных портов для успешных операций, валидации и типизированных ошибок без реального GitHub.
- [x] 6.2 Добавить интеграционные тесты адаптера с локальным mock HTTP server для Authorization, пагинации, Issue/PR distinction и idempotency.
- [x] 6.3 Добавить/обновить проверки `web → workflow → {workspace, agent, interaction, github}`, отсутствия обратных и циклических зависимостей.
- [x] 6.4 Проверить `./mvnw -q compile` после подключения всех компонентов.
- [x] 6.5 Запустить `./mvnw -q test` и исправить ошибки до успешного завершения.
- [x] 6.6 Запустить `openspec validate 2026-10-25-5-github-module-contract`.
- [x] 6.7 Запустить `openspec validate 2026-10-25-6-github-api-connection`.
- [x] 6.8 Запустить `openspec validate 2026-10-25-7-github-repositories`.
- [x] 6.9 Запустить `openspec validate 2026-10-25-8-github-issues`.
- [x] 6.10 Запустить `openspec validate 2026-10-25-9-github-pull-request`.
- [x] 6.11 Запустить `openspec validate 2026-10-25-10-github-testing` и убедиться, что результат содержит `valid`.
