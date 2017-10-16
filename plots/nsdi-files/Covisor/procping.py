#!/usr/bin/env python -tt

import sys, os

def readLogs (inputLogFile):
  logList = []
  with open(inputLogFile) as ilf:
    fileCont = ilf.readlines()
  for l in fileCont:
    print len(l.split())
    if len(l.split()) == 8:
      if l.split()[-1] == 'ms':
        logList.append((l.split()[-2]))
  return logList

def writeLog(fileCont, fileName):
  with open(fileName, 'w') as oFile:
    for r in fileCont:
      oFile.writelines(str(r)+'\n')

def aggrLogs(logs):
  pass
      
def main(fileName):
  res = readLogs(fileName)
  writeLog(res, 'procd_'+fileName)

if __name__ == '__main__':
  print sys.argv[1:]
  for fName in sys.argv[1:]:
    main(fName)
  
