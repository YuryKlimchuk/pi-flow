# Tasks

## 1. Создание модулей

- [x] 1.1 Создать пакеты модулей `workflow`, `workspace`, `agent`, `interaction`, `github`, `web` под `com.hydroyura.agents.piflow` и проверить создание.
- [x] 1.2 Создать `package-info.java` для каждого модуля с `@ApplicationModule` и проверить компиляцию `./mvnw -q compile`.

## 2. Пустые DDD-слои

- [x] 2.1 Для модулей бизнес-логики (`workflow`, `workspace`, `agent`, `interaction`, `github`) создать пустые подпакеты `api`, `domain`, `application`, `infrastructure`.
- [x] 2.2 Для `web` не создавать DDD-слои (оставить как адаптер) и проверить, что структура соответствует design.

## 3. Архитектурный тест

- [x] 3.1 Добавить тест Spring Modulith `ApplicationModules.of(...).verify()` для проверки отсутствия циклов.
- [x] 3.2 Проверить, что `./mvnw -q test` проходит.

## 4. Валидация

- [x] 4.1 Проверить `openspec validate "3-modular-architecture-skeleton"` возвращает `valid`.
