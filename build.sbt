val commonName            = "db-async-common"
val postgresqlName        = "postgresql-async"
val mysqlName             = "mysql-async"
val nettyVersion          = "4.2.15.Final"
val scala3Version         = "3.3.8"
val scala212Version       = "2.12.20"
val scala213Version       = "2.13.18"
val specs2Version         = "4.22.0"
val slf4jVersion          = "2.0.7"
val specs2Dependency      = "org.specs2"    %% "specs2-core"     % specs2Version % "test"
val logbackDependency     = "ch.qos.logback" % "logback-classic" % "1.1.8"       % "test"

lazy val root = (project in file("."))
  .settings(
    name                     := "db-async-base",
    publish / skip           := true,
    publishArtifact          := false,
    Test / testFrameworks    := Seq(TestFrameworks.Specs2)
  )
  .aggregate(common, postgresql, mysql)

lazy val common = (project in file("db-async-common"))
  .settings(
    name                     := commonName,
    Test / testFrameworks    := Seq(TestFrameworks.Specs2)
  )

lazy val postgresql = (project in file("postgresql-async"))
  .settings(
    name                     := postgresqlName,
    Test / testFrameworks    := Seq(TestFrameworks.Specs2)
  )
  .dependsOn(common)

lazy val mysql = (project in file("mysql-async"))
  .settings(
    name                     := mysqlName,
    Test / testFrameworks    := Seq(TestFrameworks.Specs2)
  )
  .dependsOn(common)

val commonDependencies = Seq(
  "org.slf4j"               % "slf4j-api"                    % slf4jVersion,
  "io.netty"                % "netty-codec"                  % nettyVersion,
  "io.netty"                % "netty-handler"                % nettyVersion,
  ("io.netty"               % "netty-transport-native-epoll" % nettyVersion).classifier("linux-x86_64"),
  "org.scala-lang.modules" %% "scala-collection-compat"      % "2.11.0",
  "com.ongres.scram"        % "scram-client"                 % "3.2",
  "com.google.guava"        % "guava"                        % "33.3.0-jre",
  specs2Dependency,
  logbackDependency
)

def opts(s: String) = {
  if (s.startsWith("2.")) {
    Seq("-Xsource:3")
  } else {
    Seq()
  }
}

organization := "com.dripower"
homepage     := Some(uri("https://github.com/dripower/postgresql-async"))
licenses     := List(License("Apache-2.0", uri("http://www.apache.org/licenses/LICENSE-2.0")))
developers   := List(
  Developer("jilen", "jilen", "jilen.zhang@gmail.com", uri("https://github.com/jilen"))
)

Test / parallelExecution  := false
crossScalaVersions := Seq(scala212Version, scala213Version, scala3Version)
scalaVersion       := scala213Version
javacOptions       := Seq("-source", "11", "-target", "11", "-encoding", "UTF8")
scalacOptions      := Seq("-feature", "-deprecation", "-release:11") ++ opts(scalaVersion.value)

(Test / testOptions) += Tests.Argument(TestFrameworks.Specs2, "sequential")
(Test / javaOptions) ++= Seq("-Dio.netty.leakDetection.level=paranoid")


libraryDependencies ++= commonDependencies
