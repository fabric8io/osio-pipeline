import io.openshift.Events
import io.openshift.Utils

def call(Map args) {
    stage("Build application") {
      def namespace = args.namespace ?: Utils.usersNamespace()
      def image = config.runtime() ?: 'oc'
      def res = args.resources
      if (!res) {
        error "Missing manadatory parameter: resources"
        currentBuild.result = 'ABORTED'
      }

      if (!res.ImageStream || !res.BuildConfig) {
        def found = res.keySet().join(', ')
        error "parameter resources must contain ImageStream and BuildConfig but found: $found"
        currentBuild.result = 'ABORTED'
        return
      }

      def status = ""
      spawn(image: image, version: config.version(), commands: args.commands) {
        Events.emit("build.start")
        try {
          createImageStream(res.ImageStream, namespace)
          buildProject(res.BuildConfig, namespace)
          status = "pass"
        } catch (e) {
          status = "fail"
          echo "build failed"
          throw e
        } finally {
          Events.emit(["build.end", "build.${status}"], [status: status, namespace: namespace])
        }
      }
    }
}

def createImageStream(imageStream, namespace) {
    def isName = imageStream.metadata.name
    def isFound = Utils.shWithOutput(this, "oc get is/$isName -n $namespace --ignore-not-found")
    if (!isFound) {
        Utils.ocApply(this, imageStream, namespace)
    } else {
        echo "image stream exist ${isName}"
    }
}

def buildProject(buildConfig, namespace) {
    Utils.ocApply(this, buildConfig, namespace)
    openshiftBuild(buildConfig: "${buildConfig.metadata.name}", showBuildLogs: 'true')
}
