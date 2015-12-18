CURRENT (1.145-SNAPSHOT)
=======
* Upgrade generating support zip to download directly to browser.
* Remove token-macro, copy-to-slave and conditional-buildstep unused dependancies
* Remove the sauce badge support as it doesn't make sense for jenkins unless you have 1 user per job
  * This may come back in the future when support works better
* Update the video and log file links to include auth tokens
* Don't worry about synchronization of the build step. Confirmed by - https://groups.google.com/forum/#!msg/jenkinsci-dev/nahS2YqEapQ/zR_pSOvyDAAJ
  * Should remove some blocking and consuming resources while waiting for large maven builds to finish
* Add option to report publisher to choose job visibility
