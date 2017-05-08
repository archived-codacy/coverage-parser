package com.codacy.parsers

import java.io.File

import com.codacy.api._
import com.codacy.parsers.implementation.JacocoParser
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class JacocoParserTest extends WordSpec with BeforeAndAfterAll with Matchers {

  "JacocoParser" should {

    "identify if report is invalid" in {
      val reader = new JacocoParser(Language.Java, new File("."), new File("src/test/resources/test_cobertura.xml"))

      reader.isValidReport shouldBe false
    }

    "identify if report is valid" in {
      val reader = new JacocoParser(Language.Java, new File("."), new File("src/test/resources/test_jacoco.xml"))

      reader.isValidReport shouldBe true
    }

    "return a valid report" in {
      val reader = new JacocoParser(Language.Java, new File("."), new File("src/test/resources/test_jacoco.xml"))

      val testReport = CoverageReport(73, List(
        CoverageFileReport("org/eluder/coverage/sample/InnerClassCoverage.java", 81,
          Map(10 -> 1, 6 -> 1, 9 -> 1, 13 -> 1, 22 -> 1, 12 -> 1, 3 -> 1, 16 -> 1, 19 -> 1)),
        CoverageFileReport("org/eluder/coverage/sample/SimpleCoverage.java", 50,
          Map(3 -> 1, 6 -> 1))))

      reader.generateReport() shouldEqual testReport
    }

  }

}
