package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api._
import com.codacy.parsers.XMLCoverageParser

import scala.xml.Node

private case class LineCoverage(missedInstructions: Int, coveredInstructions: Int)

class JacocoParser(val language: Language.Value, val rootProject: File, val coverageReport: File) extends XMLCoverageParser {

  val rootProjectDir = rootProject.getAbsolutePath + File.separator

  override def isValidReport: Boolean = {
    (xml \\ "report").nonEmpty
  }

  override def generateReport(): CoverageReport = {
    val total = (xml \\ "report" \ "counter").collectFirst {
      case counter if (counter \ "@type").text == "LINE" =>
        val covered = (counter \ "@covered").text.toFloat
        val missed = (counter \ "@missed").text.toFloat
        ((covered / (covered + missed)) * 100).toInt
    }.getOrElse(0)

    val filesCoverage = (xml \\ "package").flatMap {
      `package` =>
        val packageName = (`package` \ "@name").text
        (`package` \\ "sourcefile").map {
          file =>
            val filename = (file \ "@name").text
            lineCoverage(packageName + "/" + filename, file)
        }
    }

    CoverageReport(total, filesCoverage)
  }

  private def lineCoverage(sourceFilename: String, file: Node): CoverageFileReport = {
    val classHit = (file \\ "counter").collect {
      case counter if (counter \ "@type").text == "LINE" =>
        val covered = (counter \ "@covered").text.toFloat
        val missed = (counter \ "@missed").text.toFloat
        ((covered / (covered + missed)) * 100).toInt
    }

    val fileHit = if (classHit.sum > 0) classHit.sum / classHit.length else 0

    val lineHitMap = (file \\ "line").map {
      line =>
        (line \ "@nr").text.toInt -> LineCoverage((line \ "@mi").text.toInt, (line \ "@ci").text.toInt)
    }.toMap.collect {
      case (key, lineCoverage) if lineCoverage.missedInstructions+ lineCoverage.coveredInstructions > 0 =>

        key -> (if(lineCoverage.coveredInstructions > 0) 1 else 0)
    }

    CoverageFileReport(sourceFilename.stripPrefix(rootProjectDir), fileHit, lineHitMap)
  }

}
