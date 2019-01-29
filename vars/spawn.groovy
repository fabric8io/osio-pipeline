import static io.openshift.Utils.mergeResources

def call(Map args = [:], body = null){
    if (args.commands == null && body == null) {
        error "Please specify either command or body; aborting ..."
        currentBuild.result = 'ABORTED'
        return
    }

    def checkoutScm = args.checkout_scm ?: true

    // oc is available on master so don't spawn unnecessarily
    if (args.image == "oc") {
      execute(args.commands, body)
      return
    }

    def spec = specForImage(args.image, args.version?: 'latest')

    // read and merge environment variable passed via spwan api and spec
    def envVars = mergeEnvs(args, spec)

    pod(name: args.image, image: spec.image, shell: spec.shell, envVars: envVars) {
      if (checkoutScm) {
        checkout scm
      }

      execute(args.commands, body)
    }
}

def execute(commands, body) {
  if (commands) {
    sh commands
  }

  if (body) {
    body()
  }
}

def mergeEnvs(args, spec){
    // read if any environment variable is passed via Jenkinsfile calling spawn api directly
    def apiEnvVars = args.envVar ?: []
    // read environment variable from spec
    def specEnvVars = spec.envVar ?: []
    return mergeResources(apiEnvVars + specEnvVars)
}

def specForImage(image, version){
  // TODO use proper images
  def specs = [
    "node": [
      "latest": [
            image: "openshift/jenkins-agent-nodejs-8-centos7",
            shell: '/bin/bash'
        ],
      "8.9": [
            image: "openshift/jenkins-agent-nodejs-8-centos7",
            shell: '/bin/bash'
      ],
      "4.6": [
            image: "openshift/jenkins-slave-nodejs-centos7",
            shell: '/bin/bash'
      ],
    ],
    "oc": [
      "latest": [
            image: "openshift/jenkins-agent-nodejs-8-centos7",
            shell: '/bin/bash'
      ],
      "3.11": [
            image: "openshift/jenkins-agent-nodejs-8-centos7",
            shell: '/bin/bash'
      ],
    ],
    "java": [
      "latest": [
            image: "openshift/jenkins-slave-maven-centos7:v4.0",
            shell: '/bin/bash',
            envVar: [
            'MAVEN_OPTS': '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn',
            '_JAVA_OPTIONS': '-Duser.home=/home/jenkins -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dsun.zip.disableMemoryMapping=true -XX:+UseParallelGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xms10m -Xmx192m'
            ],
      ],
      "1.8": [
            image: "openshift/jenkins-slave-maven-centos7:v4.0",
            shell: '/bin/bash',
            envVar: [
            'MAVEN_OPTS': '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn',
            '_JAVA_OPTIONS': '-Duser.home=/home/jenkins -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dsun.zip.disableMemoryMapping=true -XX:+UseParallelGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Xms10m -Xmx192m'
            ],
      ],
    ],
  ]

  // TODO: validate image in specs
  return specs[image][version]
}
