val commonName     = "db-async-common"
val postgresqlName = "postgresql-async"
val mysqlName      = "mysql-async"

lazy val root = Project(
  id = "db-async-base",
  base = file(".")
).settings(
  baseSettings ++ Seq(
    publish         := (),
    publishLocal    := (),
    publishArtifact := false
  )
).aggregate(common, postgresql, mysql)

lazy val common = Project(
  id = commonName,
  base = file(commonName)
).settings(baseSettings)
  .settings(
    name := commonName,
    libraryDependencies ++= commonDependencies
  )

lazy val postgresql = Project(
  id = postgresqlName,
  base = file(postgresqlName)
).settings(baseSettings)
  .settings(
    name := postgresqlName,
    libraryDependencies ++= implementationDependencies
  )
  .dependsOn(common)

lazy val mysql = Project(
  id = mysqlName,
  base = file(mysqlName)
).settings(baseSettings)
  .settings(
    name := mysqlName,
    libraryDependencies ++= implementationDependencies
  )
  .dependsOn(common)

val nettyVersion  = "4.0.56.Final"
val commonVersion = "0.2.2027"
val scala212      = "2.12.18"
val scala211      = "2.11.12"
val specs2Version = "3.8.6"

val specs2Dependency = "org.specs2" %% "specs2-core" % specs2Version % "test"
val specs2JunitDependency =
  "org.specs2" %% "specs2-junit" % specs2Version % "test"
val specs2MockDependency =
  "org.specs2" %% "specs2-mock" % specs2Version % "test"
val logbackDependency =
  "ch.qos.logback" % "logback-classic" % "1.1.8" % "test"

val commonDependencies = Seq(
  "org.slf4j" % "slf4j-api"     % "1.7.22",
  "joda-time" % "joda-time"     % "2.9.7",
  "org.joda"  % "joda-convert"  % "1.8.1",
  "io.netty"  % "netty-codec"   % nettyVersion,
  "io.netty"  % "netty-handler" % nettyVersion,
  "io.netty" % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64",
  "org.javassist"            % "javassist" % "3.21.0-GA",
  "com.google.guava"         % "guava"     % "19.0",
  "com.google.code.findbugs" % "jsr305"    % "3.0.1" % "provided",
  specs2Dependency,
  specs2JunitDependency,
  specs2MockDependency,
  logbackDependency
)

val implementationDependencies = Seq(
  specs2Dependency,
  logbackDependency
)

val baseSettings = Seq(
  crossScalaVersions := Seq(scala212, scala211),
  testOptions in Test += Tests.Argument("sequential"),
  scalaVersion := scala212,
  scalacOptions :=
    Opts.compile.encoding("UTF8")
      :+ Opts.compile.deprecation
      :+ Opts.compile.unchecked
      :+ "-feature"
      :+ "-Ydelambdafy:method",
  testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential"),
  scalacOptions in doc := Seq(
    "-doc-external-doc:scala=http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs/library/"
  ),
  javacOptions      := Seq("-source", "8", "-target", "8", "-encoding", "UTF8"),
  organization      := "com.dripower",
  version           := commonVersion,
  parallelExecution := false,
  publishArtifact in Test := false
)
