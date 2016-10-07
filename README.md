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

## What is Codacy?

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features:

 - Identify new Static Analysis issues
 - Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
 - Auto-comments on Commits and Pull Requests
 - Integrations with Slack, HipChat, Jira, YouTrack
 - Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
