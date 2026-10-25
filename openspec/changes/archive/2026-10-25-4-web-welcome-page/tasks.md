# Tasks

## 1. Подключение зависимостей

- [x] 1.1 Добавить `spring-boot-starter-web` в `pom.xml` и проверить компиляцию `./mvnw -q compile`.
- [x] 1.2 Добавить `spring-boot-starter-thymeleaf` в `pom.xml` и проверить компиляцию.

## 2. Контроллер и шаблон

- [x] 2.1 Создать `WelcomeController` в модуле `web`, возвращающий шаблон `index` по `GET /`.
- [x] 2.2 Создать Thymeleaf-шаблон `index.html` в каталоге шаблонов.

## 3. Точка входа

- [x] 3.1 Сделать `main` публичным и убрать `System.exit`, чтобы сервер оставался запущенным.

## 4. Проверка

- [x] 4.1 Убедиться, что `./mvnw -q test` проходит (включая архитектурный тест).
- [x] 4.2 Проверить `openspec validate "4-web-welcome-page"` возвращает `valid`.
