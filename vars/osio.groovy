#!/usr/bin/groovy

import io.openshift.Events
import io.openshift.Plugins

def call(body) {
    node {
        // TODO: move registration to a different file; perhaps
        Plugins.register()
        Events.emit("pipeline.start", "testarg")
        checkout scm
        body()
        Events.emit("pipeline.end", "testarg")
    }
}
