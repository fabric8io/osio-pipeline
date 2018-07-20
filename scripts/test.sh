#!/bin/bash
set -x
set -v
set -e
set -u

TARGET_USER=${PROJECT_NAME/-jenkins/}
BOOSTER_TEST_REPO="https://github.com/chmouel/nodejs-health-check"
BOOSTER_TEST_BRANCH="master"
STAGES="['run']"

APPLICATION_NAME=$(basename ${BOOSTER_TEST_REPO})
APPLICATION_YAML=https://raw.githubusercontent.com/$(expr "${BOOSTER_TEST_REPO}" : '.*/\([^/]*/[^/]*\)$')/master/.openshiftio/application.yaml

TMPFILE=$(mktemp /tmp/.osioev.XXXXXX)
cleanup(){ rm -f ${TMPFILE} ;}
trap cleanup EXIT

curl -s -L -o${TMPFILE}  ${APPLICATION_YAML}

echo $JENKINSFILE
