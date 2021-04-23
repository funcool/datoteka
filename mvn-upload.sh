#!/bin/sh
mvn deploy:deploy-file -Dfile=target/datoteka.jar -DpomFile=pom.xml -DrepositoryId=clojars -Durl=https://clojars.org/repo/
