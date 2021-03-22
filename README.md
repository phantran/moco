[![pipeline status](https://gitlab.infosun.fim.uni-passau.de/phan/moco/badges/master/pipeline.svg)](https://gitlab.infosun.fim.uni-passau.de/phan/moco/-/commits/master)

# MoCo

A Maven plugin written in Kotlin that supports mutation testing for Java projects

This Maven plugin was originally developed to support Gamekins which is a Jenkins plugin that uses a gamification approach
to motivate software testing activities.

### Project requirements
- Kotlin or Java
- Apache Maven

### Setup Project
Clone this repository and install its dependencies using Maven
`mvn install`

### Usage
MoCo is available in Maven Central and it could be used easily by adding it to the pom.xml file of your project

Because MoCo uses compiled test classes and compiled source classes of your project for mutation testing,
please make sure MoCo is executed after compile phase and test phase so that all compiled sources
classes and compiled test classes are available and updated in the build (or target) folder.

Target source classes and test classes for mutation are configurable through `codeRoot` and `testRoot` parameters.
If codeRoot and testRoot are not specified, MoCo will use the default folder path information given by Maven.
You could check the configuration section or use helpmojo goal for more details about all
configurable parameters of MoCo.


### License

This software is licensed under the terms in the file named "LICENSE" in the root directory of this project. This project has dependencies that are under different licenses.

### Author Information

Tran Phan

phan06@ads.uni-passau.de
