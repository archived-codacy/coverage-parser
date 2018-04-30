package com.codacy.parsers

import java.io.File

import com.codacy.api.{CoverageReport, Language}
import com.codacy.parsers.implementation.{CoberturaParser, JacocoParser}
import com.codacy.parsers.util.XML

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

trait CoverageParserFactory {
  def apply(language: Language.Value, rootProject: File, reportFile: File): CoverageParser
}

object CoverageParserFactory {

  def withCoverageReport[A](language: Language.Value,
                            rootProject: File,
                            reportFile: File,
                            parserFactory: Option[CoverageParserFactory] = None
                           )(block: CoverageReport => A): Either[String, A] = {
    val isEmptyReport = {
      // just starting by detecting the simplest case: a single report file
      Try(reportFile.isFile && reportFile.length() == 0).getOrElse(false)
    }

    if (isEmptyReport) {
      Left(s"report file is empty: ${reportFile.getAbsolutePath}")
    } else {
      parserFactory.fold[Either[String, A]] {
        val parsers = allParsers(language, rootProject, reportFile)
        withReport(parsers)(s"could not parse report with any parser")(block)
      } { parserFactory =>
        val parser = parserFactory(language, rootProject, reportFile)
        withReport(Seq(parser))("could not parse report with the provided parser")(block)
      }
    }
  }

  private def allParsers(language: Language.Value, rootProject: File, reportFile: File): Seq[CoverageParser] = {
    Seq(
      new CoberturaParser(language, rootProject, reportFile),
      new JacocoParser(language, rootProject, reportFile)
    )
  }

  private def withReport[A](parsers: Seq[CoverageParser])(errorMessage: String)(block: CoverageReport => A): Either[String, A] = {
    parsers
      .find(_.isValidReport)
      .fold[Either[String, A]](Left(errorMessage))(parser => Right(block(parser.generateReport())))
  }

}
