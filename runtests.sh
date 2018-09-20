#!/bin/sh

docker run -it -v $(pwd)/test/utilsTest:/workspace                 \
    -v $(pwd)/test/jenkinsfile-runner:/var/jenkinsfile-runner      \
    -v $(pwd)/test/jenkinsfile-runner-cache:/var/jenkinsfile-runner-cache \
    jenkins/jenkinsfile-runner