# MoCo

This is the [m0c0-maven-plugin](http://).

![example workflow](https://github.com/phantran/moco/actions/workflows/actions.yml/badge.svg)

A Maven plugin written in Kotlin that supports mutation testing for Java projects

This Maven plugin was originally developed to support Gamekins which is a Jenkins plugin that uses a gamification
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

Since MoCo is not yet published to Maven Central, you need to install MoCo to your local repository to use it.
Just follow the setup instructions above to install MoCo to your local repository, 
then MoCo could be used easily by adding the following information to pom.xml file of your project:

- To dependencies tag
```xml
<dependency>
    <groupId>io.moco</groupId>
    <artifactId>m0c0-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

- To build tag
```xml
<plugin>
    <groupId>io.moco</groupId>
    <artifactId>m0c0-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
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
    <phase>[ENTER-PHASE-HERE]</phase>
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
To remedy the problem of rerunning mutation tests for unchanged source classes with 
corresponding test classes, MoCo offers Gitmode. Gitmode is ON by default, it helps reduce 
execution time significantly by only considering changed classes. You can turn it off with
```xml
<gitMode>false</gitMode>
```

Mutation score is currently not calculated by default. You can enable it by adding this to your configuration
```xml
<enableMetrics>true</enableMetrics>
```

Mutation testing is computationally expensive even with the bytecode manipulation approach. 
A big project with hundred of tests can take hours to finish. To speed it up you can use more worker threads.
Example: Using 3 threads.
```xml
<numberOfThreads>3</numberOfThreads>
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

This project was developed as a part of my work at
[Chair of Software Engineering II](https://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/),
University of Passau.




