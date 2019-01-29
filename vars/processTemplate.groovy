import static io.openshift.Utils.shWithOutput

def call(args=[:]) {
  def file = args.file ?: ".openshiftio/application.yaml"
  if (!fileExists(file)) {
    currentBuild.result = 'FAILURE'
    error "Application template could not be found at $file; aborting ..."
    return
  }

  def parameters = readYaml(file: file).parameters ?: []
  if(!isValidTemplate(parameters)){
    currentBuild.result = 'FAILURE'
    error "Invalid template, please refer below link to fix the application template (application.yaml) and build again \n"+
      "https://github.com/fabric8io/osio-pipeline#processtemplate"
    return
  }
  def paramNames = parameters.collect { it.name }
  def params = applyDefaults(args.params, paramNames)

  def ocParams = params.collect { k,v -> "$k=$v"}.join(' ')
  def processed = shWithOutput(this, "oc process -f $file $ocParams -o yaml")

  def kind = { r -> r.kind }
  def resources = readYaml(text: processed).items.groupBy(kind)

  // add metadata about the resource that is processed
  // tag is a special metadata and used to tag IS and else where
  if (params.RELEASE_VERSION) {
    resources.meta = [tag: params.RELEASE_VERSION]
  }
  return resources
}

// returns a map of parameter name: value by choosing from provided if the
// key is present in templateParams or uses default value
def applyDefaults(provided=[:], templateParams) {

  def params = [:]
  def setParam = { key, compute ->
    if (key in templateParams) {
      params[key] = provided[key] ?: compute()
    }
  }

  setParam('SUFFIX_NAME') { "-${env.BRANCH_NAME}".toLowerCase() }
  setParam('SOURCE_REPOSITORY_REF') { shWithOutput(this, "git rev-parse --short HEAD") }
  setParam('SOURCE_REPOSITORY_URL') { shWithOutput(this, "git config remote.origin.url") }
  setParam('RELEASE_VERSION') { shWithOutput(this, "git rev-list --count HEAD") }
  return params
}

boolean isValidTemplate(parameters=[[:]]){
  def templateParam = []
  parameters.each {
    templateParam << it.name
  }
  if(!templateParam.contains("RELEASE_VERSION")){
    echo "Missing parameter RELEASE_VERSION in openshift template"
    return false
  }
  return true
}
