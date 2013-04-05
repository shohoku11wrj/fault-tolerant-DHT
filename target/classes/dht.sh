#!/bin/bash
export JARFILE=/home/ranger/tmp/cs549/dht-test/dht.jar
export POLICY=/home/ranger/tmp/cs549/dht-test/server.policy
export CODEBASE=file:/home/ranger/tmp/cs549/dht-test/dht.jar
export SERVERHOST=localhost

if [ ! -e $JARFILE ] ; then
	echo "Missing jar file: $JARFILE"
	echo "Please assemble the dht jar file."
	exit
fi

if [ ! -e $POLICY ] ; then
	pushd /home/ranger/tmp/cs549/dht-test
	jar xf "$JARFILE" server.policy
	popd
fi

echo "Running server with CODEBASE=$CODEBASE and SERVERHOST=$SERVERHOST"
echo "java -Djava.security.policy=$POLICY -Djava.rmi.server.codebase=$CODEBASE -Djava.rmi.server.hostname=$SERVERHOST -jar $JARFILE --http $1 --id $2"
java -Djava.security.policy=$POLICY -Djava.rmi.server.codebase=$CODEBASE -Djava.rmi.server.hostname=$SERVERHOST -jar $JARFILE --http $1 --id $2