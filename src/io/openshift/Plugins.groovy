package io.openshift

import io.openshift.plugins.analytics
import io.openshift.Globals
import static io.openshift.Utils.pluginAvailable

class Plugins implements Serializable {
    static def register() {
      // analytics plugins is enabled by default
      Globals.plugins["analytics"] = Globals.plugins["analytics"] ?: [disabled: false]

      def disabled = Globals.plugins["analytics"].disabled ?: false
      if (disabled || !pluginAvailable("bayesian"))  {
        return
      }
      new analytics().register()
    }
}

