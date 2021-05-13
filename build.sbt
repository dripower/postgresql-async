val commonName            = "db-async-common"
val postgresqlName        = "postgresql-async"
val mysqlName             = "mysql-async"
val nettyVersion          = "4.1.63.Final"
val commonVersion         = "0.3.111"
val projectScalaVersion   = "2.12.13"
val specs2Version         = "4.5.1"
val specs2Dependency      = "org.specs2"    %% "specs2-core"     % specs2Version % "test"
val specs2JunitDependency = "org.specs2"    %% "specs2-junit"    % specs2Version % "test"
val specs2MockDependency  = "org.specs2"    %% "specs2-mock"     % specs2Version % "test"
val logbackDependency     = "ch.qos.logback" % "logback-classic" % "1.1.8"       % "test"

lazy val root = (project in file("."))
  .settings(baseSettings: _*)
  .settings(
    name := "db-async-base",
    publish := {},
    publishLocal := {},
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
  "org.slf4j"                % "slf4j-api"                    % "1.7.22",
  "joda-time"                % "joda-time"                    % "2.9.7",
  "org.joda"                 % "joda-convert"                 % "1.8.1",
  "io.netty"                 % "netty-codec"                  % nettyVersion,
  "io.netty"                 % "netty-handler"                % nettyVersion,
  "io.netty"                 % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64",
  "org.javassist"            % "javassist"                    % "3.21.0-GA",
  "com.google.guava"         % "guava"                        % "27.0.1-jre",
  "com.google.code.findbugs" % "jsr305"                       % "3.0.1" % "provided",
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
  crossScalaVersions := Seq("2.12.13", "2.13.5"),
  testOptions in Test += Tests.Argument("sequential"),
  scalaVersion := "2.13.0",
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
  javacOptions := Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF8"),
  (javaOptions in Test) ++= Seq("-Dio.netty.leakDetection.level=paranoid"),
  organization := "com.dripower",
  version := commonVersion,
  parallelExecution := false,
  publishArtifact in Test := false
)
(scalafmtOnCompile in ThisBuild) := true
(compile in Compile) := {
  (compile in Compile).dependsOn(scalafmtSbt in Compile).value
}
