This project contains the source code for the Jenkins/Hudson Sauce OnDemand plugin.

[![Build Status](https://jenkins.ci.cloudbees.com/job/plugins/job/sauce-ondemand-plugin/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/sauce-ondemand-plugin/)

To build and unit test the plugin, execute:
	
	mvn package
	
To deploy a built version, execute

	mvn release:prepare release:perform

Defects/enhancements can be recorded at https://issues.jenkins-ci.org/browse/JENKINS/component/15751
