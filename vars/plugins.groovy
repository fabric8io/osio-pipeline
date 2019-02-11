import io.openshift.Globals

def call(plugins = [:]) {
  Globals.plugins << plugins
}
