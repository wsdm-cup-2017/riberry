#!/usr/bin/env python

import os, sys, traceback
import subprocess
import random, time
import urllib2
import threading, thread
from SimpleXMLRPCServer import SimpleXMLRPCServer
import xmlrpclib
import httplib
import requests
import json
import inspect
mypydir =os.path.abspath(os.path.dirname(inspect.getfile(inspect.currentframe())))
sys.path.append(mypydir)
from copy import deepcopy
import logging,glob
import socket
import SocketServer

import numpy
import xgboost
from sklearn import cross_validation
from sklearn.metrics import accuracy_score
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import OneHotEncoder
import pandas as pd

flist = ['_2012_10','_2012_11','_2013_01','_2013_03','_2013_05','_2013_07','_2013_09','_2013_11','_2014_01','_2014_03','_2014_05','_2014_07','_2014_09','_2014_11','_2015_01','_2015_03','_2015_05','_2015_07','_2015_09','_2015_11','_2016_01']
# ExtactedFeatures_2012_10_TrainData
ifname = 'ExtactedFeatures%s_TrainData.csv'
fstart = 0
fend = 105
label = 106

def run():
	# load data
	fnum=1
	data = pd.read_csv(ifname%flist[fnum])
	dataset = data.values
	# split data into X and y
	X = dataset[:,fstart:fend+1]
	Y = dataset[:,label]
	# # encode string input values as integers
	# encoded_x = None
	# for i in range(0, X.shape[1]):
	# 	label_encoder = LabelEncoder()
	# 	feature = label_encoder.fit_transform(X[:,i])
	# 	feature = feature.reshape(X.shape[0], 1)
	# 	onehot_encoder = OneHotEncoder(sparse=False)
	# 	feature = onehot_encoder.fit_transform(feature)
	# 	if encoded_x is None:
	# 		encoded_x = feature
	# 	else:
	# 		encoded_x = numpy.concatenate((encoded_x, feature), axis=1)
	# print("X shape: : ", encoded_x.shape)
	# encode string class values as integers
	# label_encoder = LabelEncoder()
	# label_encoder = label_encoder.fit(Y)
	# label_encoded_y = label_encoder.transform(Y)
		# split data into train and test sets
	seed = 2
	test_size = 0.5
	X_train, X_test, y_train, y_test = cross_validation.train_test_split(X, Y, test_size=test_size, random_state=seed)
	# fit model no training data
	model = xgboost.XGBClassifier()
	model.fit(X_train, y_train)
	# make predictions for test data
	y_pred = model.predict(X_test)
	predictions = [round(value) for value in y_pred]
	y_truth = []
	for v in y_test:
		if v<0.5:
			y_truth.append(False)
		else:
			y_truth.append(True)

	# evaluate predictions
	accuracy = accuracy_score(y_truth, predictions)
	print("Accuracy: %.2f%%" % (accuracy * 100.0))


if __name__ == "__main__":
	
	run()
	print 'Main ended'

	sys.exit(0)


	#-----------------------------------------
# https://api.particle.io/v1/devices/270031000b47343432313031/counter/?access_token=0e42e4879f627e1e318f605f6e3c0c275a66df17
	fl=glob.glob(resname+'*')
