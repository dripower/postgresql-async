val commonName            = "db-async-common"
val postgresqlName        = "postgresql-async"
val mysqlName             = "mysql-async"
val nettyVersion          = "4.1.114.Final"
val scala3Version         = "3.3.4"
val scala212Version       = "2.12.20"
val scala213Version       = "2.13.15"
val specs2Version         = "4.19.2"
val slf4jVersion          = "2.0.7"
val specs2Dependency      = "org.specs2"    %% "specs2-core"     % specs2Version % "test"
val specs2JunitDependency = "org.specs2"    %% "specs2-junit"    % specs2Version % "test"
val logbackDependency     = "ch.qos.logback" % "logback-classic" % "1.1.8"       % "test"

lazy val root = (project in file("."))
  .settings(baseSettings: _*)
  .settings(
    name            := "db-async-base",
    publish         := {},
    publishLocal    := {},
    publishArtifact := false
  )
  .aggregate(common, postgresql, mysql)

lazy val common = (project in file("db-async-common"))
  .settings(baseSettings: _*)
  .settings(
    name := commonName,
    libraryDependencies ++= commonDependencies
  )

lazy val postgresql = (project in file("postgresql-async"))
  .settings(baseSettings: _*)
  .settings(
    name := postgresqlName,
    libraryDependencies ++= implementationDependencies
  )
  .dependsOn(common)

lazy val mysql = (project in file("mysql-async"))
  .settings(baseSettings: _*)
  .settings(
    name := mysqlName,
    libraryDependencies ++= implementationDependencies
  )
  .dependsOn(common)

val commonDependencies = Seq(
  "org.slf4j"               % "slf4j-api"                    % slf4jVersion,
  "io.netty"                % "netty-codec"                  % nettyVersion,
  "io.netty"                % "netty-handler"                % nettyVersion,
  "io.netty"                % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64",
  "org.scala-lang.modules" %% "scala-collection-compat"      % "2.11.0",
  "com.ongres.scram"        % "client"                       % "2.1",
  "joda-time"               % "joda-time"                    % "2.12.2",
  "com.google.guava"        % "guava"                        % "33.3.0-jre",
  specs2Dependency,
  specs2JunitDependency,
  logbackDependency
)

val implementationDependencies = Seq(
  specs2Dependency,
  logbackDependency
)

def opts(s: String) = {
  if (s.startsWith("2.")) {
    Seq("-Ydelambdafy:method")
  } else {
    Seq()
  }
}

val baseSettings = Seq(
  crossScalaVersions := Seq(scala212Version, scala213Version, scala3Version),
  testOptions in Test += Tests.Argument("sequential"),
  scalaVersion := scala213Version,
  scalacOptions := {
    Seq("-feature", "-deprecation", "-release:11") ++ opts(scalaVersion.value)
  },
  (Test / testOptions) += Tests.Argument(TestFrameworks.Specs2, "sequential"),
  (doc / scalacOptions) := Seq(
    "-doc-external-doc:scala=http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs/library/"
  ),
  javacOptions := Seq("-source", "11", "-target", "11", "-encoding", "UTF8"),
  (Test / javaOptions) ++= Seq("-Dio.netty.leakDetection.level=paranoid"),
  organization            := "com.dripower",
  parallelExecution       := false,
  publishArtifact in Test := false
)
(ThisBuild / scalafmtOnCompile) := true
