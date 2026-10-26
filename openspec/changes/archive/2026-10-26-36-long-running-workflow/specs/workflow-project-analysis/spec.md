# Spec Delta

## MODIFIED Requirements

### Requirement: Последовательность анализа
Единое Temporal workflow SHALL запускать activity анализа только после успешной подготовки workspace и SHALL сохранять результат как текущий этап до сигнала пользователя.

#### Scenario: Успешная подготовка
- **WHEN** preparation activity возвращает существующий путь workspace и рабочую ветку
- **THEN** Analyst activity запускается с этим путём, а после валидного результата workflow ожидает сигнал в том же execution

#### Scenario: Ошибка подготовки
- **WHEN** preparation activity завершается ошибкой
- **THEN** Analyst activity не вызывается и workflow переходит в `FAILED`

### Requirement: Артефакт анализа
Успешный Analyst SHALL создавать непустые читаемые `.pi-flow/analysis/analyst.md` и `.pi-flow/analysis/branch.txt`, SHALL возвращать путь workspace и сериализуемые метаданные, а полный текст SHALL читаться из workspace, не из Temporal history.

#### Scenario: Валидный результат
- **WHEN** оба ожидаемых файла существуют, читаемы и содержат валидные данные
- **THEN** query показывает результат Analyst и `WAITING_FOR_USER`, а Temporal payload не содержит полного текста артефактов

### Requirement: Повторный запуск анализа
Analysis activity SHALL использовать тот же workspace и собственные пути артефактов при `rework` и SHALL NOT удалять чужие незакоммиченные файлы workspace.

#### Scenario: Повтор activity
- **WHEN** пользователь отправляет `rework` с feedback на Analyst gate
- **THEN** Analyst запускается повторно в том же execution, обновляет только свои артефакты и заменяет текущие метаданные результата
