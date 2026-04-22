# Dripower PostgreSQL/MySQL Async

Async database drivers for PostgreSQL and MySQL built on Netty.

If you are upgrading from the `0.3.x` line, see [MIGRATING_FROM_0.3_X.md](./MIGRATING_FROM_0.3_X.md).

## Modules

- [postgresql-async](./postgresql-async/README.md)
- [mysql-async](./mysql-async/README.md)
- [db-async-common](./db-async-common)

## Current Version

- Build version: `0.4.0-SNAPSHOT`
- Java target: 11+
- Scala versions: 2.12.20, 2.13.18, 3.3.7
- Netty version: 4.2.9.Final

## What Changed In 0.4.0

- driver-facing date/time handling is based on `java.time`
- PostgreSQL `interval` values decode as `String`
- PostgreSQL `interval` prepared statements accept `java.time.Period` and `java.time.Duration`
- migration guidance from `0.3.x` is documented in [MIGRATING_FROM_0.3_X.md](./MIGRATING_FROM_0.3_X.md)

For a concise release-oriented summary, see [CHANGELOG.md](./CHANGELOG.md).
