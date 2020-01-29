package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq}

object CoberturaParser extends CoverageParser {

  override val name: String = "Cobertura"

  def parse(projectRoot: File, reportFile: File): Either[String, CoverageReport] = {
    val report = (Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if hasCorrectSchema(xml) =>
        Right(xml \\ "coverage")

      case Success(_) =>
        Left("Invalid report. Could not find top level <coverage> tag.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    })

    report.right.flatMap(parse(projectRoot, _))
  }

  // restricting the schema to <coverage line-rate=...>
  // ensures this will not consider Clover reports which also have a <coverage> tag
  private def hasCorrectSchema(xml: Elem) = {
    (xml \\ "coverage" \ "@line-rate").nonEmpty
  }

  private def parse(projectRoot: File, report: NodeSeq): Either[String, CoverageReport] = {
    val projectRootStr: String = TextUtils.sanitiseFilename(projectRoot.getAbsolutePath)

    val total = (TextUtils.asFloat((report \\ "coverage" \ "@line-rate").text) * 100).toInt

    val fileReports: List[CoverageFileReport] = (for {
      (filename, classes) <- (report \\ "class").groupBy(c => (c \ "@filename").text)
    } yield {
      val cleanFilename = TextUtils.sanitiseFilename(filename).stripPrefix(projectRootStr).stripPrefix("/")
      lineCoverage(cleanFilename, classes)
    })(collection.breakOut)

    Right(CoverageReport(total, fileReports))
  }

  private def lineCoverage(sourceFilename: String, classes: NodeSeq): CoverageFileReport = {
    val classHit = (classes \\ "@line-rate").map { total =>
      val totalValue = TextUtils.asFloat(total.text)
      (totalValue * 100).toInt
    }
    val fileHit = if (classHit.length != 0) { classHit.sum / classHit.length } else 0

    val lineHitMap: Map[Int, Int] =
      (for {
        xClass <- classes
        line <- xClass \\ "line"
      } yield (line \ "@number").text.toInt -> (line \ "@hits").text.toInt)(collection.breakOut)

    CoverageFileReport(sourceFilename, fileHit, lineHitMap)
  }

}
