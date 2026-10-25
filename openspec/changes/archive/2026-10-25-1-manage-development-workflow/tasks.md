# Tasks

## 1. Проектирование диаграммы workflow

- [x] 1.1 Построить Mermaid-диаграмму последовательности этапов (`PREPARING_REPOSITORY` → `ANALYZING_REQUIREMENTS` → `DESIGNING_SOLUTION` → `IMPLEMENTING` → `REVIEWING` → `FINALIZING`) и проверить, что порядок соответствует `specs/development-workflow/spec.md`.
- [x] 1.2 Отобразить общие состояния (`RUNNING`, `WAITING_FOR_USER`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED`) и переходы между ними на диаграмме.
- [x] 1.3 Отобразить контрольные точки пользователя, циклы исправлений (`DESIGNING_SOLUTION`, `REVIEWING` с лимитом 3) и обработку ошибок/отмены/возобновления.
- [x] 1.4 Отобразить управление блокировкой репозитория (получение, удержание, освобождение в `COMPLETED`/`FAILED`/`CANCELLED`).

## 2. Оформление итогового файла диаграммы

- [x] 2.1 Сохранить диаграмму отдельным файлом в корне каталога спецификаций `openspec/specs/` и проверить, что Mermaid-блок синтаксически корректен.
- [x] 2.2 Сверить диаграмму с `design.md` (Decisions D1–D10) и `specs/development-workflow/spec.md` на полноту и отсутствие противоречий.
- [x] 2.3 Проверить `openspec validate "1-manage-development-workflow"` возвращает `valid`.
