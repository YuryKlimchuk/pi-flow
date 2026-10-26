# Spec Delta

## MODIFIED Requirements

### Requirement: Состояние долгого workflow
Web SHALL различать текущий результат Analyst, следующий незапущенный этап `DESIGNING_SOLUTION` и повторный анализ после rework.

#### Scenario: Выполнение Analyst
- **WHEN** polling получает состояние `RUNNING` на этапе `ANALYZING_REQUIREMENTS`
- **THEN** UI отображает индикатор работы Analyst, а не финальный результат

#### Scenario: Успешный результат
- **WHEN** polling получает валидный результат анализа
- **THEN** UI прекращает polling, показывает текст результата и действие продолжения

#### Scenario: Ошибка
- **WHEN** polling получает статус `FAILED`
- **THEN** UI прекращает polling и показывает сообщение об ошибке без stack trace

#### Scenario: Ожидание Architect
- **WHEN** polling получает `RUNNING` на этапе `DESIGNING_SOLUTION` без результата Architect
- **THEN** UI показывает состояние ожидания реализации следующего этапа и не показывает Analyst как текущую операцию

#### Scenario: Rework выполняется
- **WHEN** polling получает `RUNNING` на этапе `ANALYZING_REQUIREMENTS` после rework
- **THEN** UI показывает индикатор нового анализа и не показывает старый текст/branch name
