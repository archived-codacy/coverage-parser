package com.codacy.parsers

import java.io.File

import com.codacy.parsers.implementation.CloverParser
import org.scalatest.{BeforeAndAfterAll, EitherValues, Matchers, WordSpec}

class CloverParserTest extends WordSpec with BeforeAndAfterAll with Matchers with EitherValues {
  "CloverParser" should {
    "identify if report is invalid" in {
      // use some coverage file that does not follow the Clover format
      val reader = CloverParser.parse(
        new File("."),
        new File("src/test/resources/test_cobertura.xml"))

      reader.isLeft shouldBe true
    }

    "identify if report is valid" in {
      val reader = CloverParser.parse(
        new File("."),
        new File("src/test/resources/test_clover.xml"))
      reader.isRight shouldBe true
    }

    "return a valid report" in {
      val reader = CloverParser.parse(
        new File("/home/codacy-php/"),
        new File("src/test/resources/test_clover.xml"))


      val report = reader.right.value
      report.total shouldBe 38
      // 5 files used in the initial test
      report.fileReports.length shouldBe 5

      // coverage percentage for file Parser.php
      report.fileReports.head.total shouldBe 33
      report.fileReports.head.filename shouldBe "src/Codacy/Coverage/Parser/Parser.php"

      // check only the CoverageReport.php
      report.fileReports(1).total shouldBe 33
      report.fileReports(1).filename shouldBe "src/Codacy/Coverage/Report/CoverageReport.php"
      report.fileReports(1).coverage shouldBe Map(11 -> 1, 12 -> 1, 13 -> 1, 16 -> 1,
      19 -> 0, 30 -> 0, 31 -> 0, 32 -> 0, 33 -> 0, 36 -> 0, 39 -> 0, 42 -> 0)
    }

    "return a valid report even without packages" in {
      val readerWithoutPackages = CloverParser.parse(
        new File("/home/codacy-php/"),
        new File("src/test/resources/test_clover_without_packages.xml"))

      val readerWithPackages = CloverParser.parse(
        new File("/home/codacy-php/"),
        new File("src/test/resources/test_clover.xml"))

      readerWithoutPackages shouldBe readerWithPackages
    }
  }
}
