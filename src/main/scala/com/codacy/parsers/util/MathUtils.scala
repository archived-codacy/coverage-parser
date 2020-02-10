package com.codacy.parsers.util

object MathUtils {

  def computePercentage(part: Int, total: Int): Int = {
    if (total == 0) 0 else math.round((part.toFloat / total) * 100)
  }
}
