# PostgreSQL Async - Build & Style Guide

## Build Commands
- **Test all modules**: `sbt test`
- **Test specific module**: `sbt "postgresql/test" `
- **Test single class**: `sbt "testOnly *PostgreSQLConnectionSpec"`
- **Format code**: `sbt scalafmt`
- **Compile**: `sbt compile`
- **Cross-compile**: `sbt +compile`

## Code Style
- **Formatter**: Scalafmt (v3.9.9) with 120 column limit
- **Import style**: Group imports by source, use curly braces for multi-line
- **Naming**: CamelCase for classes, PascalCase for objects, snake_case for constants
- **Error handling**: Use custom exceptions in exceptions/ packages, prefer Try/Either over throw
- **Async patterns**: Use Future for async operations, avoid blocking calls
- **Testing**: Specs2 framework with sequential execution
- **Dependencies**: Netty 4.2.2, Scala 2.12/2.13/3.3, SLF4J logging
- **Package structure**: Follow existing com.github.mauricio.async.db hierarchy

## Project Structure
- `db-async-common/`: Shared utilities and interfaces
- `postgresql-async/`: PostgreSQL-specific implementation
- `mysql-async/`: MySQL-specific implementation
- Tests mirror source package structure

## Scala Versions
- Primary: 2.13.16
- Also supports: 2.12.20, 3.3.6
- Target: Java 11+
