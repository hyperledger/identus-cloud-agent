#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

Help()
{
   # Display Help
   echo "Run an instance of the ATALA bulding-block stack locally"
   echo
   echo "Syntax: run.sh [-n/--name NAME|-d/--destroy-volumes|-h/--help]"
   echo "options:"
   echo "-n/--name              Name of this instance - defaults to dev."
   echo "-d/--destroy-volumes   Instruct docker-compose to tear down volumes."
   echo "-h/--help              Print this help text."
   echo
}

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    -n|--name)
      NAME="$2"
      shift # past argument
      shift # past value
      ;;
    -h|--help)
      Help
      exit
      ;;
    -d|--destroy-volumes)
      VOLUMES="-v"
      shift # past argument
      ;;
    -*|--*)
      echo "Unknown option $1"
      Help
      exit 1
      ;;
    *)
      POSITIONAL_ARGS+=("$1") # save positional arg
      shift # past argument
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters


if [[ -n $1 ]]; then
    echo "Last line of file specified as non-opt/last argument:"
    tail -1 "$1"
fi

NAME="${NAME:=local}"
PORT="${PORT:=80}"

echo "NAME            = ${NAME}"

echo "--------------------------------------"
echo "Stopping stack using docker compose"
echo "--------------------------------------"

PORT=${PORT} docker compose \
  -p ${NAME} \
  -f ${SCRIPT_DIR}/../shared/docker-compose.yml \
  --env-file ${SCRIPT_DIR}/.env down ${VOLUMES}
