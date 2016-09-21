node('master') {
  // Mark the code checkout 'stage'....
  stage 'Checkout'
  // Get some code from a GitHub repository
  git url: 'https://github.com/saucelabs/jenkins-sauce-ondemand-plugin'
  // Clean any locally modified files and ensure we are actually on master
  // as a failed release could leave the local workspace ahead of master
  sh "git clean -f && git reset --hard origin/master"

  stage 'Build'
  def mvnHome = tool 'Maven'
  // Mark the code build 'stage'....
  // Run the maven build this is a release that keeps the development version
  // unchanged and uses Jenkins to provide the version number uniqueness
  withEnv(["GIT_COMMITTER_EMAIL=${env.BUILD_USER_EMAIL}","GIT_COMMITTER_NAME=${env.BUILD_USER}","GIT_AUTHOR_NAME=${env.BUILD_USER}","GIT_AUTHOR_EMAIL=${env.BUILD_USER_EMAIL}"]) {
      sh "${mvnHome}/bin/mvn -v"
      sh "${mvnHome}/bin/mvn clean verify package install -Dgpg.skip=true -B"
  }

  stage 'Archive Results'
  step([$class: 'ArtifactArchiver', artifacts: '**/target/*.hpi', fingerprint: true])

  stage 'Publish Results'
  step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

  stage 'Output XML - Temp'
  sh 'for i in target/surefire-reports/*; do echo $i; cat $i; done;'
  // stage 'Run codecov'
  // sh 'virtualenv .venv'
  // sh 'source .venv/bin/activate && pip install codecov && codecov'
}
