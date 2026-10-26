# Spec Delta

## MODIFIED Requirements

### Requirement: Артефакт анализа
Успешный Analyst SHALL создавать в workspace непустой читаемый артефакт анализа в предсказуемом пути, а activity SHALL возвращать путь workspace и необходимые сериализуемые метаданные; полный текст артефакта SHALL читаться из workspace отдельной публичной операцией, а не через историю Temporal.

#### Scenario: Валидный результат
- **WHEN** агент завершён успешно и `.pi-flow/analysis/analyst.md` существует, читаем и содержит непустое содержимое
- **THEN** activity возвращает путь workspace и успешные метаданные, а Workflow переходит в `WAITING_FOR_USER`

#### Scenario: Rework с предыдущим анализом
- **WHEN** Analyst запускается повторно после feedback пользователя
- **THEN** его инструкция содержит предыдущий текст анализа или путь к нему, а новый результат снова валидируется по тому же артефакту

### Requirement: Повторный запуск анализа
Analysis activity SHALL использовать один и тот же путь артефакта для повторных вызовов и SHALL NOT удалять или перезаписывать чужие незакоммиченные файлы workspace.

#### Scenario: Повтор activity
- **WHEN** Temporal повторно вызывает analysis activity для того же workflow и workspace
- **THEN** activity проверяет или безопасно обновляет собственный артефакт без удаления иных файлов workspace

#### Scenario: Повтор activity rework
- **WHEN** Analyst повторно вызывается для того же workflow и workspace
- **THEN** activity обновляет только собственный `analyst.md` и `branch.txt`, сохраняя прочие файлы workspace

### Requirement: Предложение имени ветки
Роль `ANALYST` SHALL включать в результат анализа одно предложение branch name, а analysis activity SHALL возвращать это предложение отдельно и принимать только латинское Git-имя с допустимыми символами.

#### Scenario: Валидное имя
- **WHEN** Analyst предлагает `feature/issue-42-login`
- **THEN** результат анализа содержит это имя как branch name

#### Scenario: Невалидное имя
- **WHEN** Analyst предлагает имя с пробелом, кириллицей или недопустимой последовательностью Git
- **THEN** activity отклоняет результат как невалидный и workflow не переходит к продолжению

#### Scenario: Rework branch
- **WHEN** повторный Analyst записывает новое предложение в `branch.txt`
- **THEN** activity заново валидирует имя и отклоняет кириллическое или недопустимое значение

## ADDED Requirements

### Requirement: Работа Architect
Design activity SHALL запускать роль `ARCHITECT` с Issue-контекстом и workspace, а после успешного запуска SHALL валидировать непустой читаемый артефакт проектирования в предсказуемом пути.

#### Scenario: Валидный design результат
- **WHEN** Architect завершён успешно и `.pi-flow/design/architect.md` существует, читаем и непуст
- **THEN** activity возвращает путь workspace и сериализуемые метаданные Architect

#### Scenario: Ошибка Architect
- **WHEN** Architect завершён с ошибкой, timeout или без валидного `architect.md`
- **THEN** activity завершается ошибкой и Workflow не публикует успешное проектирование

### Requirement: Контекст rework Analyst
Rework analysis activity SHALL передавать в инструкцию Analyst предыдущий результат анализа и замечания пользователя.

#### Scenario: Предыдущий результат доступен
- **WHEN** пользователь исправляет анализ с feedback
- **THEN** Analyst получает feedback и предыдущий текст либо путь `.pi-flow/analysis/analyst.md` в одном запуске
