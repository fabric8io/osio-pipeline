* Work in Progress, documentation will come in time.

Support the pipeline below to which does an `s2i` build based on the openshift
template file found under `.openshiftio/application.yaml` and deploys it to the
`Stage` and the `Run` namespace.

```groovy

#!/usr/bin/groovy
@Library('github.com/sthaha/osio-pipeline@master')_ 

osio {
    config runtime: 'node'

    ci {
        def resources = processTemplate()
        build resources: resources
    }

    cd {
      def resources = processTemplate(params: [
        release_version: "1.0.${env.BUILD_NUMBER}"
      ])

      build resources: resources
      deploy resources: resources, env: 'stage'
      deploy resources: resources, env: 'run', approval: 'manual'
    }
}

```
