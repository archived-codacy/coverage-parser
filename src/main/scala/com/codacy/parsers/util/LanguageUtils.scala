package com.codacy.parsers.util

import com.codacy.plugins.api.languages.Language
import com.codacy.plugins.api.languages.Languages.{Java, Python, Scala}

object LanguageUtils {

  def getExtension(language: Language): Option[String] = {
    Option(language).collect {
      case Java => ".java"
      case Python => ".py"
      case Scala => ".scala"
    }
  }

}
