package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport, Language}
import com.codacy.parsers.XMLCoverageParser
import com.codacy.parsers.util.LanguageUtils

import scala.xml.Node

class CoberturaParser(val language: Language.Value, val rootProject: File, val coverageReport: File) extends XMLCoverageParser {

  val rootProjectDir = rootProject.getAbsolutePath + File.separator
  lazy val allFiles = recursiveListFiles(rootProject)(f => f.getName.endsWith(LanguageUtils.getExtension(language)))

  private def recursiveListFiles(root: File)(filter: File => Boolean): Array[File] = {
    val these = root.listFiles
    these.filter(filter) ++ these.filter(_.isDirectory).flatMap(d => recursiveListFiles(d)(filter))
  }

  override def isValidReport: Boolean = {
    (xml \\ "coverage").nonEmpty
  }

  def generateReport(): CoverageReport = {
    val total = (xml \\ "coverage" \ "@line-rate").headOption.map {
      total =>
        (total.text.toFloat * 100).toInt
    }.getOrElse(0)

    val files = (xml \\ "class" \\ "@filename").map(_.text).toSet

    val filesCoverage = files.flatMap {
      file =>
        lineCoverage(file)
    }

    CoverageReport(language, total, filesCoverage.toSeq)
  }

  private def lineCoverage(sourceFilename: String): Option[CoverageFileReport] = {
    val file = (xml \\ "class").filter {
      n: Node =>
        (n \\ "@filename").text == sourceFilename
    }

    val classHit = (file \\ "@line-rate").map {
      total =>
        (total.text.toFloat * 100).toInt
    }

    val fileHit = classHit.sum / classHit.length

    val lineHitMap = file.flatMap {
      n =>
        (n \\ "line").map {
          line =>
            (line \ "@number").text.toInt -> (line \ "@hits").text.toInt
        }
    }.toMap.collect {
      case (key, value) if value > 0 =>
        key -> value
    }

    allFiles.find(f => f.getAbsolutePath.endsWith(sourceFilename)).map {
      filename =>
        CoverageFileReport(sanitiseFilename(filename.getAbsolutePath), fileHit, lineHitMap)
    }
  }

  private def sanitiseFilename(filename: String): String = {
    filename.stripPrefix(rootProjectDir).replaceAll( """\\/""", "/")
  }

}
