import codacy.libs._

name := "coverage-parser"

scalaVersion := "2.12.10"

scalacOptions := Seq("-deprecation", "-feature", "-unchecked", "-Ywarn-adapted-args", "-Xlint", "-Xfatal-warnings")

// Runtime dependencies
libraryDependencies ++= Seq(Dependencies.Codacy.scalaApi, Dependencies.Codacy.pluginsApi) ++ Dependencies
  .scalaXml(scalaVersion.value)

// Test dependencies
libraryDependencies ++= Seq(scalatest).map(_ % "test")

// HACK: Since we are only using the public resolvers we need to remove the private for it to not fail
resolvers ~= { _.filterNot(_.name.toLowerCase.contains("codacy")) }

// HACK: This setting is not picked up properly from the plugin
pgpPassphrase := Option(System.getenv("SONATYPE_GPG_PASSPHRASE")).map(_.toCharArray)

description := "Library for parsing coverage reports"

scmInfo := Some(
  ScmInfo(url("https://github.com/codacy/coverage-parser"), "scm:git:git@github.com:codacy/coverage-parser.git")
)

publicMvnPublish

fork in Test := true
cancelable in Global := true
