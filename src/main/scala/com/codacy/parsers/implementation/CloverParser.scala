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

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    parseReport(reportFile, s"Could not find tag hierarchy <$CoverageTag> <$ProjectTag> <$MetricsTag> tags") { node =>
      parseReportNode(rootProject, node)
    }
  }

  override def validateSchema(xml: Elem): Boolean = (xml \\ CoverageTag \ ProjectTag \ MetricsTag).nonEmpty

  override def getRootNode(xml: Elem): NodeSeq = xml \\ CoverageTag

  private def parseReportNode(rootProject: File, report: NodeSeq): Either[String, CoverageReport] = {
    val metricsNode = report \ ProjectTag \ MetricsTag
    val totalCoverage = getCoveragePercentage(metricsNode)

    val rootPath = TextUtils.sanitiseFilename(rootProject.getAbsolutePath)

    val coverageFiles = (report \\ "file").foldLeft[Either[String, Seq[CoverageFileReport]]](Right(List())) {
      case (Right(accomulatedFileReports), fileElement) =>
        getCoverageFileReport(rootPath, fileElement).fold(Left(_), { fileReport =>
          Right(fileReport +: accomulatedFileReports)
        })

      case (Left(errorMessage), _) => Left(errorMessage)
    }

    coverageFiles.right.map(CoverageReport(totalCoverage, _))
  }

  private def getCoveragePercentage(metrics: NodeSeq): Int = {
    val totalStatements = (metrics \@ "statements").toInt
    val coveredStatements = (metrics \@ "coveredstatements").toInt
    MathUtils.computePercentage(coveredStatements, totalStatements)
  }

  private def getCoverageFileReport(rootPath: String, fileNode: Node): Either[String, CoverageFileReport] = {
    val filePath = getUnixPathAttribute(fileNode, "path")
    val filename = getUnixPathAttribute(fileNode, "name")

    filePath
      .orElse(filename)
      .fold[Either[String, String]] {
        Left("Could not read file path due to missing 'path' and 'name' attributes in the report file element.")
      } {
        case path if Paths.get(path).isAbsolute =>
          Right(path.stripPrefix(rootPath).stripPrefix("/"))

        case path =>
          Right(path)
      }
      .right
      .map { relativeFilePath =>
        val metricsNode = fileNode \ MetricsTag
        val fileCoverage = getCoveragePercentage(metricsNode)

        val lineCoverage: Map[Int, Int] = (fileNode \ "line").collect {
          case line if (line \@ "type") == "stmt" =>
            val lineNumber = (line \@ "num").toInt
            val countOfExecutions = (line \@ "count").toInt

            (lineNumber, countOfExecutions)
        }(collection.breakOut)

        CoverageFileReport(relativeFilePath, fileCoverage, lineCoverage)
      }
  }

  /* Retrieves the attribute with name @attributeName from @node,
   * converts the contents to string and converts path to unix style
   */
  private def getUnixPathAttribute(node: Node, attributeName: String): Option[String] = {
    node.attribute(attributeName).flatMap(_.headOption.map(_.text)).map(TextUtils.sanitiseFilename)
  }

}
