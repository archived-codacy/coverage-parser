package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport, Language}
import com.codacy.parsers.XMLCoverageParser

import scala.xml.Node

class CoberturaParser(val language: Language.Value, val rootProject: File, val coverageReport: File) extends XMLCoverageParser {

  val rootProjectDir = rootProject.getAbsolutePath + File.separator

  override def isValidReport: Boolean = {
    (xml \\ "coverage").nonEmpty
  }

  def generateReport(): CoverageReport = {
    val total = (xml \\ "coverage" \ "@line-rate").headOption.map {
      total =>
        (total.text.toFloat * 100).toInt
    }.getOrElse(0)

    val files = (xml \\ "class" \\ "@filename").map(_.text).toSet

    val filesCoverage = files.map {
      file =>
        lineCoverage(file)
    }.toSeq

    CoverageReport(language, total, filesCoverage)
  }

  private def lineCoverage(sourceFilename: String): CoverageFileReport = {
    val file = (xml \\ "class").filter {
      n: Node =>
        (n \\ "@filename").text == sourceFilename
    }

    val classHit = (file \\ "@line-rate").map {
      total =>
        (total.text.toFloat * 100).toInt
    }

    val fileHit = classHit.sum / classHit.length

    val lineHitMap = file.map {
      n =>
        (n \\ "line").map {
          line =>
            (line \ "@number").text.toInt -> (line \ "@hits").text.toInt
        }
    }.flatten.toMap.collect {
      case (key, value) if value > 0 =>
        key -> value
    }

    CoverageFileReport(sanitiseFilename(sourceFilename), fileHit, lineHitMap)
  }

  private def sanitiseFilename(filename: String): String = {
    filename.stripPrefix(rootProjectDir).replaceAll( """\\/""", "/")
  }

}
