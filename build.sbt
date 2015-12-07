name := "indyscala-di"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % "0.2.1",
  "io.circe" %% "circe-parse" % "0.2.1",
  "org.http4s" %% "http4s-blaze-client" % "0.11.2",
  "org.http4s" %% "http4s-circe" % "0.11.2",
  "org.springframework" % "spring-context" % "4.2.3.RELEASE",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.softwaremill.macwire" %% "macros" % "2.2.0" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.2.0",
  "com.softwaremill.macwire" %% "proxy" % "2.2.0"
)
