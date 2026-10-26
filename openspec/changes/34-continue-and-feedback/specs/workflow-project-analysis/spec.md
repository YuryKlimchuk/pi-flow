# Spec Delta

## MODIFIED Requirements

### Requirement: Запуск Analyst
Analysis activity SHALL передавать публичному API `agent` роль `ANALYST`, путь подготовленного workspace, инструкцию анализа, контекст исходного Issue и feedback пользователя, если rework был принят.

#### Scenario: Контекст передан агенту
- **WHEN** activity получает Issue с owner, repository, number и путь workspace
- **THEN** команда запуска содержит роль `ANALYST`, доступный workspace и эти идентификаторы в input/context

#### Scenario: Анализ с feedback
- **WHEN** activity получает feedback `Добавь риски миграции и уточни границы API`
- **THEN** инструкция Analyst содержит этот feedback как текст задачи, а агент получает тот же workspace и Issue-контекст

#### Scenario: Первый анализ без feedback
- **WHEN** activity запускается для нового Workflow без замечаний
- **THEN** Analyst получает инструкцию без пользовательского feedback

### Requirement: Предложение имени ветки
Каждый успешный запуск Analyst SHALL создавать новое предложение branch name, а activity SHALL валидировать его как латинское Git-имя независимо от предыдущего запуска.

#### Scenario: Валидное имя
- **WHEN** Analyst предлагает `feature/issue-42-login`
- **THEN** результат анализа содержит это имя как branch name

#### Scenario: Невалидное имя
- **WHEN** Analyst предлагает имя с пробелом, кириллицей или недопустимой последовательностью Git
- **THEN** activity отклоняет результат как невалидный и workflow не переходит к продолжению

#### Scenario: Новый branch после rework
- **WHEN** повторный Analyst предлагает `feature/issue-42-login-v2`
- **THEN** новый результат содержит это имя, заменяя прежнее предложение

#### Scenario: Невалидный новый branch
- **WHEN** повторный Analyst записывает кириллическое или иначе недопустимое имя
- **THEN** повторный анализ завершается ошибкой и старое успешное состояние не публикуется как новый результат
