
#Pravein, 2017

import json
import os
import sys

#main logic
dir = sys.argv[1]
files = os.listdir(dir)
outputFile = open("output.csv", "w")

for json_file in files:
    json_data = open(dir+"/"+json_file,"r")
    execAvg = 0
    for line in json_data.readlines():
        data = json.loads(line)
        if data['Event'] == "SparkListenerTaskEnd":
            if data["Stage ID"] == 1:
                #print data["Task Metrics"]["Executor Run Time"]
                execAvg = (execAvg + data["Task Metrics"]["Executor Run Time"])/2
    outputFile.write(str(execAvg/1000.0)+"\n")
    print str(execAvg/1000.0)
outputFile.close()

