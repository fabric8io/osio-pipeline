import io.openshift.Utils;

def call(Map args = [:]) {
  if (!args.env) {
    error "Missing manadatory parameter: env"
  }

  if (!args.resources) {
    error "Missing manadatory parameter: resources"
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
    // Ensure that waiting for approval happen on master so that slave isn't
    // held up waiting for input
    stage("Approve") {
      askForInput(res.tag, args.env, args.timeout ?: 30)
    }
  }

  stage("Deploy to ${args.env}") {
    spawn(image: "oc") {
      def userNS = Utils.usersNamespace();
      def deployNS = userNS + "-" + args.env;

      tagImageToDeployEnv(deployNS, userNS, res.ImageStream, res.tag)
      deployEnvironment(deployNS, res.DeploymentConfig, res.Service, res.Route, res.tag, args.env)
    }

  }
}

def askForInput(String version, String environment, int duration) {
  def appVersion = version ? "version ${version}" : "application"
  def appEnvironment = environment ? "${environment} environment" : "next environment"
  def proceedMessage = """Would you like to promote ${appVersion} to the ${appEnvironment}?"""

  try {
    timeout(time: duration, unit: 'MINUTES') {
      input id: 'Proceed', message: "\n${proceedMessage}"
    }
  } catch (err) {
    currentBuild.result = 'ABORTED'
    error "Timeout of $duration minutes has elapsed; aborting ..."
  }
}

def tagImageToDeployEnv(ns, userNamespace, imageStreams, tag) {
  imageStreams.each { is ->
    try {
      def imageName = is.metadata.name
      sh "oc tag -n ${ns} --alias=true ${userNamespace}/${imageName}:${tag} ${imageName}:${tag}"
    } catch (err) {
      error "Error running OpenShift command ${err}"
    }
  }
}

def deployEnvironment(ns, dcs, services, routes, version, env) {
  dcs.each { dc ->
    Utils.ocApply(this, dc, ns)
    openshiftVerifyDeployment(depCfg: "${dc.metadata.name}", namespace: "${ns}")
  }

  services.each { s -> Utils.ocApply(this, s, ns) }
  routes.each { r -> Utils.ocApply(this, r, ns) }
  annotateRouteURL(ns, env, routes, version)
}

def annotateRouteURL(ns, env, routes, version) {
  def svcURLs = routes.inject(''){ acc, r -> acc + "\n  ${r.metadata.name}: ${displayRouteURL(ns, r)}" }
  def depVersions = routes.inject(''){ acc, r ->  acc + "\n  ${r.metadata.name}: $version"

  def annotation = """---
environmentName: "$env"
serviceUrls: $svcURLs
deploymentVersions: $depVersions
"""
  Utils.addAnnotationToBuild(this, "environment.services.fabric8.io/$ns", annotation);
}

def displayRouteURL(ns, route) {
  try {
    def routeUrl = Utils.shWithOutput(this,
      "oc get route -n ${ns} ${route.metadata.name} --template 'http://{{.spec.host}}'")

    echo "${ns.capitalize()} URL: ${routeUrl}"
    return routeUrl

  } catch (err) {
    error "Error running OpenShift command ${err}"
  }
  return null
}
