import static io.openshift.Utils.usersNamespace

// This Api was added so that we do not expose the Utils API to the users direclty on Jenkinsfile
// Utils purpose is to be used by API's present in the osio pipeline library.
// To run the integration test we are executing them in usersNamespace for Java boosters.

def call() {
  return usersNamespace()
}
