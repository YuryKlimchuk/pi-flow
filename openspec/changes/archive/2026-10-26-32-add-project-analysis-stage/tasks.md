# Tasks

## 1. Исследование и контракты

- [x] 1.1 Зафиксировать подтверждённый CLI-контракт установленного Pi (v1.4.0): `--print` для одноразового запуска, `--append-system-prompt <file>` для роли/инструкции, `@path`/позиционные сообщения для контекста, ненулевой exit code при ошибке/abort/обрезке; отсутствие флагов `--role`/`--run-id`.
- [x] 1.2 Определить и реализовать сериализуемые модели входа/результата analysis activity, включая Issue context, workspace path, artifact path и технические метаданные; проверить компиляцией и сериализацией в тесте.
- [x] 1.3 Зафиксировать детерминированный путь артефакта в workspace и инструкции Analyst так, чтобы генерировался первичный анализ, а не OpenSpec Proposal; проверить контракт тестом на ожидаемый путь и обязательные секции.
- [x] 1.4 Создать роль-ресурсы Analyst `src/main/resources/roles/analyst/system-prompt.md` и `roles/analyst/skills/` с упрощёнными описаниями (краткий промпт роли и один скил с форматом артефакта анализа); убедиться, что пути резолвятся из classpath.

## 2. Analysis activity

- [x] 2.1 Реализовать activity, которая вызывает только публичный `AgentOperations` с ролью `ANALYST`, Issue context и подготовленным workspace; проверить mock-тестом параметры команды и отсутствие прямого process запуска.
- [x] 2.2 Добавить валидацию AgentRunResult и артефакта: status, exit code, существование, читаемость и непустое содержимое; проверить успешный, FAILED, TIMED_OUT, launch exception, отсутствующий и пустой результат unit-тестами.
- [x] 2.3 Обеспечить повторяемость activity для одного workflow и защиту чужих файлов workspace; проверить повторным вызовом, сохранением постороннего файла и безопасной обработкой собственного артефакта.
- [x] 2.4 Зарегистрировать analysis activity в существующем Temporal worker/DI wiring без импорта Temporal в `agent`; проверить `ModularArchitectureTest`/правила направлений зависимостей.

## 3. Temporal orchestration

- [x] 3.1 Изменить Temporal workflow на последовательность preparation activity → analysis activity и вернуть только путь/метаданные валидного анализа; проверить успешным workflow integration-тестом.
- [x] 3.2 Передать в analysis activity полный исходный Issue context и путь результата preparation без Git-операций и запуска Pi в workflow implementation; проверить тестом captured activity input и архитектурным анализом.
- [x] 3.3 Расширить Temporal integration-тесты сценариями ошибки preparation, ошибки Analyst и невалидного артефакта; проверить, что Analyst не вызывается после preparation failure и ни один failure не возвращается как success.
- [x] 3.4 Сохранить существующее поведение точки входа GitHub Issue и preparation activity; запустить существующие workflow/workspace tests и проверить сохранение ветки `pi-flow/<workflowId>`.

## 4. Agent runner compatibility

- [x] 4.1 Перевести `ProcessAgentRunner` на подтверждённый CLI-контракт: `--print`, `--append-system-prompt <file>` (из `roles/analyst/system-prompt.md`), `--skill <dir>` (из `roles/analyst/skills/`), контекст через `@path`/позиционные сообщения; убрать `--role`/`--run-id`/`--key=value` и stdin-payload.
- [x] 4.2 Сохранить порт `AgentProcessRunner`, рабочую директорию, таймаут и exit-code семантику; передавать описание роли через ресурсные файлы, а не вшитый текст.
- [x] 4.3 Обновить `FakePi` под реальные аргументы и переписать process-runner тесты: подтверждённый CLI-контракт и fake-process lifecycle раздельно; проверить запуском целевого набора Maven tests.

## 5. Финальная проверка и отчёт

- [x] 5.1 Запустить `./mvnw -q test` и зафиксировать фактический exit code и результаты, включая новые unit/Temporal tests.
- [x] 5.2 Запустить `openspec validate` для change и зафиксировать фактический результат проверки спецификации.
- [x] 5.3 Подготовить итоговый отчёт со списком изменённых файлов, описанием реализации, фактически запущенными тестами, статусом real Pi CLI compatibility, нерешёнными вопросами и рекомендуемыми следующими шагами; проверить соответствие scope и non-goals.
