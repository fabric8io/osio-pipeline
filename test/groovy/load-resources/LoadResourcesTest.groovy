import org.junit.Test
import org.yaml.snakeyaml.Yaml

class LoadResourcesTest extends PipelineHelper {

  static final String DEFAULT_RESOURCE_FILE = "test/groovy/load-resources/resources/resource.yaml"

  @Test
  void should_load_single_resource() throws Exception {
    //GIVEN
    initMocks()
    mockSh({ arg ->
      return Runtime.getRuntime().exec(arg.script).text
    })
    Script script = loadScript("load-resources/configmap/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "resources [ConfigMap]")
    assertJobStatusSuccess()
  }

  @Test
  void should_load_multiple_resources() throws Exception {
    //GIVEN
    initMocks()
    mockSh({ arg ->
      return Runtime.getRuntime().exec(arg.script).text
    })
    Script script = loadScript("load-resources/lists/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "resources [RoleBinding]")
    assertStepExecutes("echo", "2 RoleBinding resources")
    assertJobStatusSuccess()
  }

  @Test
  void should_fail_load_resource() throws Exception {
    //GIVEN
    initMocks()
    mockSh({ arg ->
      if(arg.script.contains("oc apply --dry-run=true --validate=true")) {
        binding.getVariable('currentBuild').result = 'FAILURE'
      }
    })
    Script script = loadScript("load-resources/invalid/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertJobStatusFailure()
  }

  @Test
  void should_fail_if_no_file_argument() throws Exception {
    //GIVEN
    initMocks()
    Script script = loadScript("load-resources/nofile/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertJobStatusFailure()
  }

  @Test
  void should_abort_if_file_not_exist() throws Exception {
    //GIVEN
    helper.registerAllowedMethod("fileExists", [String.class], { searchTerm ->
      return false
    })

    Script script = loadScript("load-resources/filenotfound/jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertJobStatusFailure()
  }

  def initMocks() {
    helper.registerAllowedMethod("fileExists", [String.class], { searchTerm ->
      return true
    })

    helper.registerAllowedMethod("readYaml", [Map], { Map parameters ->
      Yaml yaml = new Yaml()

      if (parameters.text) {
        return yaml.load(parameters.text)
      }

      return yaml.load(new File(parameters.file).text)
    })
  }

  def mockSh(Closure c) {
    helper.registerAllowedMethod("sh", [Map], { Map parameters ->
      return c.call(parameters)
    })
  }

}
