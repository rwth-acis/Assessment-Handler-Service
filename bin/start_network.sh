#!/bin/bash

# this script is autogenerated by 'gradle startscripts'
# it starts a las2peer node providing the service 'i5.las2peer.services.AssessmentHandler.AssessmentHandlerService' of this project
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh

java -cp "lib/*" --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED i5.las2peer.tools.L2pNodeLauncher --port 9011 --service-directory service uploadStartupDirectory startService\(\'i5.las2peer.services.AssessmentHandler.AssessmentHandlerService@1.0.0\'\,'assess') startWebConnector interactive
