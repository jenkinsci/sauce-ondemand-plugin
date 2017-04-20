# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  #config.vm.define "windows" do |box|
  #  box.vm.box = "ferventcoder/win7pro-x64-nocm-lite"
  #end

  box = config
  #config.vm.define "linux", primary: true do |box|
    box.vm.box = "ubuntu/trusty64"
    #box.vm.network "forwarded_port", guest: 3322, host: 22
    box.vm.provision "shell", inline: <<-SHELL
      sudo apt-get update
      sudo apt-get install -y default-jre git maven phantomjs
      id jenkins || sudo useradd -m -p jenkins -s /bin/bash jenkins
      sudo -u jenkins bash -c 'mkdir /home/jenkins/.ssh'
      sudo -u jenkins bash -c 'echo ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCte+WtnndnTIlxLEHh8H4Y7v39X+SNS8MSMp32YWFMyq4juznzyewcpGaGr/cVRB7yVLjnhrl52b7Z/BO7PyfznJ4tnIENlHKYh0WAKfYxkxbFNm+QwAP61qYTx/k41PQWjfJ6n+RrN4lZT/6BiZqPn2gsvdWDMc4M/9lc222aTc696WH2+mSDTQfJrGCAZBYVXAGHZfe9zeBx3lWrFHNIn5WRBWWSLlg5g4IBiHsGgdyMj0+FCIcIp96XCCUX2MxsNlzy8xWv3gJ/Q+p5gpIQXnyYKOCXx436LiLk3Xj64dNgGhHPC64uzDL09tcSFLgg2xh4AY7r73oJUjmE9vOd vagrant@vagrant-ubuntu-trusty-64 | tee /home/jenkins/.ssh/authorized_keys'
      sudo -u jenkins bash -c 'chmod 0600 /home/jenkins/.ssh/authorized_keys'
    SHELL
  #end

end
