#!/bin/bash
###############################################################################
# File        : runnetworktransferservice.sh
# Description : This script will start the network transfer screen capture server
#
#
# Return      : 0 = Success
#                   0!= Fail
#
# Author      : Kevin Jin
#
# Created     : 07/25/2023
#
# Changes     | Description
# ------------+-----------------------------------------------------------------
#             |
#             |
################################################################################

export JVM_PATH="/usr/java64/latest/bin/java"
export JVM_DEBUG_OPTIONS=""


export OD_VERSION="1.10.5"

exec ${JVM_PATH} $JVM_DEBUG_OPTIONS  -Xms256m -Xmx256m -XX:MaxPermSize=256m \

  -jar /usr/g/sdapplications/networktransferservice/network-transfer-service-rest-${OD_VERSION}.jar \
  >> /usr/g/service/log/ctdesktoplogs/networktransferservice-`date +'%d'`-clarity.out 2>&1 


