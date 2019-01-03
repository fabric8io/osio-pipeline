import io.fabric8.openshift.client.OpenShiftClient
import io.fabric8.openshift.client.server.mock.OpenShiftServer
import org.junit.Rule
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class BuildTest extends PipelineHelper {

  static final String STUB_YAML_FILE = "test/groovy/builds/valid/application.yaml"
  static final String DEFAULT_FILE_NAME = ".openshiftio/application.yaml"

  @Rule
  public OpenShiftServer server = new OpenShiftServer(true, true)

  @Test
  void should_fail_on_no_resources() throws Exception {
    //GIVEN
    mockSh({ arg ->
      return Runtime.getRuntime().exec(arg.script).text
    })
    Script script = loadScript("builds/no-resources-arg/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertJobStatusFailure()
  }

  @Test
  void should_fail_if_missing_mandatory_resources() throws Exception {
    //GIVEN
    initMocks()
    Script script = loadScript("builds/invalid-build-resource/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertJobStatusFailure()
  }

  @Test
  void should_tag_image_and_trigger_build() throws Exception {
    //GIVEN
    OpenShiftClient oc = server.getOpenshiftClient()
    binding.setVariable("oc", oc)
    binding.setProperty("oc", oc)
    initMocks(oc)
    Script script = loadScript("builds/valid-build-resource/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("sh", "2-buildconfig")
    assertStepExecutes("openshiftBuild", "buildConfig=nodejs-configmap-s2i-master")
    assertJobStatusSuccess()
  }

  def initMocks(oc = null) {
    binding.setProperty("env", ["BUILD_NUMBER": "2", "BRANCH_NAME": "master"])
    mockFileExists()
    mockReadYaml()
    mockWriteYaml()
    mockSHOfStringParams(oc)
    mockSHOfMapParams()
    mockOpenShiftBuild()
  }

  def mockOpenShiftBuild() {
    helper.registerAllowedMethod("openshiftBuild", [Map], { p ->
      return ""
    })
  }

  def mockSHOfMapParams() {
    helper.registerAllowedMethod("sh", [Map], { Map parameters ->
      if (parameters.script.contains("git ")) {
        return "some"
      }

      if (parameters.script.contains("oc process")) {
        def command = parameters.script + " --local=true"
        command = command.contains(DEFAULT_FILE_NAME) ? command.replaceAll(DEFAULT_FILE_NAME, STUB_YAML_FILE) : command
        return Runtime.getRuntime().exec(command).text
      }

      if (parameters.script.contains("oc get is")) {
        return ""
      }

      return Runtime.getRuntime().exec(parameters.script).text
    })
  }

  def mockSHOfStringParams(oc) {
    helper.registerAllowedMethod("sh", [String], { String str ->
      str.eachLine {
        def command = it.trim()
        if (!command.isEmpty()) {
          if (oc != null && it.contains("oc apply -f ")) {
            oc.load(getFile(command)).createOrReplace()
            return ""
          }

          return Runtime.getRuntime().exec(command).text
        }
      }
    })
  }

  def mockWriteYaml() {
    helper.registerAllowedMethod("writeYaml", [Map], { Map parameters ->
      Yaml yaml = new Yaml()
      File resourceFile = new File(parameters.file)
      resourceFile.parentFile.mkdirs()
      yaml.dump(parameters.data, new FileWriter(resourceFile))
    })
  }

  def mockReadYaml() {
    helper.registerAllowedMethod("readYaml", [Map], { Map parameters ->
      Yaml yaml = new Yaml()

      if (parameters.text) {
        return yaml.load(parameters.text)
      }

      def file = isNotDefaultTemplate(parameters.file) ? parameters.file : STUB_YAML_FILE
      return yaml.load(new File(file).text)
    })
  }

  def mockFileExists() {
    helper.registerAllowedMethod("fileExists", [String.class], { searchTerm ->
      return true
    })
  }

  def mockSh(Closure c) {
    helper.registerAllowedMethod("sh", [Map], { Map parameters ->
      return c.call(parameters)
    })
  }

  def getFile(String s) {
    return new FileInputStream(s.tokenize(" ").get(3))
  }

}
