name := "plain-sql-insert"

version := "3.3"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ywarn-dead-code",
  "-Xlint",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick"           % "3.3.2",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "ch.qos.logback"      % "logback-classic" % "1.2.3"
)

