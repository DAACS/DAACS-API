#! /bin/bash -e

rm -rf build
mkdir build
mkdir build/daacs
cp ../build/libs/daacsapi-*.jar build/daacs/daacsapi.jar
cp -r ../lightside build/

docker build -t daacsapi .
