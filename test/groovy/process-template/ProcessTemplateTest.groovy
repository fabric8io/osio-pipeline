import org.junit.Test
import org.yaml.snakeyaml.Yaml

class ProcessTemplateTest extends PipelineHelper {

  static final String DEFAULT_YAML_FILE = "test/groovy/process-template/valid-templates/application.yaml"

  @Test
  void should_process_template() throws Exception {
    //GIVEN
    initMocks()
    Script script = loadScript("process-template/valid-templates/template.jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "resources [ImageStream, BuildConfig, Service, DeploymentConfig, Route, meta]")
    assertStepExecutes("echo", "version 1.0.2")
    assertStepExecutes("echo", "image [1.0.2]")
    assertJobStatusSuccess()
  }

  @Test
  void should_process_template_with_default_app_yaml() throws Exception {
    //GIVEN
    initMocks()
    Script script = loadScript("process-template/valid-templates/default.jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "resources [ImageStream, BuildConfig, Service, DeploymentConfig, Route, meta]")
    assertStepExecutes("echo", "version 1.0.2")
    assertStepExecutes("echo", "image [1.0.2]")
    assertJobStatusSuccess()
  }

  @Test
  void check_release_version_absent_in_template() throws Exception {
    //GIVEN
    initMocks()
    Script script = loadScript("process-template/invalid-templates/jenkinsfile")

    //when
    runScript(script)

    // then
    assertJobStatusFailure()
  }

  def initMocks() {
    binding.setProperty("env", ["BUILD_NUMBER": "2", "BRANCH_NAME": "master"])

    helper.registerAllowedMethod("fileExists", [String.class], { searchTerm ->
      return true
    })

    helper.registerAllowedMethod("readYaml", [Map], { Map parameters ->
      Yaml yaml = new Yaml()

      if (parameters.text) {
        return yaml.load(parameters.text)
      }

      def file = isNotDefaultTemplate(parameters.file) ? parameters.file : DEFAULT_YAML_FILE
      return yaml.load(new File(file).text)
    })

    helper.registerAllowedMethod("sh", [Map], { Map parameters ->
      if (parameters.script.contains("git ")) {
        return "some"
      }

      if (parameters.script.contains("oc process")) {
        def command = ""
        if (parameters.script.contains(".openshiftio/application.yaml")) {
          command = parameters.script.replaceAll(".openshiftio/application.yaml", DEFAULT_YAML_FILE) + " --local=true"
        } else {
          command = parameters.script + " --local=true"
        }

        return Runtime.getRuntime().exec(command).text
      }
    })
  }

}
