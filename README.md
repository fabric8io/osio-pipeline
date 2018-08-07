* Work in Progress, documentation will come in time.


***Parameter for OpenShift Template***

User can pass parameters to OpenShift template from Jenkinsfile using `templateConfig` option.
OpenShift template file is located at `.openshiftio/application.yml` in project source code.

Example

```groovy
def templateParameters = ["LABEL_NAME": "NodeApp"]

osio {
    templateConfig = templateParameters
}

```

