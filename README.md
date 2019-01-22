# OpenShift Pipeline Library [![Build Status](https://travis-ci.org/fabric8io/osio-pipeline.svg?branch=master)](https://travis-ci.org/fabric8io/osio-pipeline)

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

* [OpenShift Pipeline Library](#overview)
	* [Overview](#overview)
	* [Prerequisites](#prerequisites)
	* [User Guide](#user-guide)
		* [Examples](#examples)
			* [Deploy stand-alone application](#deploy-stand-alone-application)
			* [Deploy stand-alone application with external configuration](#deploy-stand-alone-application-with-external-configuration)
		* [How to use this library](#how-to-use-this-library)
	* [API](#api)
		* [osio](#osio)
		* [config](#config)
		* [ci](#ci)
		* [cd](#cd)
		* [processTemplate](#processtemplate)
		* [loadResources](#loadresources)
		* [build](#build)
		* [deploy](#deploy)
		* [spawn](#spawn)
	* [Contribution Guide](#contribution-guide)
		* [Dev Setup](#dev-setup)
		* [Test](#test)

<!-- /code_chunk_output -->

## Overview

This repository provides a set of pipeline functions (pipeline library) that are used in `Jenkinsfile` to do Continuous Delivery / Continuous Integration for OpenShift.io applications.
This pipeline library can be used with any OpenShift cluster which adheres following [prerequisites](#prerequisites).

## Diagram

![OSIO-Pipeline Flow Diagram](http://www.plantuml.com/plantuml/png/hPCnRzi-4CNdlpx5GVWFxP0Ow2YuHj9aSOV6gDej2osTPCRKaI2F28oYtxtIINGjssWpT9Fe7zzxTq--2wmynyvaJddMbNQA6FBFI7jD8GSLu6LydWDFvVe9DjgXqOI2sQ2jcqEVeZOrV54T1gUg4TiEPsTQ3gdSwOQ1Gl4rad0-qX-eabMhgHKozC-OPQpPu1Zi9WK3IR0OQ5pqoYCRyan5jXIxBVeic-QxMKqFnN_JzB1HFY1CRuqsU0BBk0KajDfXjRUHOV0_sXkzzSeViapeuoLXe4K6pG4Urek7HwWiDRgDNh6sz4p05lPaN-5rfwlTb7iuLCwP0oYgfqrUbNOIVDbOubxOMF_HOxtAhKCAmmgrNGmwkXAtsuFgJBvS3FZgA2Zo1ToBdxo0tRvWnG4QRplW7S9mSSYAdJEA9Q2SZpYbuiyrIrQU1PH-GDCiwzP4N54e_lyH0_f1vUSKJkOEx55rpoG2nhRYQYExoCdeTvTG2ftOYUilsmQEwm2sgSWiaundKSiFwxJk-SyRR-jO8QmxrU9e9HbFth-fTegrJ2rDItKcjfe2lDEtM80YPGwig_3-k2vfFT6Fw8mzN7QGNpGfNy_bHdCAXIYcgg_Qp9E_KIWf-KZ_D6wlDmixjtXZrfdAREPKRorPEoG6IpCDwnMrlSgrSPUddzbzsCFtW1uQwq_rv6_jEnbtjgQ_MhSUaLRvQVWxf3fsvXS0)
[PlantUML](http://www.plantuml.com/plantuml/uml/hPCnRzi-4CNdlpx5GVWFxP0Ow2YuHj9aSOV6gDej2osTPCRKaI2F28oYtxtIINGjssWpT9Fe7zzxTq--2wmynyvaJddMbNQA6FBFI7jD8GSLu6LydWDFvVe9DjgXqOI2sQ2jcqEVeZOrV54T1gUg4TiEPsTQ3gdSwOQ1Gl4rad0-qX-eabMhgHKozC-OPQpPu1Zi9WK3IR0OQ5pqoYCRyan5jXIxBVeic-QxMKqFnN_JzB1HFY1CRuqsU0BBk0KajDfXjRUHOV0_sXkzzSeViapeuoLXe4K6pG4Urek7HwWiDRgDNh6sz4p05lPaN-5rfwlTb7iuLCwP0oYgfqrUbNOIVDbOubxOMF_HOxtAhKCAmmgrNGmwkXAtsuFgJBvS3FZgA2Zo1ToBdxo0tRvWnG4QRplW7S9mSSYAdJEA9Q2SZpYbuiyrIrQU1PH-GDCiwzP4N54e_lyH0_f1vUSKJkOEx55rpoG2nhRYQYExoCdeTvTG2ftOYUilsmQEwm2sgSWiaundKSiFwxJk-SyRR-jO8QmxrU9e9HbFth-fTegrJ2rDItKcjfe2lDEtM80YPGwig_3-k2vfFT6Fw8mzN7QGNpGfNy_bHdCAXIYcgg_Qp9E_KIWf-KZ_D6wlDmixjtXZrfdAREPKRorPEoG6IpCDwnMrlSgrSPUddzbzsCFtW1uQwq_rv6_jEnbtjgQ_MhSUaLRvQVWxf3fsvXS0)


## Prerequisites
 - OpenShift command-line interface (`oc` binary) should be available on Jenkins master or/and slave nodes.
 - Jenkins should have following set of plugins
    - [Pipeline Model Definition Plugin](https://wiki.jenkins.io/display/JENKINS/Pipeline+Model+Definition+Plugin)
    - [Kubernetes Plugin](https://wiki.jenkins.io/display/JENKINS/Kubernetes+Plugin)
 - Familarity with writing `Jenkinsfile`, basic groovy syntax and Jenkins pipeline.

## User Guide

### Examples

Following are some example `Jenkinsfiles` to illustrate how to use this pipeline library.

#### Deploy stand-alone application

The following example builds a nodejs booster and deploys it to a `stage` environment and then on approval to the `run` environment.

```groovy
@Library('github.com/fabric8io/osio-pipeline@master') _

osio {

  config runtime: 'node'

  ci {
    // runs oc process
    def resources = processTemplate()

    // performs an s2i build
    build resources: resources
  }

  cd {
    // override the RELEASE_VERSION template parameter
    def resources = processTemplate(params: [
        RELEASE_VERSION: "1.0.${env.BUILD_NUMBER}"
    ])

    build resources: resources

    deploy resources: resources, env: 'stage'

    // wait for user to approve the promotion to "run" environment
    deploy resources: resources, env: 'run', approval: 'manual'
  }
}
```

#### Deploy stand-alone application with external configuration

The following example build and deploy a nodejs application like previous one.
It also loads an external resource like `configmap` and deploy it to `stage` and `run` environments.

```groovy
@Library('github.com/fabric8io/osio-pipeline@master') _

osio {
    config runtime: 'node'

    ci {
        def app = processTemplate()
       
        build app: app
    }

    cd {
      def resources = processTemplate(params: [
        release_version: "1.0.${env.BUILD_NUMBER}"
      ])
      def cm = loadResources(file: "configmap.yaml")

      build resources: resources

      // deploy API takes multiple resources in array form
      deploy resources: [resources,  cm], env: 'stage'

      // wait for user to approve the promotion to "run" environment
      deploy resources: [resources,  cm], env: 'run', approval: 'manual'
    }
}
```

where `configmap.yaml` is

```yaml
APIVersion: v1
kind: ConfigMap
metadata:
    ...
    ...
```

`loadResources` API also supports `List` kind like following one where `configurations.yaml` is

```yaml
APIVersion: v1
kind: List
items:
  -kind: ConfigMap
    ...
  -kind: Secrete
    ...
```

### How to use this library

To use the functions in this library just add the following to the top of your `Jenkinsfile`:

```groovy
@Library('github.com/fabric8io/osio-pipeline@master') _
```
That will use the master branch (bleeding edge) of this repository.

## API

The following API's are declared in this repository. A little description and example is provided for all

### osio

This is the first functionality we use in the JenkinsFile. Everything we want to do with our pipeline, we write that in this block. This block will register all the plugins then emit an event of pipeline start and execute whatever you have specified and then emit an event of pipeline end.

```groovy
    osio {
        // your continuous integration/delivery flow.
    }
```

### config

This is the API where you provide configurations like runtime or something like global variables. This will be used further by default for spinning up pods to execute your commands or your flow.

```groovy
    config {
        runtime: 'node'
        version: '8'
    }
```

If above block is configured in your pipeline then every time the spined pod will have a container named `node` which having the environments for nodejs8. By default pod will be spined with basic utilities like `oc`, `git` etc

### ci

This is the block which will be executed for continuous integration flow. By default all branches starting with name `PR-` will go through this execution. You can override by providing a branch name in arguments

```groovy
    ci {
       // Your continuous integration flow like run tests etc.
    }
```

To override the default branch for this flow

```groovy
    ci (branch: 'test'){
       // Your continuous integration flow like run tests etc.
    }
```

**Parameters**

|      Name      |  Required  |      Default Value      |                Description                 |
|----------------|------------|-------------------------|--------------------------------------------|
|     branch     |    false   | all starting with `PR-` |  branch where you want to perform CI flow  |

### cd

This is the block which will be executed for continuous delivery flow. By default this gets executed for `master` branch. You can override by providing a branch name in arguments

```groovy
    cd {
       // Your continuous delivery flow like run tests, generate release tag etc.
    }
```

To override the default branch for this flow

```groovy
    cd (branch: 'production'){
       // Your continuous delivery flow like run tests, generate release tag etc.
    }
```

**Parameters**

|      Name      |  Required  |  Default Value |                Description                 |
|----------------|------------|----------------|--------------------------------------------|
|     branch     |    false   |     master     |  branch where you want to perform CD flow  |

### processTemplate

`processTemplate` runs `oc process` on the OpenShift template pointed by `file`
parameter and returns a representation of the resources (internals can change
in future).
All mandatory parameters required to process the template must be passed in
as `params`.

```groovy
    def resources = processTemplate(
      file: 'template.yaml',
      params: [ release_version: "1.0.${env.BUILD_NUMBER}" ]
    )
```

**Parameters**

|      Name      |  Required  |         Default Value         |                             Description                 |
|----------------|------------|-------------------------------|---------------------------------------------------------|
|      file      |   false    | .openshiftio/application.yaml |   file which you want to process as OpenShift template  |
|     params     |   false    |             null              |    a map of key value pairs of all template parameters  |

Following template parameters must be present in the template and is set to the
following values by default. You can override them by passing key value pairs in params.

|              Name           |               Default Value              |
|-----------------------------|------------------------------------------|
|         SUFFIX_NAME         |                branch name               |
|    SOURCE_REPOSITORY_URL    | output of `git config remote.origin.url` |
|    SOURCE_REPOSITORY_REF    |  output of `git rev-parse --short HEAD`  |
|       RELEASE_VERSION       |  output of `git rev-list --count HEAD`   |

NOTE : `processTemplate` API expects a `RELEASE_VERSION` parameter in OpenShift template. This parameter is used to tag an image in `build` API and then to refer the same image in `deploy` API while building and deploying an application.

### loadResources

`loadResources` returns a list of OpenShift `resources` read from a yaml file.
This API returns resources list in the form of an array where every array element is a key value pair of resource `kind` and `resource array` itself.
This API can read multiple resources separated by `---` from the yaml file.


```groovy
    def resource = loadResources(file: ".openshiftio/app.yaml")
```

**Parameters**

|      Name      |  Required  |         Default Value        |                             Description                                |
|----------------|------------|------------------------------|------------------------------------------------------------------------|
|      file      |   true     |  none           |    An relative path of resource yaml file.            |
|      validate  |   false    |  true           |    A validation for resource yaml file.               |                       

### build

This is the API which is responsible for doing s2i build, generating image and creating imagestream (if not exist)

```groovy
    build resources: resources, namespace: "test", commands: """
          npm install
          npm test
    """
```

or like

```groovy
    build resources: [
                [ BuildConfig: resources.BuildConfig],
                [ImageStream: resources.ImageStream],
        ], namespace: "test", commands: """
            npm install
            npm test
        """
```

All the commands and s2i process gets executed in a container according to the environments specified in config API otherwise default.

**Parameters**

|      Name      |  Required  |   Default Value  |                            Description                               |
|----------------|------------|------------------|----------------------------------------------------------------------|
|   resources    |    true    |       null       |  OpenShift resources at least buildConfig and imageStream resource.  |
|   namespace    |    false   |  user-namespace  |            namespace where you want to perform s2i build             |
|   commands     |    false   |       null       |            commands you want to execute before s2i build             |

### deploy

This is the API which is responsible for deploying your application to OpenShift.

```groovy
    deploy resources: resources, env: 'stage'
```

or like

```groovy
    deploy resources: resources, env: 'run', approval: 'manual', timeout: '15`
```

**Parameters**

|      Name      |  Required  |  Default Value |                                   Description                                                  |
|----------------|------------|----------------|------------------------------------------------------------------------------------------------|
|   resources    |    true    |      null      |  OpenShift resources at least deploymentConfig, service, route, tag and imageStream resource.  |
|      env       |    true    |      null      |                  environment where you want to deploy - `run` or `stage`                       |
|    approval    |    false   |      null      |            if provided `manual` then user will be asked whether to deploy or not                 |
|    timeout     |    false   |       30       |               time (in minutes) to wait for user input if approval is `manual`                  |

The route generated after above step will be added as annotation in the pipeline.

### spawn

This is an API to spawn an pod as per requirement and execute the commands in the pod.

```groovy
    spawn (image: 'oc`) {
          sh """
              oc version
          """
    }
```

or like

```groovy
    spawn image: 'oc` commands: """
              oc version
          """
```

Either one of commands or closure needs to be specified.

**Parameters**

|      Name      |  Required  |  Default Value |                       Description                              |
|----------------|------------|----------------|----------------------------------------------------------------|
|     image      |    true    |      null      |       environment you want in pod like maven, node etc.        |
|    version     |    false   |     latest     |            version of the environment you want                 |
|  checkout_scm  |    false   |      true      |    whether you want git code or not for performing commands    |
|    commands    |    false   |      null      |             commands that you want to execute                  |

NOTE: For oc image, as an optimisation, a new pod is not started instead commands and body are executed on master itself

## Contribution Guide

We love contributors. We appreciate contributions in all forms :) - reporting issues, feedback, documentation, code changes, tests.. etc. 

### Dev Setup

1. Install `maven` (v 3.0 +)
2. Clone this 
   ```
   git clone git@github.com:fabric8io/osio-pipeline.git
   ```
4. cd `osio-pipeline`
3. Import it `maven` project in your favorite IDE. We reccomond Intellije IDEA.
5. Make changes in code according to following conventions
    - `vars` -> Provides an end user pipeline API's  
    - `src`  -> Contains the the code used inside pipeline API's
    - `test` -> Contains unit tests for all source code


### Test

To run the unit tests, execute

```
mvn test
```
