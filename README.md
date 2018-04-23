This project contains the source code for the Jenkins/Hudson Sauce OnDemand plugin.

[![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/sauce-ondemand-plugin/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/sauce-ondemand-plugin/) [![codecov.io](https://codecov.io/github/saucelabs/jenkins-sauce-ondemand-plugin/coverage.svg?branch=master)](https://codecov.io/github/saucelabs/jenkins-sauce-ondemand-plugin?branch=master)

To build and unit test the plugin, execute:
	
	mvn package
	
To deploy a built version, execute

	mvn release:prepare release:perform

Defects/enhancements can be recorded at https://issues.jenkins-ci.org/browse/JENKINS-36053?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20sauce-ondemand-plugin

