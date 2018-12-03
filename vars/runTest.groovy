
def call(Map args) {
  stage("Run Tests") {
    if (!args.commands) {
      error "Missing manadatory parameter: commands, please specify the command to run test"
    }
    def image = args.image
    if (!image) {
      image = config.runtime() ?: 'oc'
    }
    spawn(image: image, version: config.version(), commands: args.commands)
  }
}