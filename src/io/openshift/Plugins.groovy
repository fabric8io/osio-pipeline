package io.openshift

import io.openshift.plugins.analytics
import io.openshift.Globals
import static io.openshift.Utils.pluginAvailable

class Plugins implements Serializable {
    static def register() {
      // analytics plugins is enabled by default
      Globals.plugins["analytics"] = Globals.plugins["analytics"] ?: [disabled: false]
      if (Globals.plugins["analytics"].disabled ?: false || !pluginAvailable("bayesian")) {
        return
      }
      new analytics().register()
    }
}

