import sbt._

object Dependencies {

  object Codacy {
    val scalaApi = "com.codacy" %% "codacy-api-scala" % "3.1.869"
    val pluginsApi = "com.codacy" %% "codacy-plugins-api" % "3.0.276"
  }

  def scalaXml(scalaVersion: String): Seq[ModuleID] = scalaVersion match {
    case "2.10.7" => Seq.empty
    case _ => Seq("org.scala-lang.modules" %% "scala-xml" % "1.1.1")
  }

}
