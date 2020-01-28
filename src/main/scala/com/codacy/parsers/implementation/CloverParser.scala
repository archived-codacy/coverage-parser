package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node, NodeSeq}

object CloverParser extends CoverageParser {
  private val COVERAGE = "coverage"
  private val PROJECT = "project"
  private val METRICS = "metrics"

  override val name: String = "Clover"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    val report = Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if hasCorrectSchema(xml) =>
        Right(xml \\ COVERAGE)

      case Success(_) =>
        Left(s"Invalid report. Could not find tag hierarchy <$COVERAGE> <$PROJECT> <$METRICS> tags.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    }

    report.right.flatMap(parse(rootProject, _))
  }

  private def hasCorrectSchema(xml: Elem) =
    (xml \\ COVERAGE \ PROJECT \ METRICS).nonEmpty

  private def parse(rootProject: File, report: NodeSeq): Either[String, CoverageReport] = {
    // global metrics
    val metrics = report \ PROJECT \ METRICS
    val totalCoverage = getCoveragePercentage(metrics)

    val rootPath = TextUtils.sanitiseFilename(rootProject.getAbsolutePath)
    val coverageFiles = (report \\ "file").map { f =>
      getCoverageFileReport(rootPath, f)
    }

    Right(CoverageReport(totalCoverage, coverageFiles))
  }

  private def getCoveragePercentage(metrics: NodeSeq) = {
    val totalStatements = TextUtils.asFloat((metrics \ "@statements").text)
    val coveredStatements = TextUtils.asFloat((metrics \ "@coveredstatements").text)
    val totalCoverage = if (totalStatements != 0) (coveredStatements / totalStatements) * 100 else 0
    scala.math.round(totalCoverage)
  }

  private def getCoverageFileReport(rootPath: String, fileNode: Node) = {
    val filename = (fileNode \ "@name").text
    val cleanFileName = TextUtils.sanitiseFilename(filename).stripPrefix(rootPath).stripPrefix("/")

    val metrics = fileNode \ METRICS
    val fileCoverage = getCoveragePercentage(metrics)

    val lineCoverage = (for {
      line <- fileNode \ "line"
      if (line \ "@type").text == "stmt"
    } yield (line \ "@num").text.toInt -> (line \ "@count").text.toInt).toMap

    CoverageFileReport(cleanFileName, fileCoverage, lineCoverage)
  }
}
