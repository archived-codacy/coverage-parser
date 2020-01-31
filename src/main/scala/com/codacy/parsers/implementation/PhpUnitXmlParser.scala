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

    report.right.flatMap(parse(rootProject, _, reportFile.getParent))
  }

  private def parse(projectRoot: File, report: NodeSeq, reportRootPath: String): Either[String, CoverageReport] = {
    val codeDirectory = report \ ProjectTag \ DirectoryTag \@ "name"
    val projectRootPath = TextUtils.sanitiseFilename(projectRoot.getAbsolutePath)

    val totalPercentage = getTotalsCoveragePercentage(report \ ProjectTag \ DirectoryTag \ TotalsTag)

    val fileNodes = report \ ProjectTag \ DirectoryTag \ "file"
    val fileReports: Either[String, Seq[CoverageFileReport]] =
      fileNodes.foldLeft[Either[String, Seq[CoverageFileReport]]](Right(Seq.empty[CoverageFileReport])) { (accum, f) =>
        accum.right.flatMap { reports =>
          val reportFileName = f \@ "href"
          val fileName = getSourceFileName(projectRootPath, codeDirectory, reportFileName)
          val coveragePercentage = getTotalsCoveragePercentage(f \ TotalsTag)

          val lineCoverage: Either[String, Map[Int, Int]] = getLineCoverage(reportRootPath, reportFileName)

          lineCoverage.right.map(CoverageFileReport(fileName, coveragePercentage, _) +: reports)
        }
      }

    fileReports.right.map(CoverageReport(totalPercentage, _))
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

    val lineCoverage: Either[String, Map[Int, Int]] = phpUnitNode.right.map { node =>
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
