# Contributing to `jenkins-sauce-ondemand-plugin`

**Thank you for your interest in `jenkins-sauce-ondemand-plugin`. Your contributions are highly
welcome.**

There are multiple ways of getting involved:

- [Report a bug](#report-a-bug)
- [Suggest a feature](#suggest-a-feature)
- [Contribute code](#contribute-code)

Below are a few guidelines we would like you to follow.
If you need help, please reach out to us by opening an issue.

## Report a bug

Reporting bugs is one of the best ways to contribute. Before creating a bug report, please check
that an [issue](/issues) reporting the same problem does not already exist. If there is such an
issue, you may add your information as a comment.

To report a new bug, you should open an issue that summarizes the bug and set the label to "bug".

If you want to provide a fix along with your bug report: That is great! In this case please send us
a pull request as described in section [Contribute Code](#contribute-code).

## Suggest a feature

To request a new feature you should open an [issue](../../issues/new) and summarize the desired
functionality and its use case. Set the issue label to "feature".

## Contribute code

This is an outline of what the workflow for code contributions looks like:

- Check the list of open [issues](../../issues). Either assign an existing issue to yourself, or
  create a new one that you would like work on and discuss your ideas and use cases.

It is always best to discuss your plans beforehand, to ensure that your contribution is in line with
our goals.

- Fork the repository on GitHub.
- Create a topic branch from where you want to base your work. This is usually master.
- Open a new pull request, label it `work in progress` and outline what you will be contributing.
- Make commits of logical units.
- Make sure you sign-off on your commits `git commit -s -m "adding X to change Y"`.
- Write good commit messages (see below).
- Push your changes to a topic branch in your fork of the repository.
- As you push your changes, update the pull request with new information and tasks as you complete
  them.
- Project maintainers might comment on your work as you progress.
- When you are done, remove the `work in progress` label and assign one of the maintainers to
  review.

## Run Locally

### Prerequisites

_Java_ and _Maven_

- Ensure Java 8 or 11 is available.

  ```console
  $ java -version
  ```
  - Use the alternate Java 11.

  ```console
  $ export JAVA_HOME=`/usr/libexec/java_home -v 11`
  $ echo $JAVA_HOME
  /Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home
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

Refer to the [Sauce Labs Jenkins Integration doc](https://docs.saucelabs.com/ci/jenkins/index.html)
for instructions on how to create a job that uses this plugin.

### Testing

While `mvn hpi:run` spins up a Jenkins server with your plugin, other things need a more complex
setup to be tested. For example, testing everything through a proxy. For this, we will use Docker
and Docker Compose to spin a Jenkins container, together with a proxy.

To start, generate the plugin `.hpi` file:

```console
$ mvn clean install -U -DskipTests=true -Dgpg.skip
```

The file will be generated in the `target` folder. After that, copy the file to the `src/test`
directory:

```console
$ cp target/sauce-ondemand.hpi src/test/
```

Then, go to the `src/test` directory and start the Docker Compose stack:

```console
$ cd src/test
$ docker-compose -f docker-compose-test.yml up --build
```

To understand the setup, check the comments in the
[`docker-compose-test.yml`](./src/test/docker-compose-test.yml) file.

After the stack is up, open http://localhost:7900/?autoconnect=1&resize=scale in a browser.
Right-click on the desktop, and go to `Applications > Shells > Bash`, then type `firefox` and hit
enter. On the remote browser, open http://jenkins:8080/jenkins/.

Start configuring Jenkins, grab the Admin password from the terminal where you started the stack,
use the noVNC menu on the left to paste content on the clipboard and make it available on the
remote browser, and paste the password on the Jenkins setup screen.

Since the Jenkins container has no access to the internet, the UI will offer you to configure a
proxy. Use `forwarder` for the server and `3128` for the port. After that, you can install the
recommended plugins.

When the plugins installation is done, create an admin user. Then confirm the instance
configuration. Once this has completed, restart the Jenkins instance to get the proxy
configuration working system-wide. To restart, stop the docker compose stack and start it again.

When the stack is up again, follow the same steps described above to open the Jenkins UI on the
remote browser. After that, follow these instructions to setup the Sauce OnDemand plugin:

- Deploy manually de Sauce OnDemand plugin:
    - Go to `Manage Jenkins > Plugins > Advanced`
    - On `Deploy Plugin`, click on `Browse` and upload the `sauce-ondemand.hpi` file
    - Restart Jenkins
- Credentials configuration:
    - Go
      to `Manage Jenkins > Credentials > System > Global credentials (unrestricted) > Add Credentials`
    - Select `Sauce Labs` as the kind
    - Fill in the username and access key fields with your Sauce Labs credentials
    - Click on `OK`
- Set up a freestyle job:
    - Go to `Dashboard > New Item`
    - Enter a name for the job
    - Select `Freestyle project`
    - Click on `OK`
    - Select `Git` under `Source Code Management` and use https://github.com/alexh-sauce/demo-java
    - Under `Branches to build`, enter `*/main`
    - Under `Build Environment`, select `Sauce Labs Support`
        - Select `Enable Sauce Connect`
        - Select `Clean up jobs and uniquely generated tunnels`
    - Under `Sauce Connect Advanced options`, select `Advanced`
        - Select `Create a new unique Sauce Connect tunnel per build`
        - Add `--proxy forwarder:3128 --proxy-tunnel` to `Sauce Connect Options`
    - Add `Build > Execute shell` and enter (proxy is needed, otherwise maven will fail to download
      dependencies)
    - ```console
      mvn dependency:resolve -Dhttp.proxyHost=forwarder -Dhttp.proxyPort=3128 -Dhttps.proxyHost=forwarder -Dhttps.proxyPort=3128
      mvn test-compile -Dhttp.proxyHost=forwarder -Dhttp.proxyPort=3128 -Dhttps.proxyHost=forwarder -Dhttps.proxyPort=3128
      mvn test -pl best-practice -Dtest=DesktopTests -Dhttp.proxyHost=forwarder -Dhttp.proxyPort=3128 -Dhttps.proxyHost=forwarder -Dhttps.proxyPort=3128
      ```
    - Add a `Post Build Action > Run Sauce Labs Test Publisher`
    - Click on `Save`
- Go to `Dashboard` and click on the job you just created to trigger a build.

To start over with a clean configuration, stop the stack and remove the `test_jenkins_home` volume:

```console
$ docker volume rm test_jenkins_home
```

To iterate on the plugin development, delete the `sauce-ondemand.hpi` file from the `src/test`,
make your code changes, and repeat the steps to generate the `.hpi` file and copy it again
to `src/test`.

---

**Have fun, and happy hacking!**

Thanks for your contributions!
