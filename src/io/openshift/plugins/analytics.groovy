package io.openshift.plugins

import io.openshift.Events
import io.openshift.Utils

def register() {
    Events.on("build.pass") {
        e, a -> echo "invoking bayesian analytics $e $a"
    }
}

