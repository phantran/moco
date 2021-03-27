[![pipeline status](https://gitlab.infosun.fim.uni-passau.de/phan/moco/badges/master/pipeline.svg)](https://gitlab.infosun.fim.uni-passau.de/phan/moco/-/commits/master)

# MoCo

A Maven plugin written in Kotlin that supports mutation testing for Java projects

This Maven plugin was originally developed to support Gamekins which is a Jenkins plugin that uses a gamification
approach to motivate software testing activities.

### Project requirements

- Kotlin or Java
- Apache Maven 3

### Setup Project

Clone this repository and install it by using Maven install command
`mvn install`

### Usage

#### pom.xml
MoCo is available in Maven Central and it could be used easily by adding it to the pom.xml file of your project

```xml
<plugin>
    <groupId>io.moco</groupId>
    <artifactId>m0c0-maven-plugin</artifactId>
    <version>ENTER-MOCO-VERSION-HERE</version>
    <executions>
        <execution>
            <goals>
                <goal>moco</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The default Maven phase of MoCo the verify phase, if you want to change the phase that executes MoCo, just use the execution 
configuration as below

```xml

<execution>
    <goals>
        <goal>moco</goal>
    </goals>
    <phase>ENTER-PHASE-HERE</phase>
</execution>
```

#### Trigger MoCo
If MoCo is added as a dependency of your project as above, it can be triggered with 
`mvn install` or `mvn verify` (if you keep the default phase as verify). Or MoCo can be executed alone with this command
`mvn m0c0:moco`

Because MoCo uses compiled test classes and compiled source classes of your project for mutation testing, please make
sure MoCo is executed after the compile phase and test phase of Maven so that all compiled sources classes and compiled test classes
are available and updated in the build (or target) folder.

Target source classes and test classes for mutation are configurable through `codeRoot` and `testRoot` parameters.
If `codeRoot` and `testRoot` are not specified, MoCo will use the default folder path information given by Maven. You
could check the configuration section or use helpmojo goal for more details about all configurable parameters of MoCo.

#### Test Frameworks
MoCo supports TestNG and JUnit (3,4,5)

#### Report
After each execution, MoCo will produce a file named `moco.json`. The default path to this file is 
`target\moco\mutation\moco.json` (if `target` is your project output build folder).
This `moco.json` file contains information about all mutations that MoCo has collected and run tests  so far.


#### Configuration 
Details about configurable parameters of MoCo will be updated here later. For the moment, please use
the helpmojo command to learn more about it
`mvn m0c0:help -Ddetail=true`

### License

This software is licensed under the terms in the file named "LICENSE" in the root directory of this project. This
project has dependencies that are under different licenses.

### Author Information

Tran Phan

phan06@ads.uni-passau.de
