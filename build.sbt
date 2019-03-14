import codacy.libs._

name := "coverage-parser"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.7")

scalacOptions := Seq("-deprecation", "-feature", "-unchecked", "-Ywarn-adapted-args", "-Xlint", "-Xfatal-warnings")

libraryDependencies ++= Seq(Dependencies.Codacy.scalaApi, Dependencies.Codacy.pluginsApi, scalatest) ++ Dependencies
  .scalaXml(scalaVersion.value)

mappings in (Compile, packageBin) ~= {
  _.filterNot {
    case (file, _) => file.getName == "logback-test.xml"
  }
}

resolvers ~= { _.filterNot(_.name.toLowerCase.contains("codacy")) }

// this setting is not picked up properly from the plugin
pgpPassphrase := Option(System.getenv("SONATYPE_GPG_PASSPHRASE")).map(_.toCharArray)

publicMvnPublish

startYear := Some(2015)

description := "Library for parsing coverage reports"

homepage := Some(url("http://www.github.com/codacy/coverage-parser/"))

scmInfo := Some(
  ScmInfo(url("https://github.com/codacy/coverage-parser"), "scm:git:git@github.com:codacy/coverage-parser.git")
)

fork in Test := true
cancelable in Global := true
