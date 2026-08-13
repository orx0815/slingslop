#!/bin/bash

if [[ $0 == *":\\"* ]]; then
  DISPLAY_PAUSE_MESSAGE=true
fi

mvn -D vault.unpack=true wcmio-content-package:download

if [ "$DISPLAY_PAUSE_MESSAGE" = true ]; then
  echo ""
  read -n1 -r -p "Press any key to continue..."
fi
