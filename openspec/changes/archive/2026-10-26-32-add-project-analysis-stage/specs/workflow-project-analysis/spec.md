# Spec Delta

## Purpose

Определяет первый содержательный этап workflow: анализ Issue и подготовленного проекта агентом Analyst с сохранением проверяемого артефакта для последующих этапов.

## ADDED Requirements

### Requirement: Последовательность анализа
Temporal workflow SHALL запускать activity анализа только после успешного результата activity подготовки workspace.

#### Scenario: Успешная подготовка
- **WHEN** preparation activity возвращает существующий путь workspace и рабочую ветку
- **THEN** analysis activity запускается с этим путём и исходным контекстом Issue

#### Scenario: Ошибка подготовки
- **WHEN** preparation activity завершается ошибкой
- **THEN** analysis activity не вызывается

### Requirement: Запуск Analyst
Analysis activity SHALL передавать публичному API `agent` роль `ANALYST`, путь подготовленного workspace, инструкцию анализа и контекст исходного Issue.

#### Scenario: Контекст передан агенту
- **WHEN** activity получает Issue с owner, repository, number и путь workspace
- **THEN** команда запуска содержит роль `ANALYST`, доступный workspace и эти идентификаторы в input/context

### Requirement: Ресурсы роли Analyst
Запуск Analyst SHALL использовать описание роли из ресурсов `roles/analyst/` (системный промпт и скилы), а не вшитый в код текст; на первом этапе содержимое SHALL быть упрощённым.

#### Scenario: Роль из ресурсов
- **WHEN** analysis activity запускает Pi для роли `ANALYST`
- **THEN** командная строка передаёт системный промпт из `roles/analyst/system-prompt.md` через `--append-system-prompt` и скилы из `roles/analyst/skills/` через `--skill`

### Requirement: Артефакт анализа
Успешный Analyst SHALL создавать в workspace непустой читаемый артефакт анализа в предсказуемом пути, а activity SHALL возвращать этот путь и необходимые сериализуемые метаданные.

#### Scenario: Валидный результат
- **WHEN** агент завершён успешно и артефакт существует, читаем и содержит непустое содержимое
- **THEN** activity возвращает результат с путём артефакта и статусом успешного анализа

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
