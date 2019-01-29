import io.openshift.Events

import static io.openshift.Utils.mergeResources
import static io.openshift.Utils.usersNamespace
import static io.openshift.Utils.ocApply
import static io.openshift.Utils.shWithOutput

def call(Map args) {
  stage("Build Image") {
    if (!args.resources) {
      currentBuild.result = 'FAILURE'
      error "Missing manadatory parameter: resources"
      return
    }

    // can pass single or multiple maps
    def res = mergeResources(args.resources)

    def required = ['ImageStream', 'BuildConfig']
    def found = res.keySet()
    def missing = required - found
    if (missing) {
      currentBuild.result = 'FAILURE'
      error "Missing mandatory build resources params: $missing; found: $found"
      return
    }

    def namespace = args.namespace ?: usersNamespace(args.osClient)
    def image = args.image
    if (!image) {
      image = args.commands ? config.runtime() : 'oc'
    }
    def gitURL = shWithOutput(this, "git config remote.origin.url")
    def commitHash = shWithOutput(this, "git rev-parse --short HEAD")
    def status = ""
    spawn(image: image, version: config.version(), commands: args.commands) {
      Events.emit("build.start")
      try {
        createImageStream namespace, res.ImageStream
        buildProject namespace, res.BuildConfig
        status = "pass"
      } catch (e) {
        status = "fail"
      } finally {
        Events.emit(["build.end", "build.${status}"],
                    [status: status, namespace: namespace, git: [url: gitURL, commit: commitHash]])
      }

      if (status == 'fail') {
        error "Build failed"
      }
    }
  }
}

def createImageStream(ns, imageStreams) {
  imageStreams.each { imageStream ->
    def isName = imageStream.metadata.name
    def isFound = shWithOutput(this, "oc get is/$isName -n $ns --ignore-not-found")
    if (!isFound) {
      ocApply this, imageStream, ns
    } else {
      echo "image stream exist ${isName}"
    }
  }
}

def buildProject(ns, buildConfigs) {
    buildConfigs.each { bc ->
      ocApply this, bc, ns
      sh "oc start-build ${bc.metadata.name} -n $ns -F"
    }
}
