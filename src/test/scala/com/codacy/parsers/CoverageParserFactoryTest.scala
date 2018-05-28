package com.codacy.parsers

import java.io.File

import com.codacy.api.{CoverageFileReport, CoverageReport, Language}
import com.codacy.parsers.implementation.{CoberturaParser, JacocoParser}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class CoverageParserFactoryTest extends WordSpec with BeforeAndAfterAll with Matchers {
  "CoverageParserFactory" should {
    "get report with unspecified parser" in {
      val expectedReport = CoverageReport(87, List(
        CoverageFileReport("src/test/resources/TestSourceFile.scala", 87,
          Map(5 -> 1, 10 -> 1, 6 -> 2, 9 -> 1, 3 -> 0, 4 -> 1)),
        CoverageFileReport("src/test/resources/TestSourceFile2.scala", 87,
          Map(1 -> 1, 2 -> 1, 3 -> 1))))

      runForFile("src/test/resources/test_cobertura.xml", None) shouldEqual Right(expectedReport)
    }

    "get report with jacoco parser" in {
      val expectedReport = CoverageReport(73, List(
        CoverageFileReport("org/eluder/coverage/sample/InnerClassCoverage.java", 81,
          Map(10 -> 1, 6 -> 1, 9 -> 1, 13 -> 1, 22 -> 1, 27 -> 0, 12 -> 1, 3 -> 1, 16 -> 1, 26 -> 0, 19 -> 1)),
        CoverageFileReport("org/eluder/coverage/sample/SimpleCoverage.java", 50,
          Map(3 -> 1, 6 -> 1, 10 -> 0, 11 -> 0))))

      runForFile("src/test/resources/test_jacoco.xml", Some(JacocoParser)) shouldEqual Right(expectedReport)
    }


    "fail to get report with wrong parser" in {
      runForFile("src/test/resources/test_jacoco.xml", Some(CoberturaParser)) shouldEqual
        Left("could not parse report with the provided parser")
    }
  }

  private def runForFile(file: String, parser: Option[CoverageParserFactory]) = {
    CoverageParserFactory.withCoverageReport(
      Language.Scala,
      new File("."),
      new File(file),
      parser
    )(identity)
  }
}
