# Contributing to `jenkins-sauce-ondemand-plugin`

**Thank you for your interest in `jenkins-sauce-ondemand-plugin`. Your contributions are highly welcome.**

There are multiple ways of getting involved:

- [Report a bug](#report-a-bug)
- [Suggest a feature](#suggest-a-feature)
- [Contribute code](#contribute-code)

Below are a few guidelines we would like you to follow.
If you need help, please reach out to us by opening an issue.

## Report a bug
Reporting bugs is one of the best ways to contribute. Before creating a bug report, please check that an [issue](/issues) reporting the same problem does not already exist. If there is such an issue, you may add your information as a comment.

To report a new bug, you should open an issue that summarizes the bug and set the label to "bug".

If you want to provide a fix along with your bug report: That is great! In this case please send us a pull request as described in section [Contribute Code](#contribute-code).

## Suggest a feature
To request a new feature you should open an [issue](../../issues/new) and summarize the desired functionality and its use case. Set the issue label to "feature".

## Contribute code
This is an outline of what the workflow for code contributions looks like:

- Check the list of open [issues](../../issues). Either assign an existing issue to yourself, or
create a new one that you would like work on and discuss your ideas and use cases.

It is always best to discuss your plans beforehand, to ensure that your contribution is in line with our goals.

- Fork the repository on GitHub.
- Create a topic branch from where you want to base your work. This is usually master.
- Open a new pull request, label it `work in progress` and outline what you will be contributing.
- Make commits of logical units.
- Make sure you sign-off on your commits `git commit -s -m "adding X to change Y"`.
- Write good commit messages (see below).
- Push your changes to a topic branch in your fork of the repository.
- As you push your changes, update the pull request with new information and tasks as you complete them.
- Project maintainers might comment on your work as you progress.
- When you are done, remove the `work in progress` label and assign one of the maintainers to review.

## Run Locally	

### Prerequisites

_Java_ and _Maven_

- Ensure Java 8 or 11 is available.	

  ```console	
  $ java -version	
  ```	
  - Use the alternate Java 8.	

  ```console	
  $ export JAVA_HOME=`/usr/libexec/java_home -v 1.8`	
  $ echo $JAVA_HOME	
  /Library/Java/JavaVirtualMachines/jdk1.8.0_252.jdk/Contents/Home
  ```	

- Ensure Maven > 3.6.0 is installed and included in the PATH environment variable.	

  ```console
  $ mvn --version	
  ```	

### Check out code

To get the code base, have [git](https://git-scm.com/downloads) installed and run:

```sh
$ git clone git@github.com:saucelabs/jenkins-sauce-ondemand-plugin.git
```

### IDE configuration	

See [IDE configuration](https://jenkins.io/doc/developer/development-environment/ide-configuration/).

### CLI	

To run tests:

```console
$ mvn test
```

To spin up a Jenkins server with your plugin and test manually:

```console	
$ mvn hpi:run	
```	
and wait for:
```text	
...	
INFO: Jenkins is fully up and running	
```	
to appear.

- Open <http://localhost:8080/jenkins/> to test the plugin locally.

Refer to the [Sauce Labs Jenkins Integration doc](https://docs.saucelabs.com/ci/jenkins/index.html) for instructions on how to create a job that uses this plugin.

---

**Have fun, and happy hacking!**

Thanks for your contributions!