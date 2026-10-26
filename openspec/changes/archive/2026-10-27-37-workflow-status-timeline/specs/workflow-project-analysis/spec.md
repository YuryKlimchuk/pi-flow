# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Результат проектирования как proposal
Успешный Architect SHALL создавать в workspace непустой читаемый proposal-артефакт с обязательными разделами `Why`, `What Changes`, `Capabilities`, `Impact` и `Non-goals`, а workspace SHALL возвращать его полный Markdown через публичную операцию.

#### Scenario: Валидный Architect proposal
- **WHEN** Architect завершён успешно и файл содержит все обязательные непустые разделы
- **THEN** workspace возвращает Markdown proposal, а workflow может опубликовать его как успешный результат

#### Scenario: Невалидный Architect proposal
- **WHEN** файл Architect отсутствует, пуст или не содержит обязательный раздел
- **THEN** workspace возвращает типизированную ошибку валидации и workflow не публикует успешный proposal
