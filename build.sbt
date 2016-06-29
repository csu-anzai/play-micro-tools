
name := "play-error-handling"

organization := "de.21re"

version := {
  "0.1-" + sys.props.get("BUILD_NUMBER").orElse(sys.env.get("BUILD_NUMBER")).getOrElse("SNAPSHOT")
}

scalaVersion := "2.11.8"

val playVersion = "2.5.3"

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
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)
