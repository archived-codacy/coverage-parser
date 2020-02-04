package com.codacy.parsers

import java.io.File

import com.codacy.parsers.implementation.CloverParser
import org.scalatest.{BeforeAndAfterAll, EitherValues, Matchers, WordSpec}

class CloverParserTest extends WordSpec with BeforeAndAfterAll with Matchers with EitherValues {
  private val coberturaReportPath = "src/test/resources/test_cobertura.xml"
  private val nonExistentReportPath = "src/test/resources/non-existent.xml"
  private val cloverReportPath = "src/test/resources/test_clover.xml"
  private val cloverWithoutPackagesFilePath = "src/test/resources/test_clover_without_packages.xml"
  private val invalidCloverReportPath = "src/test/resources/test_invalid_clover.xml"
  "parse" should {
    "identify report as invalid" when {
      "file does not follow the Cobertura format" in {
        // use some coverage file that does not follow the Clover format
        val reader = CloverParser.parse(new File("."), new File(coberturaReportPath))

        reader shouldBe 'left
      }

      "file does not exist" in {
        val reader = CloverParser.parse(new File("."), new File(nonExistentReportPath))

        reader shouldBe 'left
      }

      "file does not follow the Clover schema" in {
        val reader = CloverParser.parse(new File("."), new File(invalidCloverReportPath))
        reader shouldBe 'left
      }
    }

    "identify the report as valid" when {
      "report has packages" in {
        val reader = CloverParser.parse(new File("."), new File(cloverReportPath))
        reader shouldBe 'right
      }

      "report does not have packages" in {
        val readerWithoutPackages = CloverParser
          .parse(new File("/home/codacy-php/"), new File(cloverWithoutPackagesFilePath))

        readerWithoutPackages shouldBe 'right
      }
    }

    "return the same report with or without packages" in {
      val readerWithoutPackages = CloverParser
        .parse(new File("/home/codacy-php/"), new File(cloverWithoutPackagesFilePath))

      val readerWithPackages =
        CloverParser.parse(new File("/home/codacy-php/"), new File(cloverReportPath))

      readerWithoutPackages shouldBe readerWithPackages
    }

    "return a report with the expected number of files" in {
      val report =
        CloverParser.parse(new File("/home/codacy-php/"), new File(cloverReportPath)).right.value
      report.fileReports should have length 5
    }

    "return a report with the expected total coverage" in {
      val report =
        CloverParser.parse(new File("/home/codacy-php/"), new File(cloverReportPath)).right.value
      report.total shouldBe 38
    }

    "return a report with the expected file names" in {
      val report =
        CloverParser.parse(new File("/home/codacy-php/"), new File(cloverReportPath)).right.value
      report.fileReports.map(_.filename).sorted shouldBe Seq(
        "src/Codacy/Coverage/Parser/Parser.php",
        "src/Codacy/Coverage/Report/CoverageReport.php",
        "vendor/sebastian/global-state/src/Blacklist.php",
        "vendor/sebastian/global-state/src/Restorer.php",
        "vendor/sebastian/global-state/src/Snapshot.php"
      ).sorted
    }

    "return a report with the expected file coverage" in {
      val report =
        CloverParser.parse(new File("/home/codacy-php/"), new File(cloverReportPath)).right.value
      // coverage percentage for file Parser.php
      val firstFileReport = report.fileReports.headOption.getOrElse(fail("file reports list is empty"))
      firstFileReport.total shouldBe 33

      // coverage percentage for file Parser.php
      val secondFileReport = report.fileReports(1)
      secondFileReport.total shouldBe 33
    }

    "return a report with the expected file line coverage" in {
      val report =
        CloverParser.parse(new File("/home/codacy-php/"), new File(cloverReportPath)).right.value
      report.fileReports(1).coverage shouldBe Map(
        11 -> 1,
        12 -> 1,
        13 -> 1,
        16 -> 1,
        19 -> 0,
        30 -> 0,
        31 -> 0,
        32 -> 0,
        33 -> 0,
        36 -> 0,
        39 -> 0,
        42 -> 0
      )
    }
  }
}
