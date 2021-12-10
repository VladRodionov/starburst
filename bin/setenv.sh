#!/usr/bin/env bash

# Yeah, let set individual JAVA_HOME in .bashrc ?
# JAVA_HOME variable could be set on stand alone not-dev server.
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home
export RELEASE=1.0
export DISTRIBUTION=textprovider-${RELEASE}.tar.gz
export APP_OPTS="-Dlog4j.configurationFile=conf/log4j2.xml"
