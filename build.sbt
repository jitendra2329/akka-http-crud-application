
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "MobileStore"
  )
resolvers += "Akka library repository".at("https://repo.akka.io/maven")

val AkkaVersion = "2.9.0"
val AkkaHttpVersion = "10.6.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
)

// https://mvnrepository.com/artifact/io.spray/spray-json
//libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.6.0"
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"       % "4.2.1",
  "com.h2database"  %  "h2"                % "2.2.224",
  "ch.qos.logback"  %  "logback-classic"   % "1.4.14"
)

// https://mvnrepository.com/artifact/org.postgresql/postgresql
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.1"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.6.0"

