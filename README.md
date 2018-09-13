* Work in Progress, documentation will come in time.

Support the pipeline below to which does an `s2i` build based on the openshift
template file found under `.openshiftio/application.yaml` and deploys it to the
`Stage` and the `Run` namespace.

```groovy

#!/usr/bin/groovy
@Library('github.com/sthaha/osio-pipeline@develop')_

osio {
    config runtime: 'node'

    ci {
        def app = processTemplate()
        build app: app
    }

    cd {
      def app = processTemplate(params: [
        release_version: "1.0.${env.BUILD_NUMBER}"
      ])

      build app: app
      deploy app: app, env: 'stage'
      deploy app: app, env: 'run', approval: 'manual'
    }
}

```
