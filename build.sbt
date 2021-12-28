name := """CdxUploader"""

version := "1.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "org.scalikejdbc" %% "scalikejdbc" % "3.4.2",
  "org.scalikejdbc" %% "scalikejdbc-config" % "3.4.2",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "jline" % "jline" % "2.14.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test")

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"
// https://mvnrepository.com/artifact/org.jfxtras/jfxtras-controls
libraryDependencies += "org.jfxtras" % "jfxtras-controls" % "8.0-r6"

// https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
libraryDependencies += "com.microsoft.sqlserver" % "mssql-jdbc" % "9.4.1.jre8"


scalacOptions += "-feature"
scalacOptions += "-deprecation"

fork in run := true