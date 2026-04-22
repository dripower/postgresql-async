# Version 0.4.0

`0.4.0` is the next unreleased version of this repository. The build currently publishes it as
`0.4.0-SNAPSHOT`.

This file intentionally replaces the old historical changelog. The older `0.2.x` notes were no longer useful for this
branch; this document focuses on what matters for the upcoming `0.4.0` line.

## Release Status

- Status: unreleased
- Current build version: `0.4.0-SNAPSHOT`
- Upgrade guide from `0.3.x`: [MIGRATING_FROM_0.3_X.md](./MIGRATING_FROM_0.3_X.md)

## What Matters In 0.4.0

The main purpose of `0.4.0` is to modernize date/time handling across both drivers and document the migration from the
`0.3.x` line.

Highlights:

- Joda-Time has been removed from the build in favor of Java 8+ `java.time`
- PostgreSQL date/time values now decode to `java.time` types
- PostgreSQL `interval` values now decode to raw PostgreSQL text as `String`
- PostgreSQL prepared statements now accept `java.time.Period` and `java.time.Duration` for `interval`
- MySQL date/time support is documented around `java.time`-based usage
- Project docs now include explicit migration guidance for users coming from `0.3.x`

## Behavioral Changes From 0.3.x

### PostgreSQL

Decoded values now map as follows:

- `timestamp` -> `java.time.LocalDateTime`
- `timestamp_with_timezone` -> `java.time.OffsetDateTime`
- `date` -> `java.time.LocalDate`
- `time` -> `java.time.LocalTime`
- `time_with_timezone` -> `java.time.OffsetTime`
- `interval` -> `String`

Prepared statement parameters now additionally support:

- `java.time.LocalDate`
- `java.time.LocalDateTime`
- `java.time.OffsetDateTime`
- `java.time.Instant`
- `java.time.LocalTime`
- `java.time.OffsetTime`
- `java.time.Period`
- `java.time.Duration`

Migration impact:

- Code that cast PostgreSQL results to Joda types must move to `java.time`
- Code that read PostgreSQL `interval` as `org.joda.time.Period` must now handle `String`
- Code that wrote PostgreSQL `interval` using Joda duration/period types must move to `java.time.Period` or
  `java.time.Duration`

### MySQL

Current documented mappings in this line are:

- `date` -> `java.time.LocalDate`
- `datetime` -> `java.time.LocalDateTime`
- `timestamp` -> `java.time.LocalDateTime`
- `time` -> `java.time.Duration`

Prepared statement parameters support `java.time.LocalDate`, `java.time.LocalDateTime`,
`java.time.OffsetDateTime`, `java.time.Instant`, and `java.time.Duration` for date/time use cases.

Migration impact:

- Code using Joda types for MySQL date/time values should move to `java.time`
- Code that expected MySQL `time` to decode as `scala.concurrent.Duration` must move to `java.time.Duration`

## Compatibility Matrix

Current build settings for this branch:

- Java target: 11+
- Scala: 2.12.20, 2.13.18, 3.3.7
- Netty: 4.2.9.Final
- SLF4J: 2.0.7
- Specs2: 4.22.0

Modules:

- `db-async-common`
- `postgresql-async`
- `mysql-async`

## Upgrade Checklist

Before adopting `0.4.0`, verify:

- application code does not depend on Joda-Time types returned by the drivers
- PostgreSQL `interval` reads are updated from typed period objects to `String`
- PostgreSQL `interval` writes use `java.time.Period` or `java.time.Duration`
- timezone-aware comparisons are done by instant, not by expecting original textual offsets to round-trip unchanged

## Related Docs

- Repository overview: [README.markdown](./README.markdown)
- Migration notes: [MIGRATING_FROM_0.3_X.md](./MIGRATING_FROM_0.3_X.md)
- PostgreSQL driver notes: [postgresql-async/README.md](./postgresql-async/README.md)
- MySQL driver notes: [mysql-async/README.md](./mysql-async/README.md)
