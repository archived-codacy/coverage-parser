# coverage-parser

[![Circle CI](https://circleci.com/gh/codacy/coverage-parser/tree/master.svg?style=shield)](https://circleci.com/gh/codacy/coverage-parser/tree/master)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/c3e4c4fc9dd047f1b55076b8a83e9b3d)](https://www.codacy.com/app/Codacy/coverage-parser)
[![Codacy Badge](https://api.codacy.com/project/badge/coverage/c3e4c4fc9dd047f1b55076b8a83e9b3d)](https://www.codacy.com/app/Codacy/coverage-parser)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.codacy/coverage-parser_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.codacy/coverage-parser_2.11)

Scala library for parsing coverage reports.

Currently we support [Jacoco](http://eclemma.org/jacoco/) and [Cobertura](http://cobertura.github.io/cobertura/) reports.

Both [CoberturaParser](https://github.com/codacy/coverage-parser/blob/master/src/main/scala/com/codacy/parsers/implementation/CoberturaParser.scala) and [JacocoParser](https://github.com/codacy/coverage-parser/blob/master/src/main/scala/com/codacy/parsers/implementation/JacocoParser.scala) receive the language, the project root, and the file containing the coverage report, producing the [Codacy coverage format](http://docs.codacy.com/docs/coverage#api)

Usage:

```
val reader = new CoberturaParser(Language.Scala, rootProjectDir, coberturaFile)
val report = reader.generateReport()
```
