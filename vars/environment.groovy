import static io.openshift.Utils.usersNamespace

def call(Map args = [:]){
    if (!args.type) {
        error "Missing manadatory parameter: type"
        return
    }

    def userNS = usersNamespace(args.osClient)

    if (args.type.equalsIgnoreCase("build")) { 
        return userNS
    }

    if (args.type.equalsIgnoreCase("stage")) { 
        return userNS + "-stage"
    }

    if (args.type.equalsIgnoreCase("run")) {
        return userNS + "-run"
    }
}