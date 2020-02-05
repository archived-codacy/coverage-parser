package com.codacy.parsers.implementation

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport}
import com.codacy.parsers.CoverageParser
import com.codacy.parsers.util.{TextUtils, XMLoader}

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq}

object OpenCoverParser extends CoverageParser {
  private val RootTag = "CoverageSession"
  private val IdAttribute = "uid"
  private val FileRefTag = "FileRef"
  private val LineAttribute = "sl"
  private val VisitCounterAttribute = "vc"

  override val name: String = "OpenCover"

  override def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    loadXml(reportFile).right.flatMap { r =>
      Try(parseReportNode(r, TextUtils.sanitiseFilename(rootProject.getAbsolutePath))) match {
        case Success(coverageReport) => Right(coverageReport)
        case Failure(ex) => Left(s"Failed to parse report with error: ${ex.getMessage}")
      }
    }
  }

  private def parseReportNode(rootNode: NodeSeq, projectRoot: String): CoverageReport = {
    val fileIndices: Map[Int, String] = (rootNode \\ "Files" \ "File").map { n =>
      (n \@ IdAttribute).toInt -> n \@ "fullPath"
    }.toMap

    val validMethods = (rootNode \\ "Method").filter(m => (m \ FileRefTag).nonEmpty)

    val fileReports = (for {
      (fileIndex, methods) <- validMethods.groupBy(m => (m \ FileRefTag \@ IdAttribute).toInt)
      filename <- fileIndices.get(fileIndex)

      sanitisedFileName = TextUtils.sanitiseFilename(filename).stripPrefix(projectRoot).stripPrefix("/")
      lineCoverage = getLineCoverage(methods, sanitisedFileName)
      totalLines = lineCoverage.size
      coveredLines = lineCoverage.count(_._2 > 0)
      coverage = computePercentage(coveredLines, totalLines)
    } yield {
      CoverageFileReport(sanitisedFileName, coverage, lineCoverage)
    }).toSeq

    val totalCoverage = computeTotalCoverage(fileReports)

    CoverageReport(totalCoverage, fileReports)
  }

  private def getLineCoverage(methodNodes: NodeSeq, filename: String) = {
    val lineCoverage = for {
      methodNode <- methodNodes
      sequencePoint <- methodNode \\ "SequencePoint"
    } yield {
      (sequencePoint \@ LineAttribute).toInt -> (sequencePoint \@ VisitCounterAttribute).toInt
    }

    lineCoverage.toMap
  }

  private def computeTotalCoverage(fileReports: Seq[CoverageFileReport]) = {
    val (totalLines, coveredLines) = fileReports
      .foldLeft((0, 0)) {
        case ((total, covered), f) =>
          val totalLines = f.coverage.size
          val coveredLines = (f.total * totalLines) / 100
          (total + totalLines, covered + coveredLines)
      }

    val totalCoverage = computePercentage(coveredLines, totalLines)
    totalCoverage
  }

  private def loadXml(reportFile: File) = {
    Try(XMLoader.loadFile(reportFile)) match {
      case Success(xml) if hasCorrectSchema(xml) =>
        Right(xml \\ RootTag)

      case Success(_) =>
        Left(s"Invalid report. Could not find tag <$RootTag>.")

      case Failure(ex) =>
        Left(s"Unparseable report. ${ex.getMessage}")
    }
  }

  private def hasCorrectSchema(elem: Elem) =
    (elem \\ RootTag).nonEmpty

  private def computePercentage(covered: Int, total: Int) =
    if (total == 0) 0 else math.round((covered.toFloat / total) * 100)
}
