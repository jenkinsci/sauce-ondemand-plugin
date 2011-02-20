set SELENIUM_DRIVER=sauce-ondemand:?os=Linux^&browser=firefox^&browser-version=3.
mvn clean test -Dmaven.test.failure.ignore
