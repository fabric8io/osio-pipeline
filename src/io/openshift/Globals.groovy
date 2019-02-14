package io.openshift

class Globals implements Serializable {
 // used by vars/config to store all parameters passed into config
 static config = [:]

 // used by vars/plugins to store all parameters passed into plugins
 static plugins = [:]
}
