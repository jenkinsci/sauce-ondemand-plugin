node('master') {
  stage 'Build'
  def mvnHome = tool 'Maven'
  // we want to pick up the version from the pom
  def pom = readMavenPom file: 'pom.xml'
  def version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}")
  // Mark the code build 'stage'....
  // Run the maven build this is a release that keeps the development version
  // unchanged and uses Jenkins to provide the version number uniqueness
  withEnv(["GIT_COMMITTER_EMAIL=${env.BUILD_USER_EMAIL}","GIT_COMMITTER_NAME=${env.BUILD_USER}","GIT_AUTHOR_NAME=${env.BUILD_USER}","GIT_AUTHOR_EMAIL=${env.BUILD_USER_EMAIL}"]) {
      sh "${mvnHome}/bin/mvn -v"
      sh "${mvnHome}/bin/mvn clean verify package install -Dgpg.skip=true -B"
  }
  // Archive build result
  archive './target/*.jar'

  stage 'Publish Results'
  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

  // stage 'Run codecov'
  // sh 'virtualenv .venv'
  // sh 'source .venv/bin/activate && pip install codecov && codecov'
}
