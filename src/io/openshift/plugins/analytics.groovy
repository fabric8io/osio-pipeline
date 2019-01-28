package io.openshift.plugins

import io.openshift.Events
import static io.openshift.Utils.addAnnotationToBuild

def register() {
    Events.on("build.pass") {
        e, a -> echo "invoking bayesian analytics"
        def image = config.runtime()
        if(!image) {
            return
        }
        try {
            retry(3) {
                def response = bayesianAnalysis(url: 'https://bayesian-link', gitUrl: a[0].git.url, ecosystem: image)
                if (response.success) {
                  addAnnotationToBuild(this, 'fabric8.io/bayesian.analysisUrl', response.getAnalysisUrl())
                } else {
                  echo "Bayesian analysis failed ${response}"
                }
            }
        } catch (err) {
            echo "Unable to run Bayesian analysis: ${err}"
        }
    }
}

