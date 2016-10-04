
name := "play-micro-tools"

organization := "de.21re"

version := {
  "0.1-" + sys.props.get("BUILD_NUMBER").orElse(sys.env.get("BUILD_NUMBER")).getOrElse("SNAPSHOT")
}

scalaVersion := "2.11.8"

val playVersion = "2.5.3"

val metricsVersion = "3.1.2"

licenses +=("MIT", url("http://opensource.org/licenses/MIT"))

resolvers += "21re-bintray" at "http://dl.bintray.com/21re/public"

publishMavenStyle := true

bintrayOrganization := Some("21re")

bintrayRepository := "public"

bintrayCredentialsFile := {
  sys.props.get("BINTRAY_CREDENTIALS").orElse(sys.env.get("BINTRAY_CREDENTIALS")).map(new File(_)).getOrElse(baseDirectory.value / ".bintray" / "credentials")
}

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % playVersion % Provided,
  "com.typesafe.play" %% "play-ws" % playVersion % Provided,
  "com.github.etaty" %% "rediscala" % "1.6.0" % Provided,
  "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
  "io.dropwizard.metrics" % "metrics-jvm" % metricsVersion,
  "io.dropwizard.metrics" % "metrics-json" % metricsVersion,
  "io.dropwizard.metrics" % "metrics-logback" % metricsVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-core" % "1.10.19" % Test
)
