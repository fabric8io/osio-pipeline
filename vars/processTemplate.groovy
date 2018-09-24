import io.openshift.Utils

def call(args=[:]) {
  def file = args.file ?: ".openshiftio/application.yaml"
  if (!fileExists(file)) {
      error "Application template could not be found at $file; aborting ..."
      currentBuild.result = 'ABORTED'
      return
  }



  def templateParams = readYaml(file: file).parameters*.name ?: []
  def params = applyDefaults(args.params, templateParams)

  def ocParams = params.collect { k,v -> "$k=$v"}.join(' ')
  def yaml = Utils.shWithOutput(this, "oc process -f $file $ocParams -o yaml")

  def kind = { r -> r.kind }
  def resources = readYaml(text: yaml).items.groupBy(kind)

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
  setParam('SOURCE_REPOSITORY_REF') { Utils.shWithOutput(this, "git rev-parse --short HEAD") }
  setParam('RELEASE_VERSION') { Utils.shWithOutput(this, "git rev-list --count HEAD") }
  return params
}
