import io.openshift.Utils

def call(Map args = [:], body = null) {
    def label = Utils.buildID(env.JOB_NAME, env.BUILD_NUMBER, prefix=args.name)

    podTemplate(
      label: label,
      cloud: 'openshift',
      serviceAccount: 'jenkins',
      inheritFrom: 'base',
      containers: [
        slaveTemplate(args.name, args.image, args.shell),
        jnlpTemplate()
      ],
      volumes: volumes(),
    ) {
      try {
        timeout(time: 30, unit: 'MINUTES') {
          node (label) {
            container(name: args.name, shell: args.shell) {
              body()
            }
          }
        }
      } catch (e) {
        currentBuild.result = 'FAILED'
        error "Not able to start a slave pod with label ${label}"
        Events.emit(["build.end", "build.fail"], [status: status, namespace: namespace])
      }
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

def slaveTemplate(name, image, shell) {
    containerTemplate(
        name: name,
        image: image,
        command: "$shell -c",
        args: 'cat',
        ttyEnabled: true,
        workingDir: '/home/jenkins/',
        resourceLimitMemory: '640Mi'
    )
}

def jnlpTemplate() {
    def jnlpImage = 'fabric8/jenkins-slave-base-centos7:vb0268ae'

    return containerTemplate(
        name: 'jnlp',
        image: "${jnlpImage}",
        args: '${computer.jnlpmac} ${computer.name}',
        workingDir: '/home/jenkins/',
        resourceLimitMemory: '256Mi'
    )
}
