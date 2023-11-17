FROM jenkins/jenkins:lts-jdk11
# if we want to install via apt
USER root
RUN apt-get update && apt-get install -y maven
# Plugins to avoid checking the proxy certificate and to install plugin dependecies Sauce
# OnDemand plugin relies on
RUN jenkins-plugin-cli --plugins skip-certificate-check workflow-job workflow-basic-steps \
      workflow-cps matrix-project run-condition workflow-step-api junit jackson2-api \
      credentials maven-plugin
# drop back to the regular jenkins user - good practice
USER jenkins
