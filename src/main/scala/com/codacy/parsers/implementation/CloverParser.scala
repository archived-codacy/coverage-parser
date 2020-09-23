package com.codacy.parsers.implementation

import java.io.File
import java.nio.file.Paths

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.util.{MathUtils, TextUtils}
import com.codacy.parsers.{CoverageParser, XmlReportParser}

import scala.xml.{Elem, Node, NodeSeq}

object CloverParser extends CoverageParser with XmlReportParser {
  private val CoverageTag = "coverage"
  private val ProjectTag = "project"
  private val MetricsTag = "metrics"

  override val name: String = "Clover"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] =
    parseReport(reportFile, s"Could not find tag hierarchy <$CoverageTag> <$ProjectTag> <$MetricsTag> tags") { node =>
      Right(parseReportNode(rootProject, node))
    }

  override def validateSchema(xml: Elem): Boolean = (xml \\ CoverageTag \ ProjectTag \ MetricsTag).nonEmpty

  override def getRootNode(xml: Elem): NodeSeq = xml \\ CoverageTag

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
    val totalStatements = (metrics \@ "statements").toInt
    val coveredStatements = (metrics \@ "coveredstatements").toInt
    MathUtils.computePercentage(coveredStatements, totalStatements)
  }

  private def getCoverageFileReport(rootPath: String, fileNode: Node): CoverageFileReport = {
    val filePath = fileNode.attribute("path").flatMap(_.headOption.map(_.text)).map(TextUtils.sanitiseFilename)
    val filename = fileNode.attribute("name").flatMap(_.headOption.map(_.text)).map(TextUtils.sanitiseFilename)

    val relativeFilePath = filePath
      .orElse(filename)
      .fold[String] {
        throw new Exception(
          "Could not read file path due to missing 'path' and 'name' attributes in the report file element."
        )
      } {
        case path if Paths.get(path).isAbsolute =>
          path.stripPrefix(rootPath).stripPrefix("/")

        case path =>
          path
      }

    val metrics = fileNode \ MetricsTag
    val fileCoverage = getCoveragePercentage(metrics)

    val lineCoverage = (for {
      line <- fileNode \ "line"
      if (line \@ "type") == "stmt"
    } yield (line \@ "num").toInt -> (line \@ "count").toInt).toMap

    CoverageFileReport(relativeFilePath, fileCoverage, lineCoverage)
  }
}
