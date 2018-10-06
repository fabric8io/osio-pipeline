import io.openshift.Events
import io.openshift.Utils

def call(Map args) {
  stage("Build application") {
    if (!args.resources) {
      error "Missing manadatory parameter: resources"
    }


    // can pass single or multiple maps
    def res = Utils.mergeResources(args.resources)

    def required = ['ImageStream', 'BuildConfig']
    def found = res.keySet()
    def missing = required - found
    if (missing) {
      error "Missing mandatory build resources params: $missing; found: $found"
    }

    def namespace = args.namespace ?: Utils.usersNamespace()
    def image = args.image
    if (!image) {
      image = args.commands ? config.runtime() : 'oc'
    }
    def gitURL = Utils.shWithOutput(this, "git config remote.origin.url")
    def commitHash = Utils.shWithOutput(this, "git rev-parse --short HEAD")
    def status = ""
    spawn(image: image, version: config.version(), commands: args.commands) {
      Events.emit("build.start")
      try {
        echo "${res}"
        createImageStream(res.ImageStream, namespace)
        buildProject(res.BuildConfig, namespace)
        status = "pass"
      } catch (e) {
        echo "error : ${e}"
        status = "fail"
      } finally {
        Events.emit(["build.end", "build.${status}"],
                    [status: status, namespace: namespace, gitURL: gitURL, commitHash: commitHash])
      }

      if (status == 'fail') {
        error "Build failed"
      }
    }
  }
}

def createImageStream(imageStreams, namespace) {
    imageStreams.each { imageStream ->
        def isName = imageStream.metadata.name
        def isFound = Utils.shWithOutput(this, "oc get is/$isName -n $namespace --ignore-not-found")
        if (!isFound) {
          Utils.ocApply(this, imageStream, namespace)
        } else {
          echo "image stream exist ${isName}"
        }
    }
}

def buildProject(buildConfigs, namespace) {
    buildConfigs.each { buildConfig ->
        Utils.ocApply(this, buildConfig, namespace)
        openshiftBuild(buildConfig: "${buildConfig.metadata.name}", showBuildLogs: 'true')
    }
}
