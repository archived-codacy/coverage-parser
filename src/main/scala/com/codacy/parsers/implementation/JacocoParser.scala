package com.codacy.parsers.implementation

import java.io.File
import java.text.NumberFormat
import java.util.Locale

import com.codacy.api._
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.XML

import scala.util.Try
import scala.xml.{Elem, Node}

private case class LineCoverage(missedInstructions: Int, coveredInstructions: Int)

object JacocoParser extends CoverageParser {

  override val name: String = "Jacoco"

  def parse(projectRoot: File, reportFile: File): Either[String, CoverageReport] = {
    val projectRootStr: String = sanitiseFilename(projectRoot.getAbsolutePath)

    // TODO: Show parse errors
    val xml: Elem = Try(XML.loadFile(reportFile)).toOption.getOrElse(<root></root>)

    // TODO: Avoid return
    if ((xml \\ "report").isEmpty) {
      return Left("invalid report")
    }

    val total =
      (xml \\ "report" \ "counter")
        .collectFirst {
          case counter if (counter \ "@type").text == "LINE" =>
            val covered = asFloat((counter \ "@covered").text)
            val missed = asFloat((counter \ "@missed").text)
            ((covered / (covered + missed)) * 100).toInt
        }
        .getOrElse {
          // TODO: Avoid throwing exception
          throw new Exception("Could not retrieve total coverage")
        }

    val filesCoverage = for {
      pkg <- xml \\ "package"
      packageName = (pkg \ "@name").text
      sourcefile <- pkg \\ "sourcefile"
    } yield {
      val filename =
        sanitiseFilename(s"$packageName/${(sourcefile \ "@name").text}").stripPrefix(projectRootStr).stripPrefix("/")
      lineCoverage(filename, sourcefile)
    }

    Right(CoverageReport(total, filesCoverage))
  }

  private def lineCoverage(filename: String, fileNode: Node): CoverageFileReport = {
    val lineHit = (fileNode \ "counter").collect {
      case counter if (counter \ "@type").text == "LINE" =>
        val covered = asFloat((counter \ "@covered").text)
        val missed = asFloat((counter \ "@missed").text)
        ((covered / (covered + missed)) * 100).toInt
    }

    val fileHit =
      if (lineHit.sum > 0) { lineHit.sum / lineHit.length } else {
        throw new Exception("Could not retrieve file line coverage")
      }

    val lineHitMap: Map[Int, Int] = (fileNode \\ "line")
      .map { line =>
        (line \ "@nr").text.toInt -> LineCoverage((line \ "@mi").text.toInt, (line \ "@ci").text.toInt)
      }
      .collect {
        case (key, lineCoverage) if lineCoverage.missedInstructions + lineCoverage.coveredInstructions > 0 =>
          key -> (if (lineCoverage.coveredInstructions > 0) 1 else 0)
      }(collection.breakOut)

    CoverageFileReport(filename, fileHit, lineHitMap)
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
