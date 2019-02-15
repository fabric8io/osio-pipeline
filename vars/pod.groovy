import static io.openshift.Utils.buildID

def call(Map args = [:], body = null) {
    def label = buildID(env.JOB_NAME, env.BUILD_NUMBER, prefix=args.name)
    def envVars = processEnvVars(args.envVars)

    podTemplate(
      label: label,
      cloud: 'openshift',
      serviceAccount: 'jenkins',
      inheritFrom: 'base',
      containers: [
        slaveTemplate(args.name, args.image, args.shell, envVars),
        jnlpTemplate()
      ],
      volumes: volumes(),
      slaveConnectTimeout: 1,
    ) {
      /*try {
        timeout(time: 30, unit: 'MINUTES') {*/
          node (label) {
            container(name: args.name, shell: args.shell) {
              body()
            }
          }
      /*  }
      } catch (e) {
        currentBuild.result = 'FAILED'
        error "Not able to start a slave pod with label ${label}"
        Events.emit(["build.end", "build.fail"], [status: status, namespace: namespace])
      }*/
    }
}

def volumes() {
  [
    secretVolume(secretName: 'jenkins-release-gpg', mountPath: '/home/jenkins/.gnupg-ro'),
    secretVolume(secretName: 'jenkins-hub-api-token', mountPath: '/home/jenkins/.apitoken'),
    secretVolume(secretName: 'jenkins-ssh-config', mountPath: '/root/.ssh-ro'),
    secretVolume(secretName: 'jenkins-git-ssh', mountPath: '/root/.ssh-git-ro')
  ]
}

def slaveTemplate(name, image, shell, envVars) {
    if (envVars) {
      return containerTemplate(
          name: name,
          image: image,
          command: "$shell -c",
          args: 'cat',
          ttyEnabled: true,
          workingDir: '/home/jenkins/',
          resourceLimitMemory: '640Mi',
          envVars: envVars
      )
    } else {
      return containerTemplate(
          name: name,
          image: image,
          command: "$shell -c",
          args: 'cat',
          ttyEnabled: true,
          workingDir: '/home/jenkins/',
          resourceLimitMemory: '640Mi'
      )
    }
}

def jnlpTemplate() {
    def jnlpImage = 'quay.io/openshift/origin-jenkins-agent-base:v4.0'

    return containerTemplate(
        name: 'jnlp',
        image: "${jnlpImage}",
        args: '${computer.jnlpmac} ${computer.name}',
        workingDir: '/home/jenkins/',
        resourceLimitMemory: '256Mi'
    )
}

def processEnvVars(env){
  envVars = []
  env.each { e -> envVars << envVar(key: "${e.key}", value: "${e.value[0]}")
  }
  return envVars
}
