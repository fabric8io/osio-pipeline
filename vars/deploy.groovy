import io.openshift.Utils;

def call(Map args = [:]) {
  if (!args.env) {
    error "Missing manadatory parameter: env"
  }

  if (!args.resources) {
    error "Missing manadatory parameter: resources"
  }

  def required = ['ImageStream', 'DeploymentConfig', 'meta']
  def res = Utils.mergeResources(args.resources)

  def found = res.keySet()
  def missing = required - found
  if (missing) {
    error "Missing mandatory build resources params: $missing; found: $found"
  }

  def tag = res.meta.tag
  if (!tag) {
    error "Missing mandatory metadata: tag"
  }


  if (args.approval == 'manual') {
    // Ensure that waiting for approval happen on master so that slave isn't
    // held up waiting for input
    stage("Approve") {
      askForInput(tag, args.env, args.timeout ?: 30)
    }
  }

  def image = args.image
  if (!image) {
    image = args.commands ? config.runtime() : 'oc'
  }

  stage("Rollout to ${args.env}") {
    spawn(image: image) {
      def userNS = Utils.usersNamespace();
      def deployNS = userNS + "-" + args.env;

      tagImageToDeployEnv(deployNS, userNS, res.ImageStream, tag)
      applyResources(deployNS, res)
      verifyDeployments(deployNS, res.DeploymentConfig)
      annotateRoutes(deployNS, args.env, res.Route, tag)
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

def applyResources(ns, res) {
  def allowed = { e -> !(e.key in ["ImageStream", "BuildConfig", "meta"]) }
  def resources = res.findAll(allowed)
    .collect({ it.value })
  Utils.ocApply(this, resources, ns)
}

def verifyDeployments(ns, dcs) {
  dcs.each { dc ->
    openshiftVerifyDeployment(depCfg: "${dc.metadata.name}", namespace: "${ns}")
  }
}

def annotateRoutes(ns, env, routes, version) {
  if (!routes) {
    return
  }

  def svcURLs = routes.inject(''){ acc, r -> acc + "\n  ${r.metadata.name}: ${displayRouteURL(ns, r)}" }
  def depVersions = routes.inject(''){ acc, r ->  acc + "\n  ${r.metadata.name}: $version" }

  def annotation = """---
environmentName: "$env"
serviceUrls: $svcURLs
deploymentVersions: $depVersions
"""
  Utils.addAnnotationToBuild(this, "environment.services.fabric8.io/$ns", annotation)
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
