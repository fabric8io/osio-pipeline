# OSIO Pipeline

This git repository contains functions that are used in `Jenkinsfile` to do Continuous Delivery / Continuous Integration for openshift.io.

## Examples

### Deploy stand-alone application

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

### Deploy stand-alone application with external configuration

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
apiVersion: v1
kind: ConfigMap
metadtaa:
    ...
    ...
```

`loadResources` API also supports `List` kind like following one

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
      def configurations = loadResources(file: "configurations.yaml")

      build resources: resources

      // deploy API takes multiple resources in array form
      deploy resources: [resources, configurations], env: 'stage'

      // wait for user to approve the promotion to "run" environment
      deploy resources: [resources, configurations], env: 'run', approval: 'manual'
    }
}
```

where `configurations.yaml` is

```yaml
apiVersion: v1
kind: List
items:
  -kind: ConfigMap
    ...
  -kind: Secrete
    ...
```

## How to use this library

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

This is the api where you provide configurations like runtime or something like global variables. This will be used further by default for spining up pods to execute your commands or your flow.

```groovy
    config {
        runtime: 'node'
        version: '8'
    }
```

If above block is configured in your pipeline then everytime the spinned pod will have a container named `node` which having the environments for nodejs8. By default pod will be spinned with basic utilities like `oc`, `git` etc

### ci

This is the block which will be executed for continuous integration flow. By default all branches starting with name `PR-` will go through this execution. You can overide by providing a branch name in arguments

```groovy
    ci {
       // Your continuous integration flow like run tests etc.
    }
```

To overide the default branch for this flow

```groovy
    ci (branch: 'test'){
       // Your continuous integration flow like run tests etc.
    }
```

Parameters

|      Name      |  Required  |      Default Value      |                Description                 |
|----------------|------------|-------------------------|--------------------------------------------|
|     branch     |    false   | all starting with `PR-` |  branch where you want to perform CI flow  |

### cd

This is the block which will be executed for continuous delivery flow. By default this gets executed for `master` branch. You can overide by providing a branch name in arguments

```groovy
    cd {
       // Your continuous delivery flow like run tests, generate release tag etc.
    }
```

To overide the default branch for this flow

```groovy
    cd (branch: 'production'){
       // Your continuous delivery flow like run tests, generate release tag etc.
    }
```

Parameters

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

Parameters

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

### loadResources

`loadResources` returns a list of OpenShift `resources` read from a yaml file.
This API returns resources list in the form of an array where every array element is a key value pair of resource `kind` and `resource array` itself.
This API can read multiple resources seperated by `---` from the yaml file.


```groovy
    def resource = loadResources(file: ".openshiftio/app.yaml")
```

Parameters

|      Name      |  Required  |         Default Value        |                             Description                                |
|----------------|------------|------------------------------|------------------------------------------------------------------------|
|      file      |   true     |  none           |    An relative path of resource yaml file.            |
|      validate  |   false    |  true           |    A validation for resource yaml file.               |                       
### build

This is the api which is responsible for doing s2i build, generating image and creating imagestream (if not exist)

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

All the commands and s2i process gets executed in a container according to the environments specified in config api otherwise default.

Parameters

|      Name      |  Required  |   Default Value  |                            Description                               |
|----------------|------------|------------------|----------------------------------------------------------------------|
|   resources    |    true    |       null       |  openshift resources at least buildConfig and imageStream resource.  |
|   namespace    |    false   |  user-namespace  |            namespace where you want to perform s2i build             |
|   commands     |    false   |       null       |            commands you want to execute before s2i build             |

### deploy

This is the api which is responsible for deploying your application to openshift.

```groovy
    deploy resources: resources, env: 'stage'
```

or like

```groovy
#!/usr/bin/groovy
    deploy resources: resources, env: 'run', approval: 'manual', timeout: '15`
```

Parameters

|      Name      |  Required  |  Default Value |                                   Description                                                  |
|----------------|------------|----------------|------------------------------------------------------------------------------------------------|
|   resources    |    true    |      null      |  openshift resources at least deploymentConfig, service, route, tag and imageStream resource.  |
|      env       |    true    |      null      |                  environment where you want to deploy - `run` or `stage`                       |
|    approval    |    false   |      null      |            if provided `manual` then user will be asked whether to deploy or not                 |
|    timeout     |    false   |       30       |               time (in minutes) to wait for user input if approval is `manual`                  |

The route generated after above step will be added as annotation in the pipeline.

### spawn

This is an api to spawn an pod as per requirement and execute the commands in the pod.

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

Parameters

|      Name      |  Required  |  Default Value |                       Description                              |
|----------------|------------|----------------|----------------------------------------------------------------|
|     image      |    true    |      null      |       environment you want in pod like maven, node etc.        |
|    version     |    false   |     latest     |            version of the environment you want                 |
|  checkout_scm  |    false   |      true      |    whether you want git code or not for performing commands    |
|    commands    |    false   |      null      |             commands that you want to execute                  |

NOTE: For oc image, as an optimisation, a new pod is not started instead commands and body are executed on master itself
