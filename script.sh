#!/bin/bash


#for ((i=0; i<$1 ;i++)) do
#    java -cp build/classes br.inf.ufes.ppd.application.Client teste teste 50000 ./dictionary.txt &
#done


#slave
java -cp .:../../glassfish5/glassfish/lib/gf-client.jar br.inf.ufes.ppd.application.SlaveServer ../../dictionary.txt 192.168.1.116


