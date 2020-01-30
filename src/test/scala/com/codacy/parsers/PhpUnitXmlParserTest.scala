package com.codacy.parsers

import java.io.File

import com.codacy.parsers.implementation.PhpUnitXmlParser
import org.scalatest.{BeforeAndAfterAll, EitherValues, Matchers, WordSpec}

class PhpUnitXmlParserTest extends WordSpec with BeforeAndAfterAll with Matchers with EitherValues {
  private val root_path = "/home/codacy-php/"
  private val valid_report = "src/test/resources/phpunitxml/index.xml"
  private val incorrect_report = "src/test/resources/phpunitxml/incorrect_index.xml"
  private val cobertura_report = "src/test/resources/test_cobertura.xml"
  private val non_existent_report = "src/test/resources/non_existent_file.xml"
  private val config_php_file = "Config.php"

  "parse" should {
    "return an invalid report" when {
      "report file does not exist" in {
        val reader = PhpUnitXmlParser.parse(new File("."), new File(non_existent_report))

        reader shouldBe 'left
      }

      "report file has a different format" in {
        // use some coverage file that does not follow the PHPUnit xml format
        val reader = PhpUnitXmlParser.parse(new File("."), new File(cobertura_report))

        reader shouldBe 'left
      }

      "report refers to non-existent file coverage report" in {
        // this index contains a reference to a file that includes references to non-existent files
        val reader =
          PhpUnitXmlParser.parse(new File("."), new File(incorrect_report))

        reader.isLeft shouldBe true
      }
    }

    "verify if report is valid" in {
      val reader = PhpUnitXmlParser
        .parse(new File(root_path), new File(valid_report))

      reader shouldBe 'right
    }

    "return a report with the expected total coverage" in {
      val report = PhpUnitXmlParser
        .parse(new File(root_path), new File(valid_report))
        .right
        .value

      report.total shouldBe 69
    }

    "return a report with the expected number of files" in {
      val report = PhpUnitXmlParser
        .parse(new File(root_path), new File(valid_report))
        .right
        .value

      report.fileReports.length shouldBe 10
    }

    "return a report with the expected file names" in {
      val report = PhpUnitXmlParser
        .parse(new File(root_path), new File(valid_report))
        .right
        .value

      report.fileReports.map(_.filename).sorted shouldBe Seq(
        "src/Codacy/Coverage/Api/Api.php",
        "src/Codacy/Coverage/CodacyPhpCoverage.php",
        "src/Codacy/Coverage/Config.php",
        "src/Codacy/Coverage/Git/GitClient.php",
        "src/Codacy/Coverage/Parser/CloverParser.php",
        "src/Codacy/Coverage/Parser/Parser.php",
        "src/Codacy/Coverage/Parser/PhpUnitXmlParser.php",
        "src/Codacy/Coverage/Report/CoverageReport.php",
        "src/Codacy/Coverage/Report/FileReport.php",
        "src/Codacy/Coverage/Report/JsonProducer.php"
      ).sorted
    }

    "return a report with the expected file coverage" in {
      val report = PhpUnitXmlParser
        .parse(new File(root_path), new File(valid_report))
        .right
        .value

      report.fileReports.find(_.filename.endsWith(config_php_file)) match {
        case None => fail(config_php_file + " file is not present in the list of file reports")
        case Some(fileReport) =>
          fileReport.total shouldBe 86
      }

      report.fileReports.find(_.filename.endsWith("CloverParser.php")) match {
        case None => fail("CloverParser.php is not present in the list of file reports")
        case Some(fileReport) =>
          fileReport.total shouldBe 95
      }
    }

    "return a report with the expected line coverage" in {
      val report = PhpUnitXmlParser
        .parse(new File(root_path), new File(valid_report))
        .right
        .value

      report.fileReports.find(_.filename.endsWith(config_php_file)) match {
        case None => fail(config_php_file + " file is not present in the list of file reports")
        case Some(fileReport) =>
          fileReport.coverage shouldBe Map(24 -> 4, 25 -> 4, 26 -> 4, 27 -> 4, 28 -> 4, 29 -> 4)
      }
    }
  }
}
