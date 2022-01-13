# github workflows generator
Reducing Boilerplate Code with github workflows maven plugin
> More Time for Feature and functionality
  Through a simple set of github workflows templates and saving 60% of development time 

## Key Features
* Auto generate by maven compile phase
* Auto JUnit Tests detector by adding "JUnit Tests" stage
* Auto Integration Tests detector by adding "Integration Tests" stage
* Auto Dockerfile detector by adding "Build Docker" stage
* Auto Maven artifact detector by adding "Deploy Maven Artifact" stage
* Auto Sonar report detector by adding "Sonar Report" stage
* Auto Deployment to Cloud Platform by adding "Deployment" stage


## How to use

```
<plugin>
    <groupId>de.microtema</groupId>
    <artifactId>github-workflows-maven-plugin</artifactId>
    <version>2.0.1-SNAPSHOT</version>
    <configuration>
        <variables>
          <DOCKER_REGISTRY>docker.registry.local</DOCKER_REGISTRY>
        </variables>
    </configuration>
    <executions>
        <execution>
            <id>github-workflows</id>
            <phase>compile</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Output 
> .github/workflows/master.yml 
> NOTE: This is an example file.

```
name: Master Ci Pipeline

on:
  push:
    branches:
      - master
  pull_request:
    branches:
      - master

jobs:
  versioning:
    runs-on: ubuntu-latest
    steps:
    - run: echo versioning
  compile:
    needs:
    - versioning
    runs-on: ubuntu-latest
    steps:
    - run: echo compile
  security-check:
    needs:
    - compile
    runs-on: ubuntu-latest
    steps:
    - run: echo security-check
  unit-test:
    needs:
    - compile
    runs-on: ubuntu-latest
    steps:
    - run: echo unit-test
  acceptance-test:
    needs:
    - compile
    runs-on: ubuntu-latest
    steps:
    - run: echo acceptance-test
  quality-gate:
    needs:
    - unit-test
    - acceptance-test
    runs-on: ubuntu-latest
    steps:
    - run: echo quality-gate
  build:
    needs:
    - quality-gate
    runs-on: ubuntu-latest
    steps:
    - run: echo package
  package:
    needs:
    - build
    runs-on: ubuntu-latest
    steps:
    - run: echo package
  promote:
    needs:
    - package
    runs-on: ubuntu-latest
    steps:
    - run: echo package
  deployment:
    needs:
    - promote
    runs-on: ubuntu-latest
    steps:
    - run: echo deployment
  readyness-check:
    needs:
    - deployment
    runs-on: ubuntu-latest
    steps:
    - run: echo readyness-check
```
    
## Technology Stack

* Java 1.8
    * Streams 
    * Lambdas
* Third Party Libraries
    * Commons-BeanUtils (Apache License)
    * Commons-IO (Apache License)
    * Commons-Lang3 (Apache License)
    * Junit (EPL 1.0 License)
* Code-Analyses
    * Sonar
    * Jacoco
    
## Test Coverage threshold
> 95%
    
## License

MIT (unless noted otherwise)

## Quality Gate Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=github-workflows-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mtema_github-workflows-maven-plugin)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=github-workflows-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=mtema_github-workflows-maven-plugin)

[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=github-workflows-maven-plugin&metric=sqale_index)](https://sonarcloud.io/dashboard?id=mtema_github-workflows-maven-plugin)
