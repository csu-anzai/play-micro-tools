name := "play-micro-tools"

organization := "de.21re"

version := {
  "0.8-" + sys.props.get("BUILD_NUMBER").orElse(sys.env.get("BUILD_NUMBER")).getOrElse("SNAPSHOT")
}

scalaVersion := "2.13.0"
scalaVersion in ThisBuild := "2.13.0"

crossScalaVersions := Seq("2.13.0")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:params",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:privates"
)

shellPrompt := { _ ⇒
  scala.Console.CYAN + "play-µ-tools > " + scala.Console.RESET
}

val playVersion = "2.7.3"

val metricsVersion = "4.0.3"

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
mainSourcesScalaStyle := org.scalastyle.sbt.ScalastylePlugin.autoImport.scalastyle
  .in(Compile)
  .toTask("")
  .value
(test in Test) := { (test in Test) dependsOn mainSourcesScalaStyle }.value

val macWireVersion    = "2.3.3"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play"                % playVersion % Provided,
  "com.typesafe.play" %% "play-json"                % "2.7.4" % Provided,
  "com.typesafe.play" %% "play-ws"             % playVersion % Provided,
  "com.typesafe.play" %% "filters-helpers"     % playVersion % Provided,
  "com.typesafe.play" %% "play-ahc-ws"         % playVersion % Provided,
  "com.typesafe.play" %% "play-jdbc-evolutions"         % playVersion % Provided,
  "com.chuusai"       %% "shapeless"           % "2.3.3",
  // if you change this make sure to update the actual dependencies too
  // rediscala is provided so that every user of play-micro-tools does not need to depend on it
  "redis.clients" % "jedis" % "2.9.0" % Provided,
  "io.dropwizard.metrics"    % "metrics-core"                 % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-jvm"                  % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-json"                 % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-logback"              % metricsVersion,
  "com.softwaremill.macwire" %% "macros"                      % macWireVersion % Provided,
  "com.softwaremill.macwire" %% "util"                        % macWireVersion,
  "com.softwaremill.macwire" %% "proxy"                       % macWireVersion,
  "ch.qos.logback"           % "logback-classic"              % "1.1.7",
  "org.scalatest"            %% "scalatest"                   % "3.0.8" % Test,
  "org.scalacheck"           %% "scalacheck"                  % "1.14.0" % Test,
  "org.scalatestplus.play"   %% "scalatestplus-play"          % "4.0.3" % Test,
  "org.scalamock" %% "scalamock" % "4.3.0" % Test,
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
