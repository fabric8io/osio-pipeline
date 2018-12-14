package io.openshift

import io.fabric8.openshift.client.DefaultOpenShiftClient
import io.fabric8.openshift.client.OpenShiftClient
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class Utils {

  static String shWithOutput(script, String command) {
    return script.sh(
      script: command,
      returnStdout: true
    ).trim()
  }

  static def ocApply(script, resource, namespace) {
    def buildNum = script.env.BUILD_NUMBER
    def resources = [resource].flatten()

    resources.each { r ->
      def kind = r.kind.toLowerCase()
      def resourceFile = ".openshiftio/.tmp-${namespace}-${buildNum}-${kind}.yaml"
      script.writeYaml file: resourceFile, data: r
      script.sh """
        oc apply -f ${resourceFile} -n ${namespace}
        rm -f ${resourceFile}
      """
    }
  }

  static String usersNamespace(oc = null) {
    def ns = currentNamespace(oc)
    if (ns.endsWith("-jenkins")) {
      return ns.substring(0, ns.lastIndexOf("-jenkins"))
    }
    return ns
  }

  static String currentNamespace(oc = null) {
    oc = oc ?: new DefaultOpenShiftClient()
    return oc.getNamespace()
  }

  /**
   * Returns the id of the build, which consists of the job name,
   * build number and an optional prefix.
   * @param jobName usually env.JOB_NAME
   * @param buildNum usually env.BUILD_NUMBER
   * @param prefix optional prefix to use, default is empty string.
   * @return
   */
  static def buildID(String jobName, String buildNum, String prefix = '') {
    // job name from the org plugin
    def repo = repoNameForJob(jobName)
    prefix = prefix ? prefix + '_' : ''
    return "${prefix}${repo}_${buildNum}".replaceAll('-', '_')
      .replaceAll('/', '_')
      .replaceAll(' ', '_')
  }

  // helper to get the repo name from the job name when
  // using org + branch github plugins
  static String repoNameForJob(String jobName) {
    // job name from the org plugin
    if (jobName.count('/') > 1) {
      return jobName.substring(jobName.indexOf('/') + 1, jobName.lastIndexOf('/'))
    }
    // job name from the branch plugin
    if (jobName.count('/') > 0) {
      return jobName.substring(0, jobName.lastIndexOf('/'))
    }
    // normal job name
    return jobName
  }

  static def addAnnotationToBuild(script, annotation, value) {
    def buildName = buildNameForJob(script.env.JOB_NAME, script.env.BUILD_NUMBER)
    if (!isValidBuildName(buildName)) {
      script.error "No matching openshift build with name ${buildName} found"
    }

    script.echo "Adding annotation '${annotation}: ${value}' to Build ${buildName}"
    OpenShiftClient oc = new DefaultOpenShiftClient()
    oc.builds().inNamespace(usersNamespace()).withName(buildName)
      .edit().editMetadata().addToAnnotations(annotation, value).endMetadata()
      .done()
  }

  static def buildNameForJob(String jobName, String buildNumber) {
    def activeInstance = Jenkins.getInstance()
    def job = (WorkflowJob) activeInstance.getItemByFullName(jobName)
    def run = job.getBuildByNumber(Integer.parseInt(buildNumber))
    def clazz = Thread.currentThread().getContextClassLoader().loadClass("io.fabric8.jenkins.openshiftsync.BuildCause")
    def cause = run.getCause(clazz)
    if (cause != null) {
      return cause.name
    }
    return null
  }

  static boolean isValidBuildName(String buildName) {
    OpenShiftClient oc = new DefaultOpenShiftClient()
    def build = oc.builds().inNamespace(usersNamespace()).withName(buildName).get()
    return build != null
  }

  static def mergeResources(res) {
    def all = [res].flatten()
    if (all.size == 1) {
      return res
    }

    all.inject([:]) { acc, resource ->
      resource.each { k, v ->
        if (k == 'meta' && !acc.meta) {
          acc.meta = [tag: null]
        }
        acc[k] = (acc[k] ?: []) + v
      }
      acc
    }
  }

}
