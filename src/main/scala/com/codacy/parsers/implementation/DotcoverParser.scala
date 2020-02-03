package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq}

case class StatementNode(fileIndex: Int, line: Int, covered: Boolean)

object DotcoverParser extends CoverageParser {
  override val name: String = "DotCover"

  private val RootTag = "Root"
  private val CoverageAttribute = "CoveragePercent"
  private val CoveredAttribute = "Covered"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    loadXml(reportFile).right.map(parse(rootProject, _))
  }

  private def parse(rootProject: File, rootNode: NodeSeq): CoverageReport = {
    val projectRootStr: String = TextUtils.sanitiseFilename(rootProject.getAbsolutePath)

    val totalCoverage = (rootNode \@ CoverageAttribute).toInt

    val fileIndices: Map[Int, String] = (rootNode \ "FileIndices" \ "File").map { x =>
      (x \@ "Index").toInt -> (x \@ "Name")
    }.toMap

    val statementsPerFile: Map[Int, NodeSeq] = (rootNode \\ "Statement").groupBy(x => (x \@ "FileIndex").toInt)

    val fileReports = for {
      (fileIndex, statements) <- statementsPerFile
      filename = TextUtils.sanitiseFilename(fileIndices(fileIndex)).stripPrefix(projectRootStr).stripPrefix("/")
      lineCoverage = getLineCoverage(statements)
      totalLines = lineCoverage.keys.size
      coveredLines = lineCoverage.values.count(_ > 0)
      total = if (totalLines == 0) 0 else math.round((coveredLines.toFloat / totalLines) * 100)
    } yield {
      CoverageFileReport(filename, total, lineCoverage)
    }

    CoverageReport(totalCoverage, fileReports.toSeq)
  }

  private def loadXml(reportFile: File) = {
    Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if hasCorrectSchema(xml) =>
        Right(xml \\ RootTag)

      case Success(_) =>
        Left(s"Invalid report. Could not find tag <$RootTag $CoverageAttribute=...>.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    }
  }

  private def hasCorrectSchema(elem: Elem) =
    (elem \\ RootTag \ s"@$CoverageAttribute").nonEmpty

  private def getLineCoverage(statementNodes: NodeSeq) = {
    val lines = for {
      node <- statementNodes
      // a statement can extend over several lines
      line <- (node \@ "Line").toInt to (node \@ "EndLine").toInt
      coveredValue = if ((node \@ CoveredAttribute).toBoolean) 1 else 0
    } yield {
      (line, coveredValue)
    }

    lines.toMap
  }
}
