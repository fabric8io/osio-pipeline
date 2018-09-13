package io.openshift
import io.openshift.plugins.*

class Plugins implements Serializable {
    static def register() {
      new analytics().register()
    }
}

