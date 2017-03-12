#!/bin/bash
cd apt-graph/
mvn clean install
cd ../batch/
./analyze.sh -i ./src/main/resources/12_3_site_3_time_1_APT_ad.txt -o /tmp/mytemp/
cd ../server/
./start.sh -i /tmp/mytemp/