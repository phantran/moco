# MoCo

This is the [m0c0-maven-plugin](http://).

![example workflow](https://github.com/phantran/moco/actions/workflows/actions.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=phantran_moco&metric=alert_status)](https://sonarcloud.io/dashboard?id=phantran_moco)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.phantran/m0c0-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/io.github.phantran/m0c0-maven-plugin)


A Maven plugin written in Kotlin that supports mutation testing for Java projects.

Mutation testing is computationally expensive, and that prevents mutation testing from being applied in 
big projects and CI/CD pipelines. MoCo is a mutation testing tool that uses the bytecode manipulation approach, and it applies optimization such as Git Mode (only execute mutation tests for changed source classes) and database caching, thus mutation testing execution time in MoCo can be reduced significantly.
With the applied optimization, MoCo has good performance, and it can calculate mutation scores without re-running mutation tests for the whole project under test.

MoCo was originally developed to support Gamekins which is a Jenkins plugin that uses a gamification
approach to motivate software testing activities.

### Project requirements

- Java 8+
- Apache Maven 3

#### Test Frameworks
MoCo supports TestNG and JUnit (3, 4, 5)

### Setup Project

Clone this repository and install it by using Maven install command:

`mvn install`

While developing MoCo, a quick installation without testing and generating descriptor to test MoCo in your local repository can be done with:

` mvn install -Ddescriptor.skip -Dtest.skip`

### Usage

#### pom.xml

MoCo is available on Maven Central, and it can be used easily by adding the following information to 
pom.xml file of your project (replace MOCO-VERSION with a MoCo version, e.g. `1.0`):

- To dependencies tag
```xml
<dependency>
    <groupId>io.github.phantran</groupId>
    <artifactId>m0c0-maven-plugin</artifactId>
    <version>MOCO-VERSION</version>
</dependency>
```

- To build tag
```xml
<plugin>
    <groupId>io.github.phantran</groupId>
    <artifactId>m0c0-maven-plugin</artifactId>
    <version>MOCO-VERSION</version>
    <executions>
        <execution>
            <goals>
                <goal>moco</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The default Maven phase of MoCo is verify phase, if you want to change the phase that executes MoCo, just change the execution 
configuration as below

```xml
<execution>
    <goals>
        <goal>moco</goal>
    </goals>
    <phase>ENTER-PHASE</phase>
</execution>
```

#### Trigger MoCo
If MoCo is added as a dependency of your project as above, it can be triggered with 
`mvn verify` (if execution phase is kept as default). It can also be executed alone with this command
`mvn m0c0:moco`

Because MoCo uses compiled test classes and compiled source classes of your project for mutation testing, please make
sure MoCo is executed after the compile phase and test phase of Maven so that all compiled sources classes and compiled test classes
are available and updated in the build (or target) folder.

Target source classes and test classes for mutation are configurable through `codeRoot` and `testRoot` parameters.
If `codeRoot` and `testRoot` are not specified, MoCo will use the default folder path information given by Maven. You
could check the configuration section or use helpmojo goal for more details about all configurable parameters of MoCo.

It's highly recommended to configure your `codeRoot` and `testRoot` for MoCo mutation testing if your project is big.
It will take a long time to finish if you have a big project with hundred of source classes and test classes. 

Example: We wanted to have mutation tests only for source classes inside org/example (assume there is 
 a corresponding org/example folder in built test classes folder), then the configuration is:
```xml
<codeRoot>org/example</codeRoot>
```
```xml
<testRoot>org/example</testRoot>
```
To remedy the problem of re-running mutation tests for unchanged source classes with 
corresponding test classes, MoCo offers Git Mode. Git Mode is ON by default, it helps reduce 
execution time significantly by only considering changed classes. We can turn it off with
```xml
<gitMode>false</gitMode>
```

Mutation score is not calculated by default. You can enable it by adding this to your configuration
```xml
<enableMetrics>true</enableMetrics>
```

Mutation testing is computationally expensive even with the bytecode manipulation approach. 
A big project with hundred of tests can take hours to finish. To speed it up you can use more worker threads. 
MoCo uses 2 threads by default.
Example: Using 3 threads.
```xml
<numberOfThreads>3</numberOfThreads>
```


Below is an example configuration that uses MoCo version 1.0, Git Mode ON, mutation score calculation enabled, 
debug messages logging enabled, test timeout in preprocessing phase (collecting mutations) as 500ms, 
and use 3 threads for parallel execution:

```xml
<plugin>
    <groupId>io.github.phantran</groupId>
    <artifactId>m0c0-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <gitMode>true</gitMode>
        <debugEnabled>true</debugEnabled>
        <enableMetrics>true</enableMetrics>
        <preprocessTestTimeout>500</preprocessTestTimeout>
        <numberOfThreads>3</numberOfThreads>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>moco</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Report
After each execution, MoCo will produce a file named `moco.json`. The default path to this file is 
`**\moco\mutation\moco.json` (inside your project output build folder).
This `moco.json` file contains information about all mutations that MoCo has collected and executed so far.


#### Configuration 
Details about more configurable parameters of MoCo will be updated here later. For the moment, you could use
the helpmojo command to learn more about it.

`mvn m0c0:help -Ddetail=true`

### Contributing
If you find a problem with MoCo and wanted to fix it, it would be very helpful to create a ticket along with your pull request.

### License

This software is licensed under the terms in the file named "LICENSE" in the root directory of this project. This
project has dependencies that are under different licenses.

### Author Information

#### Tran Phan
phantran197@gmail.com

This project was developed as a part of my work at the
[Chair of Software Engineering II](https://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/),
University of Passau.




