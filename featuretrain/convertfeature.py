#!/usr/bin/env python

import os, sys
import subprocess
import random, time
import inspect
mypydir =os.path.abspath(os.path.dirname(inspect.getfile(inspect.currentframe())))
sys.path.append(mypydir)

from itertools import chain, combinations
from collections import defaultdict
import csv

iffile='ExtactedFeatures%s_TrainData'
offile='ExtactedFeatures%s_TrainData.csv'
osfile='ExtactedFeatures%s_svm.txt'

flist = ['_2012_10','_2012_11','_2013_01','_2013_03','_2013_05','_2013_07','_2013_09','_2013_11','_2014_01','_2014_03','_2014_05','_2014_07','_2014_09','_2014_11','_2015_01','_2015_03','_2015_05','_2015_07','_2015_09','_2015_11','_2016_01']

# 40,155,Jeblad,39,2012-10-29T17:36:49Z,nb,TEXT,Norge,20,Norway,NA,NA,Norway,1,1,0,0,1,1,0.8,0,0,0.2,0,0,0,0,0,0,0,0,0,0,F,F,0,5,0,0,0,1,0,NA,0.79,5,NA,NA,NA,NA,T,T,NA,NA,NA,NA,NA,NA,F,F,NA,1,F,F,2,1,0,0,0,0,0,0,F,31,T,2,wbsetlabel,pageCreation,set,NA,233,39,F,T,1,NA,NA,79,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,F,0,0,0,0,0,0,F,F,F,F,F,F,F,F,F,F

#ord('a'):97
#chr(97):'a'
convfilenum = 3
convrownum = -1

def conv(st):
	asc = str(ord(st))
	l=len(asc)
	if l==2:
		asc='0'+asc
	elif l==1:
		asc='00'+asc
	return asc # st to 3-digit ascii  

def convert2svm():
	cntf=0
	for fn in flist:
		if convfilenum!=-1 and cntf>=convfilenum:
			break
		cntf+=1

		of = open(osfile%fn,'w')
		with open(iffile%fn, 'rb') as csvfile:
			ifd = csv.reader(csvfile)
			cntr=0
			for row in ifd:
				if row[0].startswith('revision'):
					continue
				if convrownum!=-1 and cntr>=convrownum:
					break
				cntr+=1

				crow = ' '
				cnti=0
				for item in row:
					cnti+=1
					if cnti==len(row): # libsvm format label first. 
						crow=row[cnti-1]+crow
					else:
						try: 
							fitem = float(item)
							crow+=`cnti`+':'+(str(item))+' '
						except:
							newitem=''
							for pos in item:
								if pos.isdigit():
									newitem+=pos
								else:
									newitem+=conv(pos)
							crow+=`cnti`+':'+(newitem)+' '
				of.write(crow+'\n')
		of.close()

def convert2ascii():
	cntf=0
	for fn in flist:
		if convfilenum!=-1 and cntf>=convfilenum:
			break
		cntf+=1

		of = open(offile%fn,'wb')
		writer = csv.writer(of)
		with open(iffile%fn, 'rb') as csvfile:
			ifd = csv.reader(csvfile)
			cntr=0
			for row in ifd:
				if row[0].startswith('revision'):
					continue
				if convrownum!=-1 and cntr>=convrownum:
					break
				cntr+=1

				crow = [] # converted row
				for item in row:
					try: # number not be converted
						fitem = float(item)
						crow.append(str(item))
					except:
						newitem=''
						for pos in item:
							if pos.isdigit():
								newitem+=pos
							else:
								newitem+=conv(pos)
						crow.append(newitem)
				writer.writerow(crow)
		of.close()

if __name__ == "__main__":
	et=sys.argv[0]
	currentframe= inspect.getfile(inspect.currentframe()) # script filename
	print 'currentframe:'+currentframe
	abspath=os.path.abspath(os.path.dirname(currentframe))
	print 'abspath:'+abspath
	convert2svm()
