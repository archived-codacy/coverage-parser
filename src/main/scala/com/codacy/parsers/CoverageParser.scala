package com.codacy.parsers

import java.io.File

import com.codacy.api.CoverageReport
import com.codacy.parsers.implementation._

import scala.util.Try

trait CoverageParser {
  val name: String

  def parse(rootProject: File, reportFile: File): Either[String, CoverageReport]
}

object CoverageParser {

  val allParsers: List[CoverageParser] =
    List(CoberturaParser, JacocoParser, CloverParser, OpenCoverParser, DotcoverParser, PhpUnitXmlParser, LCOVParser)

  def parse(
      projectRoot: File,
      reportFile: File,
      forceParser: Option[CoverageParser] = None
  ): Either[String, CoverageReport] = {
    val isEmptyReport = {
      // Just starting by detecting the simplest case: a single report file
      Try(reportFile.isFile && reportFile.length() == 0).getOrElse(false)
    }

    val parsers = forceParser match {
      case Some(parser) => List(parser)
      case _ => allParsers
    }

    object ParsedCoverage {
      def unapply(parser: CoverageParser): Option[CoverageReport] = {
        parser.parse(projectRoot, reportFile).toOption
      }
    }

    if (isEmptyReport) {
      Left(s"Report file ${reportFile.getCanonicalPath} is empty")
    } else {
      parsers.view
        .collectFirst {
          case ParsedCoverage(report: CoverageReport) => Right(report)
        }
        .getOrElse(
          Left(s"Could not parse report, unrecognized report format (tried: ${parsers.map(_.name).mkString(", ")})")
        )
    }
  }

}
