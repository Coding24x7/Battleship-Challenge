#!/usr/bin/env bash
################################################################################
# Read input options

usage()
{
	echo "usage: ./run.sh <PORT> <SQLITE_DATABASE_FILE>"
	echo
}

if [ $# -ne 2 ]; then
	usage
	exit 1
fi

java -jar target/battleship-0.1.0.jar --server.port=$1 --spring.datasource.url=jdbc:sqlite:$2
