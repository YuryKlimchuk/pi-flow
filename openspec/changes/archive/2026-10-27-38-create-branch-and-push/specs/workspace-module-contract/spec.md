# Spec Delta

## MODIFIED Requirements

### Requirement: Порт локального Git
Внутренний domain-порт локального Git SHALL отделять application-логику от адаптера, а публичные модели SHALL NOT зависеть от Git API. Публичный workspace-контракт SHALL принимать команды создания итоговой ветки и коммита артефактов.

#### Scenario: Архитектурная проверка
- **WHEN** анализируются зависимости `workspace.api`
- **THEN** они не содержат типов JGit, CLI, `Process` или `workspace.infrastructure`

#### Scenario: Вызов через публичный API
- **WHEN** workflow передаёт команду создания ветки или коммита
- **THEN** workspace принимает её через `workspace.api` и возвращает сериализуемый результат либо типизированную ошибку
