import ReleaseTransformations._

val commonName            = "db-async-common"
val postgresqlName        = "postgresql-async"
val mysqlName             = "mysql-async"
val nettyVersion          = "4.2.9.Final"
val scala3Version         = "3.3.7"
val scala212Version       = "2.12.20"
val scala213Version       = "2.13.18"
val specs2Version         = "4.22.0"
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
    name := postgresqlName
  )
  .dependsOn(common)

lazy val mysql = (project in file("mysql-async"))
  .settings(baseSettings: _*)
  .settings(
    name := mysqlName
  )
  .dependsOn(common)

lazy val jmh = (project in file("jmh"))
  .enablePlugins(JmhPlugin)
  .settings(baseSettings: _*)
  .settings(
    publish         := {},
    publishLocal    := {},
    publishArtifact := false,
    name            := "jmh-benchmarks",
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(postgresql)

val commonDependencies = Seq(
  "org.slf4j"               % "slf4j-api"                    % slf4jVersion,
  "io.netty"                % "netty-codec"                  % nettyVersion,
  "io.netty"                % "netty-handler"                % nettyVersion,
  "io.netty"                % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64",
  "org.scala-lang.modules" %% "scala-collection-compat"      % "2.11.0",
  "com.ongres.scram"        % "scram-client"                 % "3.2",
  "joda-time"               % "joda-time"                    % "2.14.0",
  "com.google.guava"        % "guava"                        % "33.3.0-jre",
  specs2Dependency,
  specs2JunitDependency,
  logbackDependency
)

def opts(s: String) = {
  if (s.startsWith("2.")) {
    Seq("-Xsource:3")
  } else {
    Seq()
  }
}

inThisBuild(
  List(
    organization := "com.dripower",
    homepage     := Some(url("https://github.com/dripower/postgresql-async")),
    licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers   := List(
      Developer("jilen", "jilen", "jilen.zhang@gmail.com", url("https://github.com/jilen"))
    )
  )
)

val baseSettings = Seq(
  organization       := "com.dripower",
  parallelExecution  := false,
  crossScalaVersions := Seq(scala212Version, scala213Version, scala3Version),
  scalaVersion       := scala213Version,
  javacOptions       := Seq("-source", "11", "-target", "11", "-encoding", "UTF8"),
  scalacOptions      := {
    Seq("-feature", "-deprecation", "-release:11") ++ opts(scalaVersion.value)
  },
  (Test / testOptions) += Tests.Argument(TestFrameworks.Specs2, "sequential"),
  (Test / javaOptions) ++= Seq("-Dio.netty.leakDetection.level=paranoid"),
  (Test / publishArtifact) := false
)
(ThisBuild / scalafmtOnCompile) := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
