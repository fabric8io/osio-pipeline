import io.openshift.Events
import io.openshift.Utils

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
    def res = Utils.mergeResources(args.resources)

    def required = ['ImageStream', 'BuildConfig']
    def found = res.keySet()
    def missing = required - found
    if (missing) {
      currentBuild.result = 'FAILURE'
      error "Missing mandatory build resources params: $missing; found: $found"
      return
    }

    def namespace = args.namespace ?: Utils.usersNamespace(args.osClient)
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
        createImageStream(res.ImageStream, namespace)
        buildProject(res.BuildConfig, namespace)
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

def createImageStream(imageStreams, ns) {
    imageStreams.each { is ->
      ocApply(this, is, ns)
    }
}

def buildProject(buildConfigs, ns) {
    buildConfigs.each { buildConfig ->
      startBuild(buildConfig, ns)
      watchBuildLogs(buildConfig, ns)
    }
}

def startBuild(buildConfig, ns) {
  ocApply(this, buildConfig, ns)
  sh "oc start-build ${buildConfig.metadata.name} -n $ns"
}

def watchBuildLogs(buildConfig, ns) {
  retry(3) {
    sleep 3
    sh "oc logs bc/$buildConfig.metadata.name -f -n $ns"
  }
}
