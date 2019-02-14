package io.openshift

import io.openshift.plugins.analytics
import io.openshift.Globals

class Plugins implements Serializable {
    static def register() {
      // analytics plugins is enabled by default
      Globals.plugins["analytics"] = Globals.plugins["analytics"] ?: [disabled: false]
      if (Globals.plugins["analytics"].disabled ?: false) {
        return
      }
      new analytics().register()
    }
}

