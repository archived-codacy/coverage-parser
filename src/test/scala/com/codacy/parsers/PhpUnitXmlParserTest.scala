package com.codacy.parsers

import java.io.File

import com.codacy.parsers.implementation.{CloverParser, PhpUnitXmlParser}
import org.scalatest.{BeforeAndAfterAll, EitherValues, Matchers, WordSpec}

class PhpUnitXmlParserTest extends WordSpec with BeforeAndAfterAll with Matchers with EitherValues {
  "PhpUnitXmlParser" should {
    "verify if report exists" in {
      val reader = PhpUnitXmlParser.parse(new File("."), new File("src/test/resources/non_existent_file.xml"))

      reader.isLeft shouldBe true
    }

    "verify if report is invalid" in {
      // use some coverage file that does not follow the PHPUnit xml format
      val reader = PhpUnitXmlParser.parse(new File("."), new File("src/test/resources/test_cobertura.xml"))

      reader.isLeft shouldBe true
    }

    "verify if report refers to existing line coverage reports" in {
      // this index contains a reference to a file which does not exist
      val reader = PhpUnitXmlParser.parse(new File("."), new File("src/test/resources/phpunitxml/incorrect_index.xml"))

      reader.isLeft shouldBe true
    }

    "verify if report is valid" in {
      val reader = PhpUnitXmlParser
        .parse(new File("/home/codacy-php/"), new File("src/test/resources/phpunitxml/index.xml"))

      reader.isRight shouldBe true
    }

    "return a valid report" in {
      val reader = PhpUnitXmlParser
        .parse(new File("/home/codacy-php/"), new File("src/test/resources/phpunitxml/index.xml"))

      val report = reader.right.value

      report.total shouldBe 69
      report.fileReports.length shouldBe 10

      val configFileReport = report.fileReports.find(_.filename.endsWith("Config.php")).get
      configFileReport.total shouldBe 86
      configFileReport.coverage shouldBe Map(24 -> 4, 25 -> 4, 26 -> 4, 27 -> 4, 28 -> 4, 29 -> 4)
      configFileReport.filename shouldBe "src/Codacy/Coverage/Config.php"

      val cloverParserFileReport = report.fileReports.find(_.filename.endsWith("CloverParser.php")).get
      cloverParserFileReport.total shouldBe 95
      cloverParserFileReport.filename shouldBe "src/Codacy/Coverage/Parser/CloverParser.php"
    }
  }
}
