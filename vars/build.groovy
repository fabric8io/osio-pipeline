import io.openshift.Events
import io.openshift.Utils

def call(Map args) {
    stage("Build application") {
      if (!args.resources) {
        error "Missing manadatory parameter: resources"
      }


      // can pass single or multiple maps
      def res = Utils.mergeMaps(args.resources)

      def required = ['ImageStream', 'BuildConfig']
      def found = res.keySet()
      def missing = required - found
      if (missing) {
        error "Missing mandatory build resources params: $missing; found: $found"
      }

      def namespace = args.namespace ?: Utils.usersNamespace()
      def image = config.runtime() ?: 'oc'

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
