name := "play-micro-tools"

organization := "de.21re"

version := {
  "0.6-" + sys.props.get("BUILD_NUMBER").orElse(sys.env.get("BUILD_NUMBER")).getOrElse("SNAPSHOT")
}

scalaVersion := "2.12.3"

crossScalaVersions := Seq("2.12.3", "2.11.11")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

scalacOptions ++= {
  if (scalaVersion.value >= "2.12")
    Seq(
      "-Ywarn-unused:imports",
      "-Ywarn-unused:params",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:privates"
    )
  else Seq()
}

shellPrompt := { _ ⇒
  scala.Console.CYAN + "play-µ-tools > " + scala.Console.RESET
}

val playVersion = "2.6.2"

val metricsVersion = "3.1.2"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

resolvers += "21re-bintray" at "http://dl.bintray.com/21re/public"

publishMavenStyle := true

bintrayOrganization := Some("21re")

bintrayRepository := "public"

bintrayCredentialsFile := {
  sys.props
    .get("BINTRAY_CREDENTIALS")
    .orElse(sys.env.get("BINTRAY_CREDENTIALS"))
    .map(new File(_))
    .getOrElse(baseDirectory.value / ".bintray" / "credentials")
}

lazy val mainSourcesScalaStyle = taskKey[Unit]("mainSourcesScalaStyle")
mainSourcesScalaStyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle
  .in(Compile)
  .toTask("")
  .value
(test in Test) := {
  (test in Test) dependsOn mainSourcesScalaStyle
}.value

val macWireVersion = "2.2.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play"        % playVersion % Provided,
  "com.typesafe.play" %% "play-ws"     % playVersion % Provided,
  "com.typesafe.play" %% "play-ahc-ws" % playVersion % Provided,
  "com.chuusai"       %% "shapeless"   % "2.3.2",
  // if you change this make sure to update the actual dependencies too
  // rediscala is provided so that every user of play-micro-tools does not need to depend on it
  "com.github.etaty"         %% "rediscala"                   % "1.8.0" % Provided,
  "io.dropwizard.metrics"    % "metrics-core"                 % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-jvm"                  % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-json"                 % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-logback"              % metricsVersion,
  "com.softwaremill.macwire" %% "macros"                      % macWireVersion % Provided,
  "com.softwaremill.macwire" %% "util"                        % macWireVersion,
  "com.softwaremill.macwire" %% "proxy"                       % macWireVersion,
  "ch.qos.logback"           % "logback-classic"              % "1.1.7",
  "org.scalatest"            %% "scalatest"                   % "3.0.3" % Test,
  "org.scalacheck"           %% "scalacheck"                  % "1.13.5" % Test,
  "org.scalamock"            %% "scalamock-scalatest-support" % "3.6.0" % Test,
  "org.scalatestplus.play"   %% "scalatestplus-play"          % "3.1.1" % Test,
  "org.mockito"              % "mockito-core"                 % "1.10.19" % Test
)

enablePlugins(ScalafmtPlugin)
scalafmtVersion := "0.6.8"
(compile in Compile) := {
  (compile in Compile) dependsOn (scalafmt in Compile).toTask
}.value
(compile in Test) := {
  (compile in Test) dependsOn (scalafmt in Test).toTask
}.value
