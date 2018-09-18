def call(Map args = [:]) {
    if(!args.file) {
        error "Missing manadatory parameter: file"
    }

    def resources = readYaml(file: args.file)

    if (resources instanceof LinkedList) {
      return resources.groupBy({ r -> r.kind })
    } else if (resources.kind == "List") {
      return resources.items.groupBy({ r -> r.kind })
    } else {
      return [resources].groupBy({ r -> r.kind })
    }
}

