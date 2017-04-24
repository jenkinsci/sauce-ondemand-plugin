# Jenkins 1.164 - 2017-04-24

* Updating to ci-sauce 1.128 which brings in sauce-connect 4.4.6

# Jenkins 1.162 && 1.163

* Bad release and rollback

# Jenkins 1.161 - 2017-03-03

* Added new standard SAUCE_BUILD_NAME environment variable
* Added new feature for Self-Expiring Access keys
* Added support to track usage of Sauce Labs Credentials on credentials pages
* Upgraded to latest ci-sauce dependancy (1.124)
	* Updated to latest Sauce Connect Proxy (4.4.3)
	* Remove selenium dependancy
	* Re-write browser selection api to not use selenium and return more accurate results
* Add support to export sauce job results in the rest api for upcoming blueocean support

# Jenkins 1.160 - 2017-01-27

* Make parameters for publish sauce labs test results pipeline step optional - https://issues.jenkins-ci.org/browse/JENKINS-37610
* Also make a helper symbol of saucePublisher()
* Make parameters for sauceconnect pipeline step optional - https://issues.jenkins-ci.org/browse/JENKINS-41236

# Jenkins 1.159 - 2016-12-14

update to upstream sauce connect - 4.4.2

# Jenkins 1.158 - 2016-11-10

update to upstream sauce connect - 4.4.1

# Jenkins 1.157 - 2016-10-24

sauce connect pipeline step now closes sauce connect properly on slaves - https://issues.jenkins-ci.org/browse/JENKINS-38128

# Version 1.154 - 2016-06-24

Fixed NullPointer exception thrown in build stage publish results.

# Version 1.152 - 2016-05-11

Removed storing log files internally for parsing, should fix a bunch of memory related issues

# Version 1.151 - 2016-04-13

* Minimum required Jenkins version now bumped up to 1.609.2
* Added beta support for Jenkins Pipeline by creating two new blocks:
* The sauce {} block enables sauce credential variables
* The sauceconnect {} block (inside of sauce) handles the starting and stopping sauce connect
* Adding link to test results page in sauce directly from Jenkins sauce results page

# Version 1.150 - 2016-03-28

* Updated supplied sauce connect - 4.3.14

# Version 1.149 - 2016-01-26

Fix for class casting exception when using pipelining - https://github.com/jenkinsci/sauce-ondemand-plugin/pull/14

# Version 1.148 - 2016-01-21

* [Jenkins Credentials](https://wiki.jenkins-ci.org/display/JENKINS/Credentials+Plugin) Support
* Minor cleanups to reduce dependencies internally (dead code, should have no functionality change)

# Version 1.147 - 2015-12-11

* Output environment prefixes when verbose logging of environment variables
* Revert and re-add sauce parameterized build support
* Update sauce connect

# Version 1.146 - 2015-11-25

* Added Permissions (Job Configure) to be able to generate jenkins support zip for sauce labs.

# Version 1.145 - 2015-11-24 ([Less Technical Release Notes](https://support.saucelabs.com/customer/portal/articles/2233975-jenkins-plugin-v-1-145-now-available))

* Upgrade generating support zip to download directly to browser.
* Remove token-macro, copy-to-slave and conditional-buildstep unused dependencies
* Remove the sauce badge support as it doesn't make sense for jenkins unless you have 1 user per job
* This may come back in the future when support works better
* Update the video and log file links to include auth tokens
* Don't worry about synchronization of the build step. Confirmed by - https://groups.google.com/forum/#!msg/jenkinsci-dev/nahS2YqEapQ/zR_pSOvyDAAJ|https://groups.google.com/forum/#!msg/jenkinsci-dev/nahS2YqEapQ/zR_pSOvyDAAJ
* Should remove some blocking and consuming resources while waiting for large maven builds to finish
* Add option to report publisher to choose job visibility

# Version 1.144 - 2015-11-13

* FIx for NPE when running sauce connect on slaves (JENKINS-31529)
* Version 1.143 - 2015-11-9
* Bump sauce connect version to latest upstream (4.3.12)
* Fix "Send build usage data to Sauce Labs" saving/loading and move it to global configs
* Update job security level from public to share

# Version 1.142 - 2015-10-20

* Add in `SAUCE_USERNAME` env variable for consistency with the rest of examples / documentation
* Add in `SAUCE_ACCESS_KEY` env variable for consistency with the rest of examples / documentation
* Jenkins when managing tunnel identifiers will generate a new port as well to prevent collisions

# Version 1.134

* Ensure that selected browser is displayed for matrix projects

# Version 1.133

* JENKINS-29421 Resolve NullPointerException

# Version 1.132

* Include Sauce Connect 4.3.10

# Version 1.131

* Resolve test connection error
* Add ability to auto generate tunnel identifier

# Version 1.130

* Updated multi-select lists to use Chosen (https://github.com/harvesthq/chosen)
* Removed SeleniumRC related references
* Include Sauce Connect 4.3.9
* List of Sauce jobs moved to bottom of Jenkins project page and now includes Name, OS/Browser/Version, Pass/Fail, Link to video/log
* Jenkins project page now includes a 'Generate support zip' link which will create a zip file of the Sauce Connect log and Jenkins build output
* Jenkins build output now includes log lines to indicate when plugin has started/finished processing
* Included fixes for JENKINS-29047 and JENKINS-29119

# Version 1.129

* Display a warning message if builds are run with Sauce Connect v3. Support for Sauce Connect v3 is scheduled to cease on August 19 2015 (Sauce Connect 4 should be used instead).

# Version 1.128

* Ensure that all tunnels are closed when pidfile locked error occurs

# Version 1.128

* Ensure that all tunnels are closed when pidfile locked error occurs

# Version 1.127

* Use latest Sauce REST library, which ensures that valid SSL protocol is used

# Version 1.126

* Set the platform to iOS for Appium iOS devices

# Version 1.125

* Attempt to relaunch Sauce Connect if pidfile has a lock

# Version 1.124

* Fix error when Sauce Connect path was empty string

# Version 1.123

* Support supplying the path to an existing Sauce Connect installation, and include Sauce Connect 4.3.8

# Version 1.122

* Resolve NullPointerException when processing results with no test data

# Version 1.121

* Process stdout lines when no test publisher has been defined

# Version 1.120

* Use Sauce Connect 4.3.7

# Version 1.119

* Resolve NullPointerException in result parsing

# Version 1.118

* Include fix for JENKINS-25236

# Version 1.117

* Lock startup of Sauce Connect based on tunnel identifier

# Version 1.116

* Add ability to specify a prefix for the SELENIUM_* environment variables
* fix JENKINS-26662

# Version 1.115

* Include 32-bit Linux Sauce Connect

# Version 1.114

* Query Sauce REST API to see if there are any active tunnels before launching new tunnel

# Version 1.113

* Include fixes for JENKINS-25237, JENKINS-25756, JENKINS-25236

# Version 1.112

* Include 32-bit Linux version of Sauce Connect

# Version 1.111

* Restore platform to SAUCE_ONDEMAND_BROWSERS environment variable

# Version 1.110

* Append 'Simulator' to iOS device names and include Sauce Connect 4.3.6

# Version 1.109

* Retrieve supported Appium browsers from Sauce REST API

# Version 1.108

* Include device attribute values for multi configuration projects

# Version 1.107

* Don't display the Sauce badge if unable to connect to Sauce Labs

# Version 1.106

* Include Sauce Connect 4.3.5.

# Version 1.105

* Add latest iOS and Android versions to Appium devices.

# Version 1.104

* Make Sauce Test Publisher available to all build types

# Version 1.103

* Allow users to specify run conditions for Sauce Connect

# Version 1.102

* Supply browser's long name as part of SAUCE_ONDEMAND_BROWSERS environment variable

# Version 1.101

* Supply short and long version in SAUCE_ONDEMAND_BROWSERS environment variable

# Version 1.100

* Support retrieving latest version of browser

# Version 1.99

* Support portrait and landscape device orientation

# Version 1.97

* Removed sauceConnectHandler instance variable

# Version 1.93

* The plugin will treat Sauce Connect startup timeouts as errors, which will cause the build to fail (previously these errors were only being logged). Verbose logging of Sauce Connect is now enabled by default.

# Version 1.92

* Updated plugin to use Sauce Connect version 4.3

# Version 1.90

* Use the Suite Result stdout and stderr in addition to the Case Result

# Version 1.89

* Added a 'Verbose logging' option to the Job Configuration page, which will enabled the Sauce Connect output to be included with the job console output

# Version 1.88

* Fix issue with selection of 'Use Sauce Connect v3' option not saving

# Version 1.87

* The default behaviour of launching Sauce Connect is now to launch on the Slave node instead of the Master node.

# Version 1.86

* Recompiled linux build of Sauce Connect

# Version 1.85

* Updated plugin to use Sauce Connect version 4.2

# Version 1.84

* Use correct directory for Sauce Connect running under Windows

# Version 1.83

* Use Jenkins proxy information to retrieve list of supported browsers from Sauce Labs

# Version 1.80

* Use latest version of Sauce Connect (both v4 and v3)

# Version 1.74

* Add support for Sauce Connect version 4

# Version 1.68

* Use Jenkins proxy settings when retrieving list of supported browsers from Sauce Labs

# Version 1.67

* Only display the Sauce badge for the applicable build. Set the pass/fail status based on the test result, not the Jenkins build

# Version 1.66

* Performance improvements for parsing and updating jobs

# Version 1.65

* Provide support for specifying Sauce build parameters. Use Jenkins proxy settings when calling Sauce REST API. Resized embedded Sauce report.

# Version 1.64

* Added some extra logging (no new functionality)

# Version 1.63

* Added some extra logic to ensure that Sauce reports are displayed within project/build result pages

# Version 1.62

* Attempt to use username and access key set at main config if no build action available

# Version 1.61

* Return a report if the list if IDs is found without needing the REST API

# Version 1.60

* Use proxy settings set within Jenkins configuration when performing calls to Sauce REST API

# Version 1.59

* Only update metadata for Sauce Jobs if no data already exists on job

# Version 1.58

* Ensure that pass/fail status is only set if it hasn't already been set

# Version 1.57

* Bumped ci-sauce version

# Version 1.56

* Included updates to UI labels
* Bumped ci-sauce version to fix https protocol issues

# Version 1.55

* Resolve NullPointerException error

# Version 1.54

* Set the pass/fail status based on the build status

# Version 1.53

* Set pass/fail status for all jobs parsed by the plugin

# Version 1.52

* Ensure that logParserMap is not null when decorating logger

# Version 1.51

* JENKINS-17303 Resolve NPE when navigating to non-Sauce projects

# Version 1.50

* Handle null logParser

# Version 1.49

* Use latest version of Sauce Connect

# Version 1.48

* Added Sauce job summary table to project page. Pass through command options to Sauce Connect

# Version 1.47

* Handle null browser when outputting browser JSON

# Version 1.46

* Use latest version of Sauce Connect

# Version 1.45

* Handle null browser

# Version 1.44

* Fixed errors with test connection function

# Version 1.43

* Ensure that spaces aren't used for directories created by multi-config projects

# Version 1.42

* Include Sauce build status badges in Jenkins dashboard list

# Version 1.41

* Updated logic which compared test name with Sauce job

# Version 1.40

* Ensure log parsing is thread safe

# Version 1.39

* Add name column to results table

# Version 1.38

* Use raw platform string as SELENIUM_PLATFORM environment variable

# Version 1.37

* Store build variables set by multi configuration projects as environment variables

# Version 1.36

* Include username and access key in SELENIUM_DRIVER environment variable

# Version 1.35

* Handle invalid characters as part of the base64 encoding of the Sauce REST authentication

# Version 1.34

* For Maven Jenkins projects, store the parent's build number against the Sauce Job using the Sauce REST API

# Version 1.33

* Remove non-alphanumeric characters from the build number stored against the Sauce Job

# Version 1.32

* Invoke the Sauce REST API to find jobs run against a specific build number when displaying the embedded Job results

# Version 1.31

* Included fix for JENKINS-15793

# Version 1.30

* Fall back to using InetAddress.getLocalHost() if Computer.currentComputer().getHostName() returns null when storing the SELENIUM_HOST environment variable

# Version 1.29

* Changed Jenkins parent version to 1.439 to cater for Cloudbees Jenkins version

# Version 1.28

* Included fix for JENKINS-15764

# Version 1.27

* Included ability to display links to Sauce jobs on the build summary page

# Version 1.26

* Additional fix for JENKINS-15261

# Version 1.25

* Additional fix for JENKINS-15261

# Version 1.24

* Resolved JENKINS-15261

# Version 1.23

* Resolved JENKINS-15326, JENKINS-15261 and JENKINS-15330

# Version 1.22

* Changed code that handles displaying the embedded Sauce OnDemand results to reference https://saucelabs.com

# Version 1.21

* Updated logic used to close Sauce Connect process

# Version 1.20

* Resolved JENKINS-14592

# Version 1.19

* Resolved JENKINS-14577 and JENKINS-14592

# Version 1.18

* Resolved JENKINS-14492

# Version 1.17

* Restored Jenkins version dependency to 1.439

# Version 1.16

* Made variables used by log parser transient so that serialisation errors are resolved

# Version 1.15

* Included an option to specify whether to run Sauce Connect on the Slave or Master node

# Version 1.14

* Resolved JENKINS-13794, JENKINS-13798 and JENKINS-13796

# Version 1.13

* Resolved JENKINS-13207

# Version 1.12

* Set the working directory when invoking Sauce Connect. The working directory can be set via the Adminstration interface, and will default to the user home directory if not set.

# Version 1.11

* Updated plugin to provide better support for multi-configuration jobs. The plugin will now launch a single instance of Sauce Connect per user. That is, if a multi-configuration Job is configured to run using multiple browsers, then only one Sauce Connect instance will be launched for the Jobs.
* The plugin now also allows the user to specify the browser(s) to be used on the Job level.  If multiple browsers are selected, then they will be included in JSON format in a SAUCE_ONDEMAND_BROWSERS environment variable
* Resolved JENKINS-12880 and JENKINS-12741

# Version 1.10

* Resolved JENKINS-7828, JENKINS-12321 and JENKINS-12570

# Version 1.9

* Resolved JENKINS-12468 and JENKINS-12320

# Version 1.8

* Rebuilt plugin to include a missing dependency

# Version 1.7

* Updated plugin to reference Sauce CI library, which is logic common across the Jenkins and Bamboo plugins

# Version 1.6

* Integrated Sauce Connect 2 into the plugin
* The plugin will also perform a check to see if a later version of Sauce Connect is available, which can be updated via the Administration interface

# Version 1.5

* Replaced video and server logs with embedded Sauce OnDemand reports.
* Upload job information after the build (job name, build number, pass/fail status). This requires selenium-client-factory 1.3.

# Version 1.4 (Oct 1, 2010)
* Extended the time out (until the tunnel endpoint comes up) from 3 minutes to 5 minutes.

# Version 1.3 (Sep 1, 2010)

* Added a custom axis for the matrix projects to choose different browsers. This works in combination with selenium client factory.
* You can now capture video and server logs into Hudson and watch them from within Hudson

# Version 1.2

* This plugin enables Hudson to manage SSH tunnels.
