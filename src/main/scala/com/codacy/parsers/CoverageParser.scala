package com.codacy.parsers

import java.io.File

import com.codacy.api.CoverageReport
import com.codacy.parsers.implementation.{CloverParser, CoberturaParser, JacocoParser, LCOVParser, PhpUnitXmlParser}

import scala.util.Try

trait CoverageParser {
  val name: String

  def parse(rootProject: File, reportFile: File): Either[String, CoverageReport]
}

object CoverageParser {
  val parsers: List[CoverageParser] = List(CoberturaParser, JacocoParser, CloverParser, PhpUnitXmlParser, LCOVParser)

  def parse(projectRoot: File, reportFile: File): Either[String, CoverageReport] = {
    val isEmptyReport = {
      // Just starting by detecting the simplest case: a single report file
      Try(reportFile.isFile && reportFile.length() == 0).getOrElse(false)
    }

    if (isEmptyReport) {
      Left(s"Report file ${reportFile.getCanonicalPath} is empty")
    } else {
      parsers.toStream
        .map(_.parse(projectRoot, reportFile))
        .find(_.isRight)
        .getOrElse(
          Left(s"Could not parse report, unrecognized report format (tried: ${parsers.map(_.name).mkString(", ")})")
        )
    }
  }

}
