# Spec Delta

## ADDED Requirements

### Requirement: Состояние long-running execution
Application-сценарий SHALL получать актуальное состояние long-running workflow по стабильному workflow id и SHALL не использовать завершение отдельного stage execution как источник истины.

#### Scenario: Query состояния
- **WHEN** web запрашивает `GET /workflows/track` для активного workflow
- **THEN** application получает query-состояние того же Temporal execution и возвращает текущий stage, status и доступные actions

#### Scenario: Ошибка query
- **WHEN** Temporal не отвечает на query
- **THEN** application возвращает типизированную ошибку получения состояния и не подменяет его локальным успешным результатом
