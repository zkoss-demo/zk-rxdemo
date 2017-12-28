#!/bin/bash

./gradlew clean war

scp build/libs/zk-rxdemo.war zktest@zktest:servers/support3-tomcat/webapps