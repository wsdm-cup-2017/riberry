package wsdm;

import java.awt.datatransfer.FlavorListener;
import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Scanner;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import scala.reflect.internal.Trees.New;
//import ml.dmlc.xgboost4j.java.example.util.DataLoader;


public class Classifier {
	public static String My_IP;
	public static final float MISSING = -100.0f;
	public static int NumFeature ;//= 119; // num of features, not include label
	public static ArrayList<Integer> SkipFeatPos = new ArrayList<>(); // skip these features 1-119.
	static{
		SkipFeatPos.add(1);
	}
	public static int[] trainfset;
	public static int[] testfset;
	
	public static final String BASEDIR_DATA_home = "/home/yiran/wsdm/featuretrain/";
	public static final String BASEDIR_DATA_lab = "./";
	public static String BASEDIR_DATA ;
	public static String[] flist = {"_2012_10","_2012_11","_2013_01","_2013_03","_2013_05","_2013_07","_2013_09","_2013_11","_2014_01","_2014_03","_2014_05","_2014_07","_2014_09","_2014_11","_2015_01","_2015_03","_2015_05","_2015_07","_2015_09"};
	public static String DataFormat= "ExtactedFeatures%s_array.txt";
	public static final String VandalizedFile = "vandalized_array.txt";
	
	/**
	 * Aggregate array records from file, store in 1D float[] at offset.
	 */
	public static int aggregateDataAsFloat(String filename, float[] data, float[] label, int rowOffset){
		String line;
		int lpos = rowOffset;
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			while ((line = br.readLine()) != null) {
				if ("".equals(line.trim())) { // consistent with getShape, otherwise vec len not match ! 
					continue;
				}
				String st[] = line.trim().split(" ");
				if (st.length == NumFeature + 1) {
					for (int k = 1; k <= NumFeature; k++) {
						if (SkipFeatPos.contains(k)) {
							data[k - 1 + NumFeature * lpos] = MISSING;
							continue;
						}
						try {
							float tmp = Float.parseFloat(st[k]);
							if (tmp>-Float.MAX_VALUE && tmp<Float.MAX_VALUE) {
								data[k - 1 + NumFeature * lpos] = tmp;
							}else{
								data[k - 1 + NumFeature * lpos] = MISSING;
								//System.out.println("! Missing: "+tmp+" at row "+(1+lpos));
							}
						} catch (Exception e) {
							data[k - 1 + NumFeature * lpos] = MISSING;
						}
					}
					label[lpos] = Float.parseFloat(st[0]);
				} else {
					System.out.println(filename + ": invalid line at lpos=" + lpos+"\n\n"+line+"\n");
					for (int k = 1; k <= NumFeature; k++) {
						data[k - 1 + NumFeature * lpos] = data[k -1-NumFeature + NumFeature * lpos]; // copy last record 
					}
					label[lpos] = label[lpos-1];
				}
				lpos++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    System.out.println("Move offset to "+lpos);
	    return lpos;
	}
	
	/**
	 * aggregate [rows, cols] of data in files pos in flist[].
	 */
	public static int[] aggregateShape(int[] fn){
		int rc[] = new int[2];
		int n ;
		int lines=0, col=0;
		
		for (n = 0; n < fn.length; n++) {
			String filename = BASEDIR_DATA + String.format(DataFormat, flist[fn[n]]);
			try {
				BufferedReader reader = new BufferedReader(new FileReader(filename));
				String line;
				while ((line = reader.readLine()) != null) {
					if (!"".equals(line.trim())) {
						lines++;
						if (col == 0) {
							col = line.split(" ").length;
						}
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		rc[0]=lines;
		rc[1]=col;
		return rc;
	}
	
	/**
	 * return [rows, cols] of data in file.
	 */
	public static int[] getshape(String filename){
		int rc[] = new int[2];
		int lines=0, col=0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null) {
				if (!"".equals(line.trim())) {
					lines++;
					if (col==0) {
						col=line.split(" ").length;
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		rc[0]=lines;
		rc[1]=col;
		return rc;
	}

	/**
	 * check results of multi-Dim vector, vec[:]=pred[:]
	 */
	public static float checkPredicts(float[][] fPredicts, float[][] sPredicts) {
		//System.out.println(fPredicts.length); // # rows
		if (fPredicts.length != sPredicts.length) {
			return -1;
		}
		int total = 0, right = 0;
		for (int i = 0; i < fPredicts.length; i++) {
			total++;
			//System.out.println(fPredicts[i].length);break; // =1.
			if (Arrays.equals(fPredicts[i], sPredicts[i])) {
				right++;
			}
		}
		return ((float) right)/total;
	}
	
	/**
	 * check results of 1D prediction, truth[i]==pred[i][0]
	 */
	public static float getAccuracy(float[] truth, float[][] sPredicts) {
		if (truth.length != sPredicts.length) {
			return -1;
		}
		int total = 0, right = 0;
		for (int i = 0; i < truth.length; i++) {
			total++;
			if (truth[i]<=0.5 && sPredicts[i][0]<=0.5) {
				right++;
			}
			if (truth[i]>0.5 && sPredicts[i][0]>0.5) {
				right++;
			}
		}
		return ((float) right)/total;
	}

	public static void trainClassifier() throws Exception {
		System.out.println("We have "+flist.length+" files + 1 vandalized");
		System.out.println("Skip features (1-119): "+SkipFeatPos.toString());
		
		int trainf [];
		if (trainfset==null || trainfset.length==0) {
		   int tmp[] = {0,2,4,6,8};
		   trainf = tmp;
		}else{
			trainf= trainfset;
		}
		for (int i = 0; i < trainf.length; i++) {
			System.out.println("Train files "+flist[trainf[i]]);
		}
		
		int testf []; 
		if (testfset==null || testfset.length==0) {
			int tmp[] = { 1,3,5,7 };
			testf = tmp;
		} else {
			testf = testfset;
		}
		for (int i = 0; i < testf.length; i++) {
			System.out.println("Test files "+flist[testf[i]]);
		}

		int trainsp[] = aggregateShape(trainf);
		int trainrow=trainsp[0];
		NumFeature=trainsp[1]  -1   ;  // no label -1
		
		int testsp[] = aggregateShape(testf); 
		int testrow = testsp[0];
		
		String vanfile = BASEDIR_DATA+VandalizedFile;
		int vansp[] = getshape(vanfile); 
		int vanrow = vansp[0];
		
		int totalrow = trainrow+vanrow;
		
		System.out.println("Aggregate: train row="+totalrow+", test row="+testrow+", num feat="+NumFeature);
		
		float[] traindata=new float[totalrow*NumFeature];
		float[] trainlabel=new float[totalrow];
		
		float[] testdata=new float[testrow*NumFeature];
		float[] testlabel=new float[testrow];
		
		System.out.println("-----  train data  ------");
		int offset = 0;
		for (int i = 0; i < trainf.length; i++) {
			String filename = BASEDIR_DATA + String.format(DataFormat, flist[trainf[i]]);
			offset = aggregateDataAsFloat(filename, traindata, trainlabel, offset);
			System.out.println(filename+" -> "+offset);
		}
		offset = aggregateDataAsFloat(vanfile, traindata, trainlabel, offset);
		System.out.println(vanfile+" -> "+offset);
		
		System.out.println("-----  test data  ------");
		offset=0;
		for (int i = 0; i < testf.length; i++) {
			String filename = BASEDIR_DATA + String.format(DataFormat, flist[testf[i]]);
			offset = aggregateDataAsFloat(filename, testdata, testlabel, offset);
			System.out.println(filename+" -> "+offset);
		}
		
		System.out.print("1st rec:\n");
		for(int i=0;i<traindata.length;i++){
			if(i<NumFeature){
				System.out.print(traindata[i]+" ");
			}
		}
		System.out.println();
		System.out.println("last rec:");
		for(int i=traindata.length-NumFeature;i<traindata.length;i++){
			System.out.print(traindata[i]+" ");
		}
		System.out.println();
		System.out.print("first/last label:");
		System.out.print(trainlabel[0]+",");
		System.out.println(trainlabel[trainlabel.length-1]);
		
		
		DMatrix trainMat = new DMatrix(traindata, traindata.length/NumFeature, NumFeature, MISSING);
		trainMat.setLabel(trainlabel);
		DMatrix testMat = new DMatrix(testdata, testdata.length/NumFeature, NumFeature, MISSING);
		testMat.setLabel(testlabel);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eta", 0.3);
		params.put("max_depth", 3);
		params.put("silent", 1);
		params.put("objective", "binary:logistic");
		params.put("eval_metric","auc");

		HashMap<String, DMatrix> watches = new HashMap<String, DMatrix>();
		watches.put("train", trainMat);
		watches.put("test", testMat);

		// set round
		int round = 2;

		// train a boost model
		Booster booster = XGBoost.train(trainMat, params, round, watches, null, null);

		// ground truth of test data
		float[] truth = testMat.getLabel();

		// save model to modelPath
		File file = new File("./model");
		if (!file.exists()) {
			file.mkdirs();
		}

		String modelPath = "./model/xgb.model";
		booster.saveModel(modelPath);

		// save dmatrix into binary buffer
		//testMat.saveBinary("./model/dtest.buffer");

		// reload model and data
		Booster booster2 = XGBoost.loadModel("./model/xgb.model");
		//DMatrix testMat2 = new DMatrix("./model/dtest.buffer");
		
		float[][] predicts2 = booster2.predict(testMat);

		// check the two predicts
		System.out.println("Accuracy: "+getAccuracy(truth, predicts2));
	}

	public static void main(String[] args) throws Exception {
		System.out.println("args:   "+args[0]+"    "+args[1]);
		
		try {
	        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        while (interfaces.hasMoreElements()) {
	            NetworkInterface iface = interfaces.nextElement();
	            // filters out 127.0.0.1 and inactive interfaces
	            if (iface.isLoopback() || !iface.isUp())
	                continue;

	            Enumeration<InetAddress> addresses = iface.getInetAddresses();
	            while(addresses.hasMoreElements()) {
	                InetAddress addr = addresses.nextElement();
	                My_IP = addr.getHostAddress();
	            }
	        }
	    } catch (SocketException e) {
	        throw new RuntimeException(e);
	    }
		
		System.out.println("IP: "+My_IP);
		if (My_IP.startsWith("192.168")) {
			BASEDIR_DATA = BASEDIR_DATA_home;
		}else if (My_IP.startsWith("128.")) {
			BASEDIR_DATA = BASEDIR_DATA_lab;
		}else {
			System.out.println("Wrong IP, exit...");
			return;
		}
		
		if (args.length>=2) {
			System.out.println("Train: "+args[0]+"     Test: "+args[1]);
			String trainst[] = args[0].split(",");
			String testst[] = args[1].split(",");
			trainfset = new int[trainst.length];
			testfset = new int[testst.length];
			for (int i = 0; i < trainfset.length; i++) {
				trainfset[i]=Integer.parseInt(trainst[i]);
			}
			for (int i = 0; i < testfset.length; i++) {
				testfset[i]=Integer.parseInt(testst[i]);
			}
		}
		
		trainClassifier();
	}
	
	
	/////////////////////////////////  utils //////////////////////
	public static float[] concatenate (float[] a, float[] b) {
	    int aLen = a.length;
	    int bLen = b.length;
	    float[] c = (float[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
	    System.arraycopy(a, 0, c, 0, aLen);
	    System.arraycopy(b, 0, c, aLen, bLen);
	    return c;
	}
}
