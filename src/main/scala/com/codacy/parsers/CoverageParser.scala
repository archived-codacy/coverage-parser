package com.codacy.parsers

import com.codacy.api.{CoverageReport, Language}
import com.codacy.parsers.implementation.{CoberturaParser, JacocoParser}
import com.codacy.parsers.util.XML
import java.io.File
import scala.util.Try
import scala.xml.Elem

trait CoverageParser {

  val coverageReport: File
  val language: Language.Value
  val rootProject: File

  def isValidReport: Boolean

  def generateReport(): CoverageReport

}

trait XMLCoverageParser extends CoverageParser {

  val xml: Elem = Try(XML.loadFile(coverageReport)).toOption.getOrElse(<root></root>)

}

object CoverageParserFactory {

  def withCoverageReport[A](language: Language.Value, rootProject: File, reportFile: File)(block: CoverageReport => A): Either[String, A] = {
    val isEmptyReport = {
      // just starting by detecting the simplest case: a single report file
      Try(reportFile.isFile && reportFile.length() == 0).getOrElse(false)
    }

    if (isEmptyReport) {
      Left(s"report file is empty: ${reportFile.getAbsolutePath}")
    } else {
      create(language, rootProject, reportFile).map {
        parser =>
          val report = parser.generateReport()
          Right(block(report))
      }.getOrElse {
        Left(s"no parser for $language")
      }
    }
  }

  private def create(language: Language.Value, rootProject: File, reportFile: File): Option[CoverageParser] = {
    val implementations =
      Seq(
        new CoberturaParser(language, rootProject, reportFile),
        new JacocoParser(language, rootProject, reportFile)
      )

    implementations.collectFirst {
      case implementation if implementation.isValidReport =>
        implementation
    }
  }

}
