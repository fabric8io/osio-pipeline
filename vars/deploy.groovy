import io.openshift.Utils;

def call(Map args = [:]) {
  stage ("Deploy to ${args.env}") {
    if (!args.env) {
      error "Missing manadatory parameter: env"
      currentBuild.result = 'ABORTED'
    }

    if (!args.resources) {
      error "Missing manadatory parameter: resources"
      currentBuild.result = 'ABORTED'
    }

    def required = ['ImageStream', 'DeploymentConfig', 'Service', 'Route', 'tag']
    // can pass single or multiple maps
    def res = Utils.mergeMaps(args.resources)
    def found = res.keySet()
    def missing = required - found
    if (missing) {
      error "Missing mandatory build resources params: $missing; found: $found"
    }


    if (args.approval == 'manual') {
      askForInput(res.tag, args.env, args.timeout?: 30)
    }

    spawn(image: "oc") {
      def userNS = Utils.usersNamespace();
      def deployNS = userNS + "-" + args.env;

      tagImageToDeployEnv(deployNS, userNS, res.ImageStream, res.tag)
      def routeUrl = deployEnvironment(deployNS, res.DeploymentConfig, res.Service, res.Route)
      displayRouteURLOnUI(deployNS, args.env, routeUrl, res.Route, res.tag)
    }

  }
}

def askForInput(String version, String environment, int duration) {
    def appVersion = version ? "version ${version}" : "application"
    def appEnvironment = environment ? "${environment} environment" : "next environment"
    def proceedMessage = """Would you like to promote ${appVersion} to the ${appEnvironment}?"""

    stage("Approve") {
        try {
            timeout(time: duration, unit: 'MINUTES') {
                input id: 'Proceed', message: "\n${proceedMessage}"
            }
        } catch (err) {
            currentBuild.result = 'ABORTED'
            error "Timeout of $duration minutes has elapsed; aborting ..."
        }
    }
}

def tagImageToDeployEnv(ns, userNamespace, is, tag) {
    try {
        def imageName = is.metadata.name
        sh "oc tag -n ${ns} --alias=true ${userNamespace}/${imageName}:${tag} ${imageName}:${tag}"
    } catch (err) {
        error "Error running OpenShift command ${err}"
    }
}

def deployEnvironment(ns, dc, service, route) {
    Utils.ocApply(this, dc, ns)
    openshiftVerifyDeployment(depCfg: "${dc.metadata.name}", namespace: "${ns}")
    Utils.ocApply(this, service, ns)
    Utils.ocApply(this, route, ns)
    return displayRouteURL(ns, route)

}

def displayRouteURLOnUI(namespace, env, routeUrl, route, version) {
   def routeMetadata = """---
environmentName: "$env"
serviceUrls:
  $route.metadata.name: "$routeUrl"
deploymentVersions:
  $route.metadata.name: "$version"
"""
    Utils.addAnnotationToBuild(this, "environment.services.fabric8.io/$namespace", routeMetadata);
}

def displayRouteURL(namespace, route) {
    try {
        def routeUrl = Utils.shWithOutput(this, "oc get route -n ${namespace} ${route.metadata.name} --template 'http://{{.spec.host}}'")
        echo namespace.capitalize() + " URL: ${routeUrl}"
        return routeUrl
    } catch (err) {
        error "Error running OpenShift command ${err}"
    }
    return null
}
