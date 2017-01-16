# PlüS

[![Build Status](https://travis-ci.org/plues/plues.svg?style=flat-square)](https://travis-ci.org/plues/plues)
[![SonarQube Tech Debt](https://img.shields.io/sonar/http/sonarqube.com/plues/tech_debt.svg?maxAge=2592000&style=flat-square)](https://sonarqube.com/overview?id=plues)
[![Dependency Status](https://www.versioneye.com/user/projects/57a33b001dadcb004d680562/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/57a33b001dadcb004d680562)
[![codebeat badge](https://codebeat.co/badges/6216d53c-afad-4808-8da8-2cf748f0016d)](https://codebeat.co/projects/github-com-plues-plues)
[![Code Climate](https://codeclimate.com/github/plues/plues/badges/gpa.svg)](https://codeclimate.com/github/plues/plues)


**Current Version:** 2.0.0-RC1

This is the main application component of the
[ProB](https://www3.hhu.de/stups/prob/) based timetable validation tool
[PlÜS](https://github.com/plues) built using the [Prob2
Java API](https://www3.hhu.de/stups/prob/index.php5/ProB_Java_API).

## Configuration

The application can be configured using environment variables or a properties
file. These options can (or have to) be used during development to use local
copies of the data and B models.

### Properties

The properties are defined in two files `main.properties` and
`local.properties`. `main.properties` is always loaded before
`local.properties`.

`local.properties` can be used to set and override configuration options for
development. See
[`src/main/resources/local.properties.example`](src/main/resources/local.properties.example)
for a sample configuration file.


__modelpath__ defines the path to the B models, the default is
`src/main/resourcesmodels`.

__dbpath__ defines the path to the database used to generate the data machines.

__solver__ defines which solver to use, options are `prob` or `mock`. Setting
__solver__ to `prob` starts the default ProB 2.0 based solver and `mock`
creates a dummy solver, only useful for development and testing purposes.

__NOTE__: `local.properties` should never be under version control.


#### Environment Variables

The path to the models and to the database can also be configured using
environment variables:

__MODELPATH__ defines the path to the B models.

__DBPATH__ defines the path to the database used to generate the data machines.

## Development Setup

**Make sure to initialize the git submodules used in this project.**

To run the application you will need a copy of the
[data](https://github.com/plues/data) and models repositories.

Make sure you are in the correct branch of each repository.

* Run `make dist` in each repository to generate the files needed for the server.
* Set __dbpath/DBPATH__ to the path of the dist directory in the checkout of the data repository.
* Set __modelpath/MODELPATH__ to the path of the dist directory in the checkout of the models repository.

The application can be started either by running `./gradlew run` or using your IDE.

## License

This project is distributed under the terms of the [ISC License](LICENSE).

