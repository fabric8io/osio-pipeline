def call(Map args = [:]) {
  if(!args.file) {
    error "Missing manadatory parameter: file"
  }

  def yaml = readYaml(file: args.file)

  // resources can be:
  //  - a single resource
  //  - an array of resources
  //  - a map with `kind` property set "List"
  //    and items property is a list of resources
  //
  // for each of the above, loadResources must return [kind: [Resources, ...]]

  def isListKind = yaml instanceof Map && yaml.kind == "List"
  def resources = isListKind ? yaml.items : [yaml].flatten()
  return resources.groupBy({ r -> r.kind })
}
