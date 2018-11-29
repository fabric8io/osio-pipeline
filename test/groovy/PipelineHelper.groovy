import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource


class PipelineHelper extends BasePipelineTest {

  @Override
  @Before
  void setUp() throws Exception {
    plugSharedPipelineLib()

    // its intended to call it later
    super.setUp()
  }

  def plugSharedPipelineLib() {
    String sharedLibs = this.class.getResource('./').getFile()

    def library = library()
      .name('osio-pipeline')
      .allowOverride(false)
      .retriever(localSource(sharedLibs))
      .targetPath(sharedLibs)
      .defaultVersion("master")
      .implicit(true)
      .build()
    helper.registerSharedLibrary(library)

    setScriptRoots(['src', 'vars', 'test/groovy'] as String[])
    setScriptExtension('groovy')
  }
}
