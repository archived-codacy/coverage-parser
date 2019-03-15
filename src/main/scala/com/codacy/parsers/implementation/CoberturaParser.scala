package com.codacy.parsers.implementation

import java.io.File
import java.text.NumberFormat
import java.util.Locale

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.XML

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

object CoberturaParser extends CoverageParser {

  override val name: String = "Cobertura"

  def parse(projectRoot: File, reportFile: File): Either[String, CoverageReport] = {
    val projectRootStr: String = sanitiseFilename(projectRoot.getAbsolutePath)

    // TODO: Show parse errors
    val xml: Elem = Try(XML.loadFile(reportFile)).toOption.getOrElse(<root></root>)

    // TODO: Avoid return
    if ((xml \\ "coverage").isEmpty) {
      return Left("invalid report")
    }

    val total = (asFloat((xml \\ "coverage" \ "@line-rate").text) * 100).toInt

    val fileReports: List[CoverageFileReport] = (for {
      (filename, classes) <- (xml \\ "class").groupBy(c => (c \ "@filename").text)
    } yield {
      val cleanFilename = sanitiseFilename(filename).stripPrefix(projectRootStr).stripPrefix("/")
      lineCoverage(cleanFilename, classes)
    })(collection.breakOut)

    Right(CoverageReport(total, fileReports))
  }

  private def lineCoverage(sourceFilename: String, classes: NodeSeq): CoverageFileReport = {
    val classHit = (classes \\ "@line-rate").map { total =>
      val totalValue = asFloat(total.text)
      (totalValue * 100).toInt
    }
    val fileHit = classHit.sum / classHit.length

    val lineHitMap: Map[Int, Int] =
      (for {
        xClass <- classes
        line <- xClass \\ "line"
      } yield (line \ "@number").text.toInt -> (line \ "@hits").text.toInt)(collection.breakOut)

    CoverageFileReport(sourceFilename, fileHit, lineHitMap)
  }

  // TODO: Move to helper
  private def asFloat(str: String): Float = {
    Try(str.toFloat).getOrElse {
      // The french locale uses the comma as a sep.
      val instance = NumberFormat.getInstance(Locale.FRANCE)
      val number = instance.parse(str)
      number.floatValue()
    }
  }

  private def sanitiseFilename(filename: String): String = {
    filename
      .replaceAll("""\\/""", "/") // Fix for paths with \/
      .replace("\\", "/") // Fix for paths with \
  }

}
