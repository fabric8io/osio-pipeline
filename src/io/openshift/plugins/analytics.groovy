package io.openshift.plugins

import io.openshift.Events
import io.openshift.Utils

def register() {
    Events.on("build.pass") {
        e, build -> echo "invoking bayesian analytics"
        def image = config.runtime()
        if(!image) {
            return
        }
        try {
            retry(3) {
                def response = bayesianAnalysis(url: 'https://bayesian-link', gitUrl: build.git.url, ecosystem: image)
                if (response.success) {
                  Utils.addAnnotationToBuild(this, 'fabric8.io/bayesian.analysisUrl', response.analysisUrl)
                } else {
                  echo "Bayesian analysis failed ${response}"
                }
            }
        } catch (err) {
            echo "Unable to run Bayesian analysis: ${err}"
        }
    }
}

