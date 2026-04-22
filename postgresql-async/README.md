<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [postgresql-async](#postgresql-async)
  - [What can it do now?](#what-can-it-do-now)
  - [Supported Scala/Java types and their destination types on PostgreSQL](#supported-scalajava-types-and-their-destination-types-on-postgresql)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# postgresql-async

The main goal of this project is to implement a performant and fully functional async PostgreSQL driver. This project
has no interest in JDBC, it's supposed to be a clean room implementation for people interested in talking directly
to PostgreSQL.

[PostgreSQL protocol information and definition can be found here](http://www.postgresql.org/docs/devel/static/protocol.html)

If you are upgrading from the `0.3.x` line, see [../MIGRATING_FROM_0.3_X.md](../MIGRATING_FROM_0.3_X.md) for the
cross-driver migration notes.

This driver contains Java code from the [JDBC PostgreSQL](http://jdbc.postgresql.org/) driver under the
`com.github.mauricio.async.db.postgresql.util` package consisting of the `ParseURL` class.

## What can it do now?

- connect to a database with or without authentication (supports cleartext, MD5, and SCRAM authentication)
- receive database parameters
- receive database notices
- execute direct queries (without portals/prepared statements)
- portals/prepared statements
- parses most of the basic PostgreSQL types, other types are parsed as string
- date, time and timestamp types are handled with the Java 8+ `java.time` API instead of `java.util.Date`
- query timeout configuration is supported through `Configuration.queryTimeout`
- configurable Netty `eventLoopGroup` and `channelClass`
- all work is done using `scala.concurrent.Future` and `scala.concurrent.Promise`
- support for Byte arrays if using PostgreSQL >= 9.0
- support for LISTEN/NOTIFY operations (see [ListenNotifySpec](/home/jilen/Workspaces/drip-postgresql-async/postgresql-async/src/test/scala/com/github/mauricio/async/db/postgresql/ListenNotifySpec.scala))

## Supported Scala/Java types and their destination types on PostgreSQL

All types also support their array versions, but they are returned as `IndexedSeq` of the type and not
pure `Array` types.

PostgreSQL type | Scala/Java type
--- | --- | ---
boolean | Boolean
smallint | Short
integer (or serial) | Int
bigint (or bigserial) | Long
numeric | BigDecimal
real | Float
double | Double
text | String
varchar | String
bpchar | String
timestamp | java.time.LocalDateTime
timestamp_with_timezone | java.time.OffsetDateTime
date | java.time.LocalDate
time | java.time.LocalTime
time_with_timezone | java.time.OffsetTime
interval | String
bytea | Array[Byte] (PostgreSQL 9.0 and above only)

All other types are returned as String.

Now from Scala/Java types to PostgreSQL types (when using prepared
statements):

Scala/Java type | PostgreSQL type
--- | --- | ---
Boolean | boolean
Short | smallint
Int | integer
Long | bigint
Float | float
Double | double
BigInteger | numeric
BigDecimal | numeric
String | varchar
Array[Byte] | bytea (PostgreSQL 9.0 and above only)
java.nio.ByteBuffer | bytea (PostgreSQL 9.0 and above only)
io.netty.buffer.ByteBuf | bytea (PostgreSQL 9.0 and above only)
java.util.Date | timestamp_with_timezone
java.sql.Timestamp | timestamp_with_timezone
java.sql.Date | date
java.sql.Time | time
java.time.LocalDate | date
java.time.LocalDateTime | timestamp
java.time.OffsetDateTime | timestamp_with_timezone
java.time.Instant | timestamp_with_timezone
java.time.LocalTime | time
java.time.OffsetTime | time_with_timezone
java.time.Period | interval
java.time.Duration | interval

Array types are encoded with the kind of object they hold and not the array type itself. Java `Collection` and
Scala `Traversable` objects are also assumed to be arrays of the types they hold and will be sent to PostgreSQL
like that.
