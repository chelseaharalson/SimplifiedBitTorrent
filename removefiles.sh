#!/bin/bash

shopt -s extglob

rm -R -- */

## Delete all files except image and java class
rm !(*.java|*.class|image2.jpg|removefiles.sh)