# Dripower's internal Postgres/MySQL Async

If you are upgrading from the `0.3.x` line, see [MIGRATING_FROM_0.3_X.md](./MIGRATING_FROM_0.3_X.md).

## Diffs

+ Guava cache based `PrepareStatement` automatically close
+ Buffered `ServerColumnDefinitionMessage`
+ [Netty](https://netty.io) 4.0.x version with native transport enabled for linux platform by default
+ Configurable netty `eventLoopGroup` and `channelClass`

## Roadmap

+ Client side preparestatement
+ More momery effcient server side preparedstatement cache
