def call(Map args = [:]) {
  if(!args.file) {
    error "Missing manadatory parameter: file"
  }

  def resources = readYaml(file: args.file)

  if (resources instanceof Map && resources.kind == "List") {
    return resources.items.groupBy({ r -> r.kind })
  } else {
    return [resources].flatten().groupBy({ r -> r.kind })
  }
}
