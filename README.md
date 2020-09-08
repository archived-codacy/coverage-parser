# coverage-parser

[![Circle CI](https://circleci.com/gh/codacy/coverage-parser/tree/master.svg?style=shield)](https://circleci.com/gh/codacy/coverage-parser/tree/master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cba8fd0874ac4f569f4f880e473cbac9)](https://www.codacy.com/gh/codacy/coverage-parser?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/coverage-parser&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/cba8fd0874ac4f569f4f880e473cbac9)](https://www.codacy.com/gh/codacy/coverage-parser?utm_source=github.com&utm_medium=referral&utm_content=codacy/coverage-parser&utm_campaign=Badge_Coverage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.codacy/coverage-parser_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.codacy/coverage-parser_2.11)

Scala library for parsing coverage reports.

The parsers in this project receive as input two parameters, the project root folder, and the coverage report file.
As a result of the parse operation, it will produce either a CoverageReport, a class that is compatible with the
[Codacy coverage format](https://support.codacy.com/hc/en-us/articles/207279819-Coverage) or a string describing the
error parsing the coverage report file.

## Supported formats

Check the formats we support [on the documentation](https://docs.codacy.com/coverage-reporter/adding-coverage-to-your-repository/). You can use this parser with any of the listed coverage formats, independently of your language or coverage tool of choice.

If your coverage reports are in a different format you can [use a format converter to generate a supported format](https://docs.codacy.com/coverage-reporter/troubleshooting-common-issues/#unsupported-report-formats).

Usage:

```
val reader: Either[String, CoverageReport] = CoverageParser.parse(rootProjectDir, coberturaFile)
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
