#!groovy

/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])

node ('linux') {
  stage('Checkout') {
    checkout scm
  }

  stage('Build') {
    def mvnHome = tool 'mvn'
    env.JAVA_HOME = tool 'jdk8'
    sh "${mvnHome}/bin/mvn clean install -B -V -U -e -Dsurefire.useFile=false -Dmaven.test.failure.ignore=true -Dgpg.skip=true"
  }

  stage('Archive Results') {
    step([$class: 'ArtifactArchiver', artifacts: 'target/*.hpi,target/*.jpi'])
  }

  stage('Publish Results') {
    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
  }

}

