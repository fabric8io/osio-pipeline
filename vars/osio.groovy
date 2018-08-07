
def askForInput() {
  //TODO: parameters
  def approvalTimeOutMinutes = 30;
  def proceedMessage = """Would you like to promote to the next environment?
          """
  try {
    timeout(time: approvalTimeOutMinutes, unit: 'MINUTES') {
      input id: 'Proceed', message: "\n${proceedMessage}"
    }
  } catch (err) {
    throw err
  }
}

def displayRouteURL(nameSpace, route) {
  try {
    ROUTE_PREVIEW = shWithOutput("oc get route -n ${nameSpace} ${route.metadata.name} --template 'http://{{.spec.host}}'")
    echo nameSpace.capitalize() + " URL: ${ROUTE_PREVIEW}"
  } catch (err) {
    error "Error running OpenShift command ${err}"
  }
}

def tagImageToDeployEnv(deployNameSpace, userNameSpace, is, tag) {
  try {
    sh "oc tag -n ${deployNameSpace} --alias=true ${userNameSpace}/${is}:${tag} ${is}:${tag}"
  } catch (err) {
    error "Error running OpenShift command ${err}"
  }
}

def deployEnvironment(deployNameSpace, dc,  service, route) {
  ocApplyResource(dc, deployNameSpace)
  openshiftVerifyDeployment(depCfg: "${dc.metadata.name}", namespace: "${deployNameSpace}")
  ocApplyResource(service, deployNameSpace)
  ocApplyResource(route, deployNameSpace)
  displayRouteURL(deployNameSpace, route)
}

def getUserNameSpace() {
  return sh (
    script: "oc whoami|sed 's/.*:\\(.*\\)-jenkins:.*/\\1/'",
    returnStdout: true
    ).trim()
}

def getCurrentRepo() {
  return sh (
    script: "git config remote.origin.url",
    returnStdout: true
    ).trim()
}

def toParamString(templateVars) {
  String parameters = ""
  templateVars.each{ v, k -> parameters = parameters + (v + "=" + k + " ")}
  return parameters.trim()
}

def getProcessedTemplate(templateVars) {
  def resources = [:]

  def templateParams = toParamString(templateVars)
  def output = sh (
    script: "oc process -f .openshiftio/application.yaml ${templateParams} -o yaml",
    returnStdout: true
  ).trim()

  readYaml(text: output).items.each {
    r -> resources[r.kind] = r
  }

  return resources
}

def shWithOutput(String command) {
  return sh(
          script: command,
          returnStdout: true
  ).trim()
}

def ocApplyResource(resource, namespace) {
  def resourceFile = "/tmp/${namespace}${env.BUILD_NUMBER}${resource.kind}.yaml"
  writeYaml file: resourceFile, data: resource
  sh "oc apply -f ${resourceFile} -n ${namespace}"
}

def createImageStream(imageStream, namespace) {
  def isName = imageStream.metadata.name
  def isFound = shWithOutput("oc get is/$isName -n $namespace --ignore-not-found")
  if (!isFound) {
    ocApplyResource(imageStream, namespace)
  }
}

def buildProject(buildConfig, namespace) {
  ocApplyResource(buildConfig, namespace)
  openshiftBuild(buildConfig: "${buildConfig.metadata.name}", showBuildLogs: 'true')
}

def main(params) {
  checkout scm

  if (!fileExists('.openshiftio/application.yaml')) {
    println("File not found: .openshiftio/application.yaml")
    currentBuild.result = 'FAILURE'
    return
  }

  params.templateConfig['SOURCE_REPOSITORY_URL'] = params.templateConfig['SOURCE_REPOSITORY_URL'] ?:  getCurrentRepo()
  params.templateConfig['SOURCE_REPOSITORY_REF'] = shWithOutput("git rev-parse --short HEAD")

  def resources = getProcessedTemplate(params.templateConfig)
  def imageStreamName = resources.ImageStream.metadata.name

  def tag = params.templateConfig['RELEASE_VERSION']
  def userNameSpace = getUserNameSpace()
  def stages = params.get('stages', ["run", "stage"])

  stage('Building application') {
    createImageStream(resources.ImageStream, userNameSpace)
    buildProject(resources.BuildConfig, userNameSpace)
  }

  if (stages.contains("stage")) {
    stage('Deploy to staging') {
      def deployNameSpace = userNameSpace + "-" + "stage"
      tagImageToDeployEnv(deployNameSpace, userNameSpace, imageStreamName, tag)
      deployEnvironment(deployNameSpace, resources.DeploymentConfig, resources.Service, resources.Route)
      askForInput()
    }
  }

  if(stages.contains("run")) {
    stage('Deploy to Prod') {
      def deployNameSpace = userNameSpace + "-" + "run"
      tagImageToDeployEnv(deployNameSpace, userNameSpace, imageStreamName, tag)
      deployEnvironment(deployNameSpace, resources.DeploymentConfig, resources.Service, resources.Route)
    }
  }
}

def call(body) {
  def jobTimeOutHour = 1
  def defaultBuilder = 'nodejs'

  def pipelineParams = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipelineParams
  body()

  pipelineParams.templateConfig = pipelineParams.templateConfig ?: [:]
  pipelineParams.templateConfig["SUFFIX_NAME"] = pipelineParams.templateConfig["SUFFIX_NAME"] ?: "-${env.BRANCH_NAME}".toLowerCase()

  try {
    timeout(time: jobTimeOutHour, unit: 'HOURS') {
      node(pipelineParams.get(defaultBuilder)) {
        main(pipelineParams)
      }
    }
  } catch (err) {
    echo "in catch block"
    echo "Caught: ${err}"
    currentBuild.result = 'FAILURE'
    throw err
  }
}
