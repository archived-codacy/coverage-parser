package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node, NodeSeq}

object CloverParser extends CoverageParser {
  private val CoverageTag = "coverage"
  private val ProjectTag = "project"
  private val MetricsTag = "metrics"

  override val name: String = "Clover"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    val report = Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if hasCorrectSchema(xml) =>
        Right(xml \\ CoverageTag)

      case Success(_) =>
        Left(s"Invalid report. Could not find tag hierarchy <$CoverageTag> <$ProjectTag> <$MetricsTag> tags.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    }

    report.flatMap(
      r =>
        Try(parseReportNode(rootProject, r)) match {
          case Success(coverageReport) => Right(coverageReport)
          case Failure(ex) => Left(s"Failed to parse the report: ${ex.getMessage}")
      }
    )
  }

  private def hasCorrectSchema(xml: Elem) =
    (xml \\ CoverageTag \ ProjectTag \ MetricsTag).nonEmpty

  private def parseReportNode(rootProject: File, report: NodeSeq): CoverageReport = {
    // global metrics
    val metrics = report \ ProjectTag \ MetricsTag
    val totalCoverage = getCoveragePercentage(metrics)

    val rootPath = TextUtils.sanitiseFilename(rootProject.getAbsolutePath)
    val coverageFiles = (report \\ "file").map { f =>
      getCoverageFileReport(rootPath, f)
    }

    CoverageReport(totalCoverage, coverageFiles)
  }

  private def getCoveragePercentage(metrics: NodeSeq) = {
    val totalStatements = TextUtils.asFloat(metrics \@ "statements")
    val coveredStatements = TextUtils.asFloat(metrics \@ "coveredstatements")
    val totalCoverage = if (totalStatements != 0) (coveredStatements / totalStatements) * 100 else 0
    scala.math.round(totalCoverage)
  }

  private def getCoverageFileReport(rootPath: String, fileNode: Node) = {
    val filename = fileNode \@ "name"
    val cleanFileName = TextUtils.sanitiseFilename(filename).stripPrefix(rootPath).stripPrefix("/")

    val metrics = fileNode \ MetricsTag
    val fileCoverage = getCoveragePercentage(metrics)

    val lineCoverage = (for {
      line <- fileNode \ "line"
      if (line \@ "type") == "stmt"
    } yield (line \@ "num").toInt -> (line \@ "count").toInt).toMap

    CoverageFileReport(cleanFileName, fileCoverage, lineCoverage)
  }
}
