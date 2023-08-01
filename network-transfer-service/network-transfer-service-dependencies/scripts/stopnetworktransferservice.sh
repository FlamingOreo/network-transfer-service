#!/bin/bash
###############################################################################
# File        : runnetworktransferservice.sh
# Description : This script will stop the network transfer service screen capture server
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

pid=`ps aux | grep network-transfer-service-rest | grep -v grep | awk '{print $2}'`
if [ -n "$pid" ]
     then
     sudo kill -9 $pid
fi
