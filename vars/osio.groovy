
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

def deployEnvironment(_environ, user, is, dc, route) {
  environ = "-"  + _environ

  try {
    sh "oc tag -n ${user}${environ} --alias=true ${user}/${is}:latest ${is}:latest"
  } catch (err) {
    error "Error running OpenShift command ${err}"
  }

  openshiftDeploy(deploymentConfig: "${dc}", namespace: "${user}" + environ)

  try {
    ROUTE_PREVIEW = sh (
      script: "oc get route -n ${user}${environ} ${route} --template 'http://{{.spec.host}}'",
      returnStdout: true
    ).trim()
    echo _environ.capitalize() + " URL: ${ROUTE_PREVIEW}"
  } catch (err) {
    error "Error running OpenShift command ${err}"
  }

}

def getCurrentUser() {
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

def getJsonFromProcessedTemplate(templateVars) {
  templateParams = toParamString(templateVars)

  def output = sh (
    script: "oc process -f .openshiftio/application.yaml ${templateParams} -o json",
    returnStdout: true
  ).trim()
  return new groovy.json.JsonSlurperClassic().parseText(output.trim())
}

def getNameFromTemplate(json, type) {
  def r = json.items.findResults { i ->
    i.kind == type ?
      i.metadata.name :
      null
  }
  // For ImageStream we need to filter out the runtime stuff
  if (type == "ImageStream") {
    r = r.findResults { i ->
      !i.startsWith("runtime") ?
      i :
      null
    }
  }

  if (r.size() == 0) {
    throw new Exception("We didn't find any ${type}")
  }
  if (r.size() > 1) {
    throw new Exception("There should be only one ${type} we have: ${r}")
  }
  return r[0]
}

def getEnvironments(ns) {
  def environments = [:]
  output = sh (
    script: "oc -n ${ns} extract configmap/fabric8-environments --to=-",
    returnStdout: true
  ).trim()
  output.split("\r?\n").each {line ->
    if (line.startsWith("name:")) {
      name = line.trim().replace("name: ", "").toLowerCase()
    }
    if (line.startsWith("namespace:")) {
      namespace = line.trim().replace("namespace: ", "")
      environments[name] = namespace
    }
  }
}

def toParamString(templateVars) {
  String parameters = ""
  templateVars.each{ v, k -> parameters = parameters + (v + "=" + k + " ")}
  return parameters.trim()
}


def main(params) {
  checkout scm;

  if (!fileExists('.openshiftio/application.yaml')) {
    println("File not found: .openshiftio/application.yaml")
    currentBuild.result = 'FAILURE'
    return
  }

  params.templateConfig['SOURCE_REPOSITORY_URL'] = params.templateConfig['SOURCE_REPOSITORY_URL'] ?:  getCurrentRepo()

  json = getJsonFromProcessedTemplate(params.templateConfig)
  templateDC = getNameFromTemplate(json, "DeploymentConfig")
  templateService = getNameFromTemplate(json, "Service")
  templateBC = getNameFromTemplate(json, "BuildConfig")
  templateISDest = getNameFromTemplate(json, "ImageStream")
  templateRoute = getNameFromTemplate(json, "Route")

  currentUser = getCurrentUser()
  params.templateConfig['SOURCE_REPOSITORY_REF'] = sh(script: 'git rev-parse --short HEAD', returnStdout: true).toString().trim()
  templateParams = toParamString(params.templateConfig)

  stages = params.get('stages', ["run", "stage"])
  stage('Processing Template') {
    sh """
       set -u
       set -e

       # Deleting everything cause no resources brah
       for i in ${currentUser}-{stage,run};do
          oc delete all --all -n  \$i
       done

       for i in ${currentUser} ${currentUser}-{stage,run};do
          oc process -f .openshiftio/application.yaml ${templateParams} | \
            oc apply -f- -n \$i
       done

       # Remove dc/service from currentUser
       oc delete dc/${templateDC} service/${templateService} -n ${currentUser}

       #TODO(make it smarter)
       for i in ${currentUser}-{stage,run};do
        oc delete bc ${templateBC} -n \$i
       done
    """
  }

  stage('Building application') {
    openshiftBuild(buildConfig: "${templateBC}", showBuildLogs: 'true')
  }

  if (stages.contains("stage")) {
    stage('Deploy to staging') {
      deployEnvironment("stage", "${currentUser}", "${templateISDest}", "${templateDC}", "${templateRoute}")
      askForInput()
    }
  }

  if(stages.contains("run")) {
    stage('Deploy to Prod') {
      deployEnvironment("run", "${currentUser}", "${templateISDest}", "${templateDC}", "${templateRoute}")
    }
  }
}

def call(body) {
  //TODO: parameters
  def jobTimeOutHour = 1
  def defaultBuilder = 'nodejs'

  def pipelineParams= [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipelineParams
  body()

  pipelineParams.templateConfig = pipelineParams.templateConfig ?: [:]
  pipelineParams.templateConfig["SUFFIX_NAME"] = pipelineParams.templateConfig["SUFFIX_NAME"] ?: "-osio-${env.BRANCH_NAME}".toLowerCase()

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
