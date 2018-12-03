#!/usr/bin/env groovy

import org.junit.Assert
import org.junit.Test

class CDTest extends PipelineHelper {

  @Test
  void should_run_cd() throws Exception {
    //GIVEN
    Script script = loadScript("cd/jenkinsfiles/cd.jenkinsfile")
    binding.setProperty("env", ["BRANCH_NAME": "master"])

    // WHEN
    runScript(script)

    // THEN
    assertStepExecutes("echo", "cd test")
    assertJobStatusSuccess()
  }

  @Test
  void should_not_run_cd_on_master_branch() throws Exception {
    //GIVEN
    Script script = loadScript("cd/jenkinsfiles/no-cd.jenkinsfile")
    binding.setProperty("env", ["BRANCH_NAME": "pr-123"])

    // WHEN
    runScript(script)

    // THEN
    assertMethodNotCalled("echo")
    assertJobStatusSuccess()
  }
}
