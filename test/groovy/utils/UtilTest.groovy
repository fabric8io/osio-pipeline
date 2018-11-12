#!/usr/bin/env groovy
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.openshift.client.DefaultOpenShiftClient
import io.fabric8.openshift.client.OpenShiftClient
import org.junit.Assert
import org.junit.Test

class UtilTest extends PipelineHelper {

  @Test
  void should_merge_maps_with_different_keys() throws Exception {
    //GIVEN
    Script script = loadScript("utils/jenkinsfiles/mergemaps.jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "Different Keys : [a:[b], b:[d]])")
    assertJobStatusSuccess()
  }

  @Test
  void should_merge_maps_with_same_keys() throws Exception {
    //GIVEN
    Script script = loadScript("utils/jenkinsfiles/mergemaps.jenkinsfile")

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "Same Keys : [a:[b, d]])")
    assertJobStatusSuccess()
  }

  @Test
  void get_username_space() throws Exception {
    //GIVEN
    Script script = loadScript("utils/jenkinsfiles/usernamespace.jenkinsfile")
    OpenShiftClient oc = new DefaultOpenShiftClient(new ConfigBuilder().withNamespace("user-jenkins").build())
    binding.setVariable("oc", oc)

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "Namespace : user")
    assertJobStatusSuccess()
  }
}
