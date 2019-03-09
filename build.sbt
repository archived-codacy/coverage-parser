name := "coverage-parser"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.7")

scalacOptions := Seq("-deprecation", "-feature", "-unchecked", "-Ywarn-adapted-args", "-Xlint", "-Xfatal-warnings")

resolvers ++= Seq("Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases/")

libraryDependencies ++= Seq(Dependencies.Codacy.scalaApi, Dependencies.Codacy.pluginsApi, Dependencies.scalaTest) ++ Dependencies
  .scalaXml(scalaVersion.value)

mappings in (Compile, packageBin) ~= {
  _.filterNot {
    case (file, _) => file.getName == "logback-test.xml"
  }
}

publicMvnPublish

organization := "com.codacy"

organizationName := "Codacy"

organizationHomepage := Some(new URL("https://www.codacy.com"))

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
