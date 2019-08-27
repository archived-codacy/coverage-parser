package com.codacy.parsers.implementation

import com.codacy.parsers.CoverageParser
import com.codacy.api.{CoverageFileReport, CoverageReport}
import java.io.File

import scala.io.Source
import scala.util.{Failure, Success, Try}

object LCOVParser extends CoverageParser {
  override val name: String = "LCOV"

  final val SF = "SF:"
  final val DA = "DA:"

  def parse(rootProject: File, reportFile: File): Either[String, CoverageReport] = {
    val report = Try(Source.fromFile(reportFile)) match {
      case Success(lines) =>
        Right(lines.getLines)

      case Failure(ex) =>
        Left(s"Can't load report file. ${ex.getMessage}")
    }

    report.right.flatMap(parse(reportFile, _))
  }

  private def parse(reportFile: File, lines: Iterator[String]): Either[String, CoverageReport] = {
    val coverageFileReports =
      lines.foldLeft[Either[String, Seq[CoverageFileReport]]](Right(Seq.empty[CoverageFileReport]))(
        (accum, next) =>
          accum.right.flatMap {
            case reports if next startsWith SF =>
              Right(CoverageFileReport(next stripPrefix SF, 0, Map()) +: reports)
            case reports if next startsWith DA =>
              reports.headOption match {
                case Some(value) =>
                  val coverage = next.stripPrefix(DA).split(",")
                  if (coverage.length >= 2 && coverage.forall(_ forall Character.isDigit)) {
                    val coverageValue = coverage.map(_.toInt)
                    Right(
                      value.copy(coverage = value.coverage + (coverageValue(0) -> coverageValue(1))) +: reports.tail
                    )
                  } else Left(s"Misformatting of file ${reportFile.toString()}")
                case _ => Left(s"Fail to parse ${reportFile.toString()}")
              }
            case reports =>
              val res = Right(reports)
              res
        }
      )
    coverageFileReports.right.map { fileReports =>
      val totalFileReport = fileReports.map { report =>
        CoverageFileReport(
          report.filename,
          (if (report.coverage.size != 0)
             (report.coverage.count { case (line, hit) => hit > 0 }.toFloat / report.coverage.size) * 100
           else 0f).round.toInt,
          report.coverage
        )
      }

      val (covered, total) = totalFileReport
        .map { f =>
          (f.coverage.count { case (line, hit) => hit > 0 }, f.coverage.size)
        }
        .foldLeft(0 -> 0) {
          case ((accumCovered, accumTotal), (nextCovered, nextTotal)) =>
            (accumCovered + nextCovered, accumTotal + nextTotal)
        }
      CoverageReport((if (total != 0) ((covered.toFloat / total) * 100) else 0f).round.toInt, totalFileReport)
    }
  }
}
