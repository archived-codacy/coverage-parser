package com.codacy.parsers

import java.io.File

import com.codacy.api.{CoverageReport, Language}
import com.codacy.parsers.implementation.{JacocoParser, CoberturaParser}
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

class CoverageParserFactory(language: Language.Value, rootProject: File) {

  def create(reportFiles: Seq[File]): Option[CoverageParser] = {
    val implementations = reportFiles.flatMap {
      reportFile =>
        Seq(
          new CoberturaParser(language, rootProject, reportFile),
          new JacocoParser(language, rootProject, reportFile)
        )
    }

    implementations.collectFirst {
      case implementation if implementation.isValidReport =>
        implementation
    }
  }

}
