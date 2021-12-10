#!/usr/bin/env bash

START_HOME=$PWD
echo Server home directory is "${START_HOME}"

cd "${START_HOME}" || exit

. ./setenv.sh

libdir="${START_HOME}/../lib/${RELEASE}"
rm -rf "${libdir}"
mkdir -p "${libdir}"
cd "${libdir}" || exit 1
tar zxf "${START_HOME}/../dist/target/${DISTRIBUTION}" &>/dev/null
cd ../..
for ix in $(find "${libdir}"); do
  CPATH=${ix}\:${CPATH}
done

export JVM_OPTS="-cp .:${CPATH}"

#===== start server =====
start() {

  exec_cmd="java ${JVM_OPTS} org.bigbase.textprovider.Server ${APPS_PARAMS}"
  #echo "${exec_cmd}"
  mkdir -p logs
  nohup ${exec_cmd} >>logs/server-stdout.log &
  echo "Server instance is staring, please wait..."
  sleep 1
  echo "Server instance started"
  exit 0
}


#==== main =====
java -version
if [ $# -eq 0 ]
  then
    echo "Absolute path to a text file is expected. Aborting ..."
    exit 1
fi
APPS_PARAMS=$1
start
