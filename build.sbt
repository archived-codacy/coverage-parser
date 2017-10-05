import Dependencies._

name := "coverage-parser"

version := "1.1.1"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.3")

scalacOptions := Seq("-deprecation", "-feature", "-unchecked", "-Ywarn-adapted-args", "-Xlint", "-Xfatal-warnings")

resolvers ++= Seq(
  DefaultMavenRepository,
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  Classpaths.typesafeReleases,
  Classpaths.sbtPluginReleases
)

libraryDependencies ++= Seq(
  codacyScalaApi,
  scalaTest
)

mappings in(Compile, packageBin) ~= {
  _.filterNot {
    case (file, _) => file.getName == "logback-test.xml"
  }
}

organization := "com.codacy"

organizationName := "Codacy"

organizationHomepage := Some(new URL("https://www.codacy.com"))

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

startYear := Some(2015)

description := "Library for parsing coverage reports"

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://www.github.com/codacy/coverage-parser/"))

pomExtra :=
  <scm>
    <url>https://github.com/codacy/coverage-parser</url>
    <connection>scm:git:git@github.com:codacy/coverage-parser.git</connection>
    <developerConnection>scm:git:https://github.com/codacy/coverage-parser.git</developerConnection>
  </scm>
    <developers>
      <developer>
        <id>mrfyda</id>
        <name>Rafael</name>
        <email>rafael [at] codacy.com</email>
        <url>https://github.com/mrfyda</url>
      </developer>
    </developers>
