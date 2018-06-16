#!/bin/bash

for ((i=0; i<$1 ;i++)) do
    java -cp build/classes br.inf.ufes.ppd.application.Client teste teste 50000 ./dictionary.txt &
done


 
