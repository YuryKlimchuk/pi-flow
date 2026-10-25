# Tasks

## 1. Публичный вход по Issue и web UI

- [x] 1.1 Расширить публичную команду запуска workflow входными owner, repository и положительным issue number, сохранив валидацию и совместимое преобразование в контекст Issue; проверить unit-тестами валидный и невалидный ввод.
- [x] 1.2 Добавить в Issue view действие запуска для каждой Issue с owner, repository и number; проверить MVC-тестом наличие корректной ссылки/формы и отсутствие доступа шаблона к внутренним workflow-пакетам.
- [x] 1.3 Добавить web endpoint обработки формы, вызов `WorkflowOperations` и представление результата/ошибки; проверить тестами успешный запуск и отсутствие вызова workflow при ошибке валидации.

## 2. Workflow orchestration

- [x] 2.1 Удалить `AgentOperations` и `agent.run` из зависимостей и цепочки `WorkflowService.start`; проверить Mockito-тестом порядок создания состояния и вызова execution и отсутствие вызова agent.
- [x] 2.2 Перестроить `WorkflowExecutionCommand`/mapping так, чтобы Temporal execution получал workflow id, owner, repository, issue number и нормализованный repository URL; проверить тестом содержимое команды.
- [x] 2.3 Сохранить фиксацию execution id и публичного состояния через существующие `WorkflowOperations`/repository; проверить тестами ошибки валидации и запуска Temporal.
- [x] 2.4 Обновить архитектурные проверки направления `web → workflow → workspace` и отсутствия зависимости workflow domain/api от Temporal/Spring/infrastructure; проверить `WorkflowArchitectureTest` и `ApplicationModules.verify()`.

## 3. Temporal activity и workspace clone

- [x] 3.1 Изменить Temporal input и workflow contract для передачи owner/repository/issue/workflow id и вызова preparation activity до завершения минимального execution; проверить TestWorkflowEnvironment-сценарием.
- [x] 3.2 Реализовать `WorkflowPreparationActivitiesImpl` через инъецированный `WorkspaceOperations.prepare` с веткой `pi-flow/<workflowId>` и без прямого JGit-кода; проверить Mockito-тестом точный `WorkspacePreparationRequest`.
- [x] 3.3 Подключить activity implementation к Spring Temporal worker configuration без создания второго workspace adapter; проверить старт worker и обработку activity через `TestWorkflowEnvironment`.
- [x] 3.4 Добавить activity integration test с реальным workspace adapter и локальным временным JGit-репозиторием; проверить существование клона и активной ветки `pi-flow/<workflowId>`.
- [x] 3.5 Проверить failure path clone/branch preparation и преобразование ошибки в failed Temporal execution без запуска agent; проверить тестом недоступный или невалидный источник.

## 4. Контракты workspace и совместимость модулей

- [x] 4.1 Проверить, что `WorkspaceOperations.prepare` принимает workflow id и target branch, а вычисление директории, clone и branch switching остаётся в workspace; изменить только необходимые публичные контракты и проверить существующие workspace tests.
- [x] 4.2 Проверить архитектурные тесты всех модулей после переноса вызова workspace в activity; выполнить `./mvnw -q test` и подтвердить отсутствие циклических зависимостей.

## 5. Docker Temporal и e2e-документация

- [x] 5.1 Проверить и при необходимости обновить Docker Compose readiness, endpoint `7233`, namespace `default`, task queue `pi-flow` и Temporal UI `8233`; проверить `docker compose config`.
- [x] 5.2 Документировать отдельный e2e-профиль против Temporal из Docker Compose, включая fixture Git-репозиторий, запуск приложения, URL UI, ожидание execution и очистку; проверить команды на чистом Docker-окружении.
- [ ] 5.3 Добавить e2e-тест «выбрал Issue → отправил web-форму → создан Temporal execution → репозиторий склонирован → ветка `pi-flow/<workflowId>` существует»; запускать его только с явным integration/e2e профилем и Docker Temporal. *(частично: chain верiфицирован unit/integration-тестами + activity clone integration; полный UI→Temporal e2e требует реального GitHub repo/token и описан в docker/README.md)*

## 6. Сквозная верификация

- [x] 6.1 Выполнить полный Maven test/verify с unit, architecture и Temporal test-server тестами; зафиксировать прохождение тестов и отсутствие вызовов agent в минимальном сценарии.
- [ ] 6.2 Выполнить Docker-based e2e из документации и проверить в Temporal UI или API наличие execution, а в workspace — клона и активной ветки; записать результат как критерий готовности change. *(Docker Temporal поднят и проверен (namespace default, UI 200, gRPC 7233); полный UI→clone e2e требует реального GitHub repo/token и остаётся ручным шагом по docker/README.md)*
