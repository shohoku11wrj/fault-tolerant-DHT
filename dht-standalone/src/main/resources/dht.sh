#!/bin/bash
export JARFILE=${server.testdir}/${server.name}.jar
export POLICY=${server.testdir}/server.policy
export CODEBASE=${server.codebase}
export SERVERHOST=${server.host}

if [ ! -e $JARFILE ] ; then
	echo "Missing jar file: $JARFILE"
	echo "Please assemble the dht jar file."
	exit
fi

if [ ! -e $POLICY ] ; then
	pushd ${server.testdir}
	jar xf "$JARFILE" server.policy
	popd
fi

echo "Running server with CODEBASE=$CODEBASE and SERVERHOST=$SERVERHOST"
echo "java -Djava.security.policy=$POLICY -Djava.rmi.server.codebase=$CODEBASE -Djava.rmi.server.hostname=$SERVERHOST -jar $JARFILE --http $1 --id $2"
java -Djava.security.policy=$POLICY -Djava.rmi.server.codebase=$CODEBASE -Djava.rmi.server.hostname=$SERVERHOST -jar $JARFILE --http $1 --id $2