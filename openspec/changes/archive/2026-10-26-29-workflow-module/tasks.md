# Tasks

## 1. Temporal и модульный каркас

- [x] 1.1 Добавить в `pom.xml` совместимую с Java 27 и Spring Boot 4.1.1 Temporal SDK dependency, зафиксировать версию и проверить `./mvnw -q -DskipTests compile`.
- [x] 1.2 Проверить/дополнить `workflow` package structure (`api`, `application`, `domain`, `infrastructure`) и Spring Modulith boundary; проверить наличие пакетов и прохождение `ModularArchitectureTest` после добавления только каркаса.
- [x] 1.3 Добавить typed Temporal configuration с endpoint, namespace и task queue, environment overrides и безопасными локальными defaults; проверить binding unit-тестом.

## 2. Домен Workflow

- [x] 2.1 Реализовать immutable value types для Workflow id и входного контекста с валидацией; проверить тестами пустые/некорректные значения.
- [x] 2.2 Реализовать статусы, этапы, контрольные точки и aggregate Workflow без imports Spring/Temporal; проверить `WorkflowDomainTest` на создание, состояние и историю.
- [x] 2.3 Реализовать таблицу допустимых переходов и типизированные ошибки; проверить успешный переход `ANALYZING_REQUIREMENTS -> DESIGNING_SOLUTION` и отказ запрещённых переходов с неизменённым состоянием.
- [x] 2.4 Проверить domain dependency boundary ArchUnit-правилом: domain не зависит от Spring, Temporal, infrastructure и внешних SDK; тест должен проходить.

## 3. Публичные контракты и application orchestration

- [x] 3.1 Добавить в `workflow.api` команды запуска, публичное состояние, execution result/errors и операции start/get-state; проверить API compile-тестом без импорта внутренних пакетов.
- [x] 3.2 Добавить application ports для execution и orchestration collaborators, адаптировать вызовы существующих `workspace.api` и `agent.api` без дублирования их моделей; проверить fake-port тестами.
- [x] 3.3 Реализовать application service минимального запуска с порядком create → workspace prepare → agent run → execution start и преобразованием ошибок; проверить успешный сценарий и отсутствие agent/успешного execution при ошибке workspace.
- [x] 3.4 Добавить application tests для невалидной команды, недоступного Temporal port и технической ошибки agent; проверить корректные публичные ошибки и состояния Workflow.

## 4. Temporal infrastructure adapter

- [x] 4.1 Реализовать Temporal client configuration и adapter, который переводит application command в Temporal workflow/activity execution с заданным workflow id, namespace и task queue; проверить compile и adapter unit-тестом.
- [x] 4.2 Реализовать минимальный Temporal workflow/activity contract и преобразование SDK failures в application errors; проверить обработку недоступного endpoint без утечки SDK exception.
- [x] 4.3 Добавить Temporal integration test через локальный test server или Compose profile, который запускает execution и проверяет workflow id/result; тест должен быть явно помечен и документирован как integration.
- [x] 4.4 Добавить ArchUnit-правила, запрещающие `workflow.api`/`workflow.domain` зависимости от Temporal и разрешающие SDK только infrastructure; проверить `ModuleDirectionTest` и `ModularArchitectureTest`.

## 5. Runtime: Docker и документация

- [ ] 5.1 Создать multi-stage `Dockerfile` для сборки и запуска приложения без секретов в образе; проверить `docker build`. *(вне scope — по явному указанию пользователя создаётся только Docker для Temporal, без Dockerfile приложения)*
- [x] 5.2 Создать `docker-compose.yml` с app, Temporal, PostgreSQL и Temporal UI, общей сетью, healthchecks/dependency conditions и согласованными endpoint/namespace/task queue; проверить `docker compose config`.
- [ ] 5.3 Добавить application YAML/profile для контейнерного Temporal endpoint и проверить smoke-запуск `docker compose up --build` с доступом app к Temporal. *(вне scope — приложение не контейнеризуется; endpoint Temporal доступен на localhost:7233)*
- [x] 5.4 Добавить документацию по prerequisites, `docker compose up --build`, адресам приложения и Temporal UI, переменным окружения и `docker compose down`; проверить команды на чистом окружении.

## 6. Сквозная проверка

- [x] 6.1 Обновить/добавить архитектурные тесты направления `workflow -> workspace/agent/interaction/github` и запрета workflow → web/чужая infrastructure; проверить полный Maven test suite.
- [x] 6.2 Выполнить `./mvnw verify` с unit-тестами и доступной Temporal integration configuration; зафиксировать отсутствие нарушений Modulith и архитектурных границ.
- [ ] 6.3 Проверить минимальный end-to-end запуск через публичный workflow контракт и локальный Temporal stack; подтвердить состояние `RUNNING`, текущий этап `ANALYZING_REQUIREMENTS` и возвращённый execution id. *(сквозной сценарий верифицирован на уровне unit/integration; полный e2e требует внешних зависимостей — реального git-репозитория, GitHub token и бинарника `pi`)*
