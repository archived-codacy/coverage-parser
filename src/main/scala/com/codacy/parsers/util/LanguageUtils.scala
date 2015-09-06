package com.codacy.parsers.util

import com.codacy.api.Language

object LanguageUtils {

  def getExtension(language: Language.Value): String = {
    language match {
      case Language.Java => ".java"
      case Language.Python => ".py"
      case Language.Scala => ".scala"
    }
  }

}
