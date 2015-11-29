#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "saucelabs/jenkins-sauce-ondemand-plugin" ] && [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  git remote add jenkins https://${GH_TOKEN}@github.com/jenkinsci/sauce-ondemand-plugin
  git push -q jenkins master --tags >/dev/null
fi
