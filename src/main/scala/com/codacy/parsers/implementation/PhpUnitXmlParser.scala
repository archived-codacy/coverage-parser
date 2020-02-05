package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

object PhpUnitXmlParser extends CoverageParser {
  override val name: String = "PHPUnit"

  private val PhpUnitTag = "phpunit"
  private val ProjectTag = "project"
  private val DirectoryTag = "directory"
  private val TotalsTag = "totals"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    val report = loadPhpUnitFile(reportFile)

    report.flatMap { r =>
      Try(parseReportNode(rootProject, r, reportFile.getParent)) match {
        case Success(reportEither) => Right(reportEither)
        case Failure(ex) => Left(s"Failed to parse the report: ${ex.getMessage}")
      }
    }
  }

  private def parseReportNode(projectRoot: File, report: NodeSeq, reportRootPath: String): CoverageReport = {
    val codeDirectory = report \ ProjectTag \ DirectoryTag \@ "name"
    val projectRootPath = TextUtils.sanitiseFilename(projectRoot.getAbsolutePath)

    val totalPercentage = getTotalsCoveragePercentage(report \ ProjectTag \ DirectoryTag \ TotalsTag)

    val fileReports = for {
      fileNode <- report \ ProjectTag \ DirectoryTag \ "file"
      reportFileName = fileNode \@ "href"
      fileName = getSourceFileName(projectRootPath, codeDirectory, reportFileName)
      coveragePercentage = getTotalsCoveragePercentage(fileNode \ TotalsTag)
      lineCoverage = getLineCoverage(reportRootPath, reportFileName).right.getOrElse(throw new Exception("ERROR!"))
    } yield CoverageFileReport(fileName, coveragePercentage, lineCoverage)

    CoverageReport(totalPercentage, fileReports)
  }

  private def loadPhpUnitFile(reportFile: File) = {
    Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if (xml \\ PhpUnitTag).nonEmpty =>
        Right(xml \\ PhpUnitTag)

      case Success(_) =>
        Left(s"Invalid report. Could not find top level <$PhpUnitTag> tag.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    }
  }

  private def getLineCoverage(reportRootPath: String, filename: String) = {
    val coverageDetailFile = new File(reportRootPath, filename)
    val phpUnitNode = loadPhpUnitFile(coverageDetailFile)

    val lineCoverage: Either[String, Map[Int, Int]] = phpUnitNode.map { node =>
      (node \\ "coverage" \\ "line").map { line =>
        (line \@ "nr").toInt -> (line \ "covered").length
      }.toMap
    }
    lineCoverage
  }

  private def getTotalsCoveragePercentage(totals: NodeSeq) = {
    val percentageStr = (totals \ "lines" \@ "percent").dropRight(1)
    scala.math.round(percentageStr.toFloat)
  }

  private def getSourceFileName(pathToRemove: String, codeRootDirectory: String, reportRelativePath: String) = {
    new File(codeRootDirectory, reportRelativePath).getAbsolutePath
      .stripPrefix(pathToRemove)
      .stripPrefix("/")
      .stripSuffix(".xml")
  }
}
