#!/bin/bash

# go to the directory of the script
cd "$(dirname "${BASH_SOURCE[0]}")"

rm -r "./bin/"

# Construct paths based on the script's location
classpath="./src"
output="./bin"
input="./src/checker/*"

javac -cp $classpath -d $output $input
