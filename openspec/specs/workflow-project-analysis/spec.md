# workflow-project-analysis Specification

## Purpose
Определяет первый содержательный этап workflow: анализ Issue и подготовленного проекта агентом Analyst с сохранением проверяемого артефакта для последующих этапов.

## Requirements

### Requirement: Последовательность анализа
Temporal workflow SHALL запускать activity анализа только после успешного результата activity подготовки workspace.

#### Scenario: Успешная подготовка
- **WHEN** preparation activity возвращает существующий путь workspace и рабочую ветку
- **THEN** analysis activity запускается с этим путём и исходным контекстом Issue

#### Scenario: Ошибка подготовки
- **WHEN** preparation activity завершается ошибкой
- **THEN** analysis activity не вызывается

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

### Requirement: Ресурсы роли Analyst
Запуск Analyst SHALL использовать описание роли из ресурсов `roles/analyst/` (системный промпт и скилы), а не вшитый в код текст; на первом этапе содержимое SHALL быть упрощённым.

#### Scenario: Роль из ресурсов
- **WHEN** analysis activity запускает Pi для роли `ANALYST`
- **THEN** командная строка передаёт системный промпт из `roles/analyst/system-prompt.md` через `--append-system-prompt` и скилы из `roles/analyst/skills/` через `--skill`

### Requirement: Артефакт анализа
Успешный Analyst SHALL создавать в workspace непустой читаемый артефакт анализа в предсказуемом пути, а activity SHALL возвращать путь workspace и необходимые сериализуемые метаданные; полный текст артефакта SHALL читаться из workspace отдельной публичной операцией, а не через историю Temporal.

#### Scenario: Валидный результат
- **WHEN** агент завершён успешно и артефакт существует, читаем и содержит непустое содержимое
- **THEN** activity возвращает результат с путём workspace и статусом успешного анализа, а текст артефакта доступен через публичный контракт workspace

### Requirement: Валидация результата
Analysis activity SHALL отклонять запуск, если агент не запущен, завершён с ошибкой или timeout, либо артефакт отсутствует, пуст или недоступен для чтения.

#### Scenario: Ненулевой exit code
- **WHEN** AgentRunResult имеет статус FAILED или ненулевой exit code
- **THEN** activity завершается ошибкой и workflow не возвращает успешный результат

#### Scenario: Отсутствующий артефакт
- **WHEN** агент сообщил успешное завершение, но ожидаемый файл отсутствует или пуст
- **THEN** activity завершается ошибкой валидации результата

### Requirement: Повторный запуск анализа
Analysis activity SHALL использовать один и тот же путь артефакта для повторных вызовов и SHALL NOT удалять или перезаписывать чужие незакоммиченные файлы workspace.

#### Scenario: Повтор activity
- **WHEN** Temporal повторно вызывает analysis activity для того же workflow и workspace
- **THEN** activity проверяет или безопасно обновляет собственный артефакт без удаления иных файлов workspace

### Requirement: Предложение имени ветки
Каждый успешный запуск Analyst SHALL создавать новое предложение branch name, а activity SHALL валидировать его как латинское Git-имя независимо от предыдущего запуска. Завершающая операция SHALL использовать последнее утверждённое предложение как имя итоговой ветки.

#### Scenario: Валидное имя
- **WHEN** Analyst предлагает `feature/issue-42-login`
- **THEN** результат анализа содержит это имя как branch name

#### Scenario: Использование имени при завершении
- **WHEN** пользователь утверждает proposal Architect после анализа с `feature/issue-42-login`
- **THEN** создаваемая итоговая ветка называется `feature/issue-42-login`

#### Scenario: Невалидное имя
- **WHEN** Analyst предлагает имя с пробелом, кириллицей или недопустимой последовательностью Git
- **THEN** activity отклоняет результат как невалидный и workflow не переходит к продолжению

#### Scenario: Новый branch после rework
- **WHEN** повторный Analyst предлагает `feature/issue-42-login-v2`
- **THEN** новый результат содержит это имя, заменяя прежнее предложение

#### Scenario: Невалидный новый branch
- **WHEN** повторный Analyst записывает кириллическое или иначе недопустимое имя
- **THEN** повторный анализ завершается ошибкой и старое успешное состояние не публикуется как новый результат

### Requirement: Результат проектирования как proposal
Успешный Architect SHALL создавать в workspace непустой читаемый proposal-артефакт с обязательными разделами `Why`, `What Changes`, `Capabilities`, `Impact` и `Non-goals`, а workspace SHALL возвращать его полный Markdown через публичную операцию.

#### Scenario: Валидный Architect proposal
- **WHEN** Architect завершён успешно и файл содержит все обязательные непустые разделы
- **THEN** workspace возвращает Markdown proposal, а workflow может опубликовать его как успешный результат

#### Scenario: Невалидный Architect proposal
- **WHEN** файл Architect отсутствует, пуст или не содержит обязательный раздел
- **THEN** workspace возвращает типизированную ошибку валидации и workflow не публикует успешный proposal
