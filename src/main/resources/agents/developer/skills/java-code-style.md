---
name: java-code-style
description: Java code style conventions. Use utility methods instead of manual null checks and conditionals.
---
Use `Objects.requireNonNull`, `StringUtils.isBlank`, `CollectionUtils.isEmpty`, and other Apache Commons / JDK utility methods instead of manual null checks and conditionals. Prefer utility classes over raw expressions.
