val commonName            = "db-async-common"
val postgresqlName        = "postgresql-async"
val mysqlName             = "mysql-async"
val nettyVersion          = "4.1.97.Final"
val projectScalaVersion   = "2.13.12"
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
  "org.slf4j"        % "slf4j-api"                    % slf4jVersion,
  "io.netty"         % "netty-codec"                  % nettyVersion,
  "io.netty"         % "netty-handler"                % nettyVersion,
  "io.netty"         % "netty-transport-native-epoll" % nettyVersion classifier "linux-x86_64",
  "com.ongres.scram" % "client"                       % "2.1",
  "joda-time"        % "joda-time"                    % "2.12.2",
  "com.google.guava" % "guava"                        % "27.0.1-jre",
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
  crossScalaVersions := Seq("2.13.12", "3.3.1"),
  testOptions in Test += Tests.Argument("sequential"),
  scalaVersion := projectScalaVersion,
  scalacOptions := {
    Seq("-feature", "-deprecation", "-release:11") ++ opts(scalaVersion.value)
  },
  testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential"),
  scalacOptions in doc := Seq(
    "-doc-external-doc:scala=http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs/library/"
  ),
  javacOptions := Seq("-source", "11", "-target", "11", "-encoding", "UTF8"),
  (javaOptions in Test) ++= Seq("-Dio.netty.leakDetection.level=paranoid"),
  organization            := "com.dripower",
  parallelExecution       := false,
  publishArtifact in Test := false
)
(scalafmtOnCompile in ThisBuild) := true
(compile in Compile) := {
  (compile in Compile).dependsOn(scalafmtSbt in Compile).value
}
