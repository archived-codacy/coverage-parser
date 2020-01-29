package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

object PhpUnitXmlParser extends CoverageParser {
  override val name: String = "PHPUnit"

  private val PHPUNIT = "phpunit"
  private val PROJECT = "project"
  private val DIRECTORY = "directory"
  private val TOTALS = "totals"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    val report = loadPhpUnitFile(reportFile)

    report.right.flatMap(parse(rootProject, _, reportFile.getParent))
  }

  private def parse(projectRoot: File, report: NodeSeq, reportRootPath: String): Either[String, CoverageReport] = {
    val codeDirectory = (report \ PROJECT \ DIRECTORY \ "@name").text
    val projectRootPath = TextUtils.sanitiseFilename(projectRoot.getAbsolutePath)

    val totalPercentage = getTotalsCoveragePercentage(report \ PROJECT \ DIRECTORY \ TOTALS)

    val fileNodes = report \ PROJECT \ DIRECTORY \ "file"
    val fileReports: Either[String, Seq[CoverageFileReport]] =
      fileNodes.foldLeft[Either[String, Seq[CoverageFileReport]]](Right(Seq.empty[CoverageFileReport])) { (accum, f) =>
        accum.right.flatMap { reports =>
          val reportFileName = (f \ "@href").text
          val fileName = getSourceFileName(projectRootPath, codeDirectory, reportFileName)
          val coveragePercentage = getTotalsCoveragePercentage(f \ TOTALS)

          val lineCoverage: Either[String, Map[Int, Int]] = getLineCoverage(reportRootPath, reportFileName)

          lineCoverage.right.map(CoverageFileReport(fileName, coveragePercentage, _) +: reports)
        }
      }

    fileReports.right.map(CoverageReport(totalPercentage, _))
  }

  private def loadPhpUnitFile(reportFile: File) = {
    Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if (xml \\ PHPUNIT).nonEmpty =>
        Right(xml \\ PHPUNIT)

      case Success(_) =>
        Left(s"Invalid report. Could not find top level <$PHPUNIT> tag.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    }
  }

  private def getLineCoverage(reportRootPath: String, filename: String) = {
    val coverageDetailFile = new File(reportRootPath, filename)
    val phpUnitNode = loadPhpUnitFile(coverageDetailFile)

    val lineCoverage: Either[String, Map[Int, Int]] = phpUnitNode.right.map { node =>
      (node \\ "coverage" \\ "line").map { line =>
        (line \ "@nr").text.toInt -> (line \ "covered").length
      }(collection.breakOut)
    }
    lineCoverage
  }

  private def getTotalsCoveragePercentage(totals: NodeSeq) = {
    val percentageStr = (totals \ "lines" \ "@percent").text.dropRight(1)
    scala.math.round(percentageStr.toFloat)
  }

  private def getSourceFileName(pathToRemove: String, codeRootDirectory: String, reportRelativePath: String) = {
    new File(codeRootDirectory, reportRelativePath).getAbsolutePath
      .stripPrefix(pathToRemove)
      .stripPrefix("/")
      .stripSuffix(".xml")
  }
}
