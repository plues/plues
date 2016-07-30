# PlüS

[![Build Status](https://travis-ci.org/plues/plues.svg)](https://travis-ci.org/plues/plues)

**Current Version:** 2.0.0-SNAPSHOT

This is the main application component of the
[ProB](https://www3.hhu.de/stups/prob/) based timetable validation tool
[PlÜS](https://github.com/plues) built using the [Prob2
Java API](https://www3.hhu.de/stups/prob/index.php5/ProB_Java_API).

## Configuration

The application can be configured using environment variables or a properties
file.
These options can (or have to) be used during development to use local copies
of the data and B models.

### Environment Variables

__MODELPATH__ defines the path to the B models.

__DBPATH__ defines the path to the database used to generate the data machines.

### Properties

The server can also be configured using a properties file located in
`src/main/resources/local.properties`. See
[`src/main/resources/local.properties.example`](src/main/resources/local.properties.example) for a sample configuration file.

__modelpath__ defines the path to the B models, the default is
`src/main/resourcesmodels`.

__dbpath__ defines the path to the database used to generate the data machines.

## Development Setup

Make sure to initialize the git submodules used in this project.

To run the applcation you will need a copy of the
[data](https://github.com/plues/data) and models repositories.

Make sure you are in the correct branch of each repository.

* Run `make dist` in each repository to generate the files needed for the server.
* Set __dbpath/DBPATH__ to the path of the dist directory in the checkout of the data repository.
* Set __modelpath/MODELPATH__ to the path of the dist directory in the checkout of the models repository.

The server can be started either using your IDE, or by running `./gradlew run`.

## License

This project is distributed under the terms of the [ISC License](LICENSE).
