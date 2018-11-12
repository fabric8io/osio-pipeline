#!/usr/bin/env groovy

import org.junit.Assert
import org.junit.Test

class CITest extends PipelineHelper {

  @Test
  void should_run_ci() throws Exception {
    //GIVEN
    Script script = loadScript("ci/jenkinsfiles/ci.jenkinsfile")
    binding.setProperty("env", ["BRANCH_NAME": "PR-123"])

    // WHEN
    runScript(script)


    // THEN
    assertStepExecutes("echo", "ci test")
    assertJobStatusSuccess()
  }

  @Test
  void should_run_ci_on_custom_branch() throws Exception {
    //GIVEN
    Script script = loadScript("ci/jenkinsfiles/ci.jenkinsfile")
    binding.setProperty("env", ["BRANCH_NAME": "trunc"])

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "ci test trunc")
    assertJobStatusSuccess()
  }

  @Test
  void should_not_run_ci_on_master_branch() throws Exception {
    //GIVEN
    Script script = loadScript("ci/jenkinsfiles/no-ci.jenkinsfile")
    binding.setProperty("env", ["BRANCH_NAME": "master"])

    // WHEN
    runScript(script)

    // THEN
    assertMethodNotCalled("echo")
    assertJobStatusSuccess()
  }
}
