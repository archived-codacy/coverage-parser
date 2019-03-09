package com.codacy.parsers.implementation

import java.io.File
import java.text.NumberFormat
import java.util.Locale

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.util.LanguageUtils
import com.codacy.parsers.{CoverageParser, CoverageParserFactory, XMLCoverageParser}
import com.codacy.plugins.api.languages.Language

import scala.util.Try
import scala.xml.Node

object CoberturaParser extends CoverageParserFactory {
  override def apply(language: Language, rootProject: File, reportFile: File): CoverageParser =
    new CoberturaParser(language, rootProject, reportFile)
}

class CoberturaParser(val language: Language, val rootProject: File, val coverageReport: File)
    extends XMLCoverageParser {

  override val name = "Cobertura"

  val rootProjectDir = sanitiseFilename(rootProject.getAbsolutePath + File.separator)

  lazy val allFiles = recursiveListFiles(rootProject) { file =>
    LanguageUtils.getExtension(language).fold(true)(file.getName.endsWith(_))
  }.map(file => sanitiseFilename(file.getAbsolutePath))

  private[this] def convertToFloat(str: String): Try[Float] = {
    Try(str.toFloat).recoverWith {
      case ex =>
        Try {
          // The french locale uses the comma as a sep.
          val instance = NumberFormat.getInstance(Locale.FRANCE)
          val number = instance.parse(str)
          number.floatValue()
        }
    }
  }

  private def recursiveListFiles(root: File)(filter: File => Boolean): Array[File] = {
    val these = root.listFiles
    these.filter(filter) ++ these.filter(_.isDirectory).flatMap(d => recursiveListFiles(d)(filter))
  }

  override def isValidReport: Boolean = {
    (xml \\ "coverage").nonEmpty
  }

  def generateReport(): CoverageReport = {
    val total = (xml \\ "coverage" \ "@line-rate").headOption
      .map { total =>
        val totalValue = convertToFloat(total.text)
        (totalValue.getOrElse(0.0f) * 100).toInt
      }
      .getOrElse(0)

    val files = (xml \\ "class" \\ "@filename").map(_.text).toSet

    val filesCoverage = files.flatMap { file =>
      lineCoverage(file)
    }

    CoverageReport(total, filesCoverage.toSeq)
  }

  private def lineCoverage(sourceFilename: String): Option[CoverageFileReport] = {
    val file = (xml \\ "class").filter { n: Node =>
      (n \\ "@filename").text == sourceFilename
    }

    val classHit = (file \\ "@line-rate").map { total =>
      val totalValue = convertToFloat(total.text)
      (totalValue.getOrElse(0.0f) * 100).toInt
    }

    val fileHit = classHit.sum / classHit.length

    val lineHitMap = file
      .flatMap { n =>
        (n \\ "line").map { line =>
          (line \ "@number").text.toInt -> (line \ "@hits").text.toInt
        }
      }
      .toMap
      .collect {
        case (key, value) =>
          key -> value
      }

    allFiles.find(f => f.endsWith(sanitiseFilename(sourceFilename))).map { filename =>
      CoverageFileReport(stripRoot(filename), fileHit, lineHitMap)
    }
  }

  private def sanitiseFilename(filename: String): String = {
    filename
      .replaceAll("""\\/""", "/") // Fix for paths with \/
      .replace("\\", "/") // Fix for paths with \
  }

  private def stripRoot(filename: String): String = {
    filename.stripPrefix(rootProjectDir)
  }

}
