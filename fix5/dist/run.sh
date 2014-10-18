#!/bin/sh
CONFIG="fix.cfg"
USER="U000001"
PASS="12345"

APP_HOME=`dirname $0`

CLASSPATH="$CLASSPATH:$APP_HOME/sample-app.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/lib/nextfix.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/lib/mina-core-1.1.7.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/lib/slf4j-jdk14-1.6.3.jar"
CLASSPATH="$CLASSPATH:$APP_HOME/lib/slf4j-api-1.6.3.jar"

java -cp "$CLASSPATH" kz.kase.next.fix.SampleClient $CONFIG $USER $PASS
       