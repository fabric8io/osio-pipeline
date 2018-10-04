package io.openshift.plugins

import io.openshift.Events
import io.openshift.Utils

def register() {
    Events.on("build.pass") {
        e, a -> echo "invoking bayesian analytics $e $a"
        def image = config.runtime() ?: ''
        if(image != "") {
            def url = Utils.shWithOutput(this, "git config remote.origin.url")
            echo "URL:$url"
            echo "Image:$image"
            try {
                retry(3) {
                    def response = bayesianAnalysis url: 'https://bayesian-link', gitUrl: url, ecosystem: image
                    if (response.success) {
                      Utils.addAnnotationToBuild(this, 'fabric8.io/bayesian.analysisUrl', response.getAnalysisUrl())
                    } else {
                      echo "Bayesian analysis failed ${response}"
                    }
                }
            } catch (err) {
                echo "Unable to run Bayesian analysis: ${err}"
            }
        }
    }
}

