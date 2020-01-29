package com.codacy.parsers

import java.io.File

import com.codacy.parsers.implementation.PhpUnitXmlParser
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

    "verify if report is invalid if it refers to non-existing line coverage reports" in {
      // this index contains a reference to a file that includes references to inexsistent files
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

      report.fileReports.find(_.filename.endsWith("Config.php")) match {
        case None => fail("Config.php file is not present in the list of file reports")
        case Some(fileReport) =>
          fileReport.total shouldBe 86
          fileReport.coverage shouldBe Map(24 -> 4, 25 -> 4, 26 -> 4, 27 -> 4, 28 -> 4, 29 -> 4)
          fileReport.filename shouldBe "src/Codacy/Coverage/Config.php"
      }

      report.fileReports.find(_.filename.endsWith("CloverParser.php")) match {
        case None => fail("CloverParser.php is not present in the list of file reports")
        case Some(fileReport) =>
          fileReport.total shouldBe 95
          fileReport.filename shouldBe "src/Codacy/Coverage/Parser/CloverParser.php"
      }
    }
  }
}
