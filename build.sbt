import org.scalafmt.bootstrap.ScalafmtBootstrap

name := "play-micro-tools"

organization := "de.21re"

version := {
  "0.1-" + sys.props.get("BUILD_NUMBER").orElse(sys.env.get("BUILD_NUMBER")).getOrElse("SNAPSHOT")
}

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-deprecation"
)

val playVersion = "2.5.9"

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
(test in Test) := { (test in Test) dependsOn mainSourcesScalaStyle }.value

val macWireVersion = "2.2.5"

libraryDependencies ++= Seq(
  "com.typesafe.play"        %% "play"                        % playVersion % Provided,
  "com.typesafe.play"        %% "play-ws"                     % playVersion % Provided,
  "com.chuusai"              %% "shapeless"                   % "2.3.2" % Provided,
  "com.github.etaty"         %% "rediscala"                   % "1.6.0" % Provided,
  "io.dropwizard.metrics"    % "metrics-core"                 % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-jvm"                  % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-json"                 % metricsVersion,
  "io.dropwizard.metrics"    % "metrics-logback"              % metricsVersion,
  "com.softwaremill.macwire" %% "macros"                      % macWireVersion % Provided,
  "com.softwaremill.macwire" %% "util"                        % macWireVersion,
  "com.softwaremill.macwire" %% "proxy"                       % macWireVersion,
  "ch.qos.logback"           % "logback-classic"              % "1.1.7",
  "org.scalatest"            %% "scalatest"                   % "3.0.1" % Test,
  "org.scalacheck"           %% "scalacheck"                  % "1.13.4" % Test,
  "org.scalamock"            %% "scalamock-scalatest-support" % "3.4.2" % Test,
  "org.scalatestplus.play"   %% "scalatestplus-play"          % "2.0.0-M1" % Test,
  "org.mockito"              % "mockito-core"                 % "1.10.19" % Test
)

val scalafmtTask = taskKey[Unit]("Scala fmt")

scalafmtTask := {
  org.scalafmt.bootstrap.ScalafmtBootstrap.main(Seq("--non-interactive"))
  ()
}

(compile in Compile) := { (compile in Compile) dependsOn scalafmtTask }.value
