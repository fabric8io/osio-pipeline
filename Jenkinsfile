#!/usr/bin/groovy
// pipeline {
//   agent any

//   stages {
//     stage('Build') {
//       steps {
//         library identifier: "osio-pipeline@${env.BRANCH_NAME}", retriever: workspaceRetriever("${WORKSPACE}")
//         sh(script: 'scripts/test.sh')
//       }
//     }
//   }
// }
node {
  checkout scm
  library identifier: "syndesis-pipeline-library@${env.BRANCH_NAME}", retriever: workspaceRetriever("${WORKSPACE}")
}
