package wsdm;

import java.awt.datatransfer.FlavorListener;
import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
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
	public static final int expandTo = 1; // expend vandal to 10 times its ori size 
	
	public static int NumFeature ;// num of features, not include label
	public static ArrayList<Integer> SkipFeatPos = new ArrayList<>(); // skip these features 1-119.
	static{
		SkipFeatPos.add(1); //r id
		SkipFeatPos.add(118); // ori label
	}
	public static ArrayList<Integer> MissFeatPos = new ArrayList<>(); // miss these features 1-119.
	static{
		MissFeatPos.add(2); // u id
		MissFeatPos.add(4); // g id
	}
	public static ArrayList<Integer> perturbFeatPos = new ArrayList<>(); // change these features 1-119.
	static{
		perturbFeatPos.add(3); // 
		perturbFeatPos.add(5); // 
		perturbFeatPos.add(39); // 
		perturbFeatPos.add(40); // 
		perturbFeatPos.add(44); // 
		perturbFeatPos.add(46); // 
	}
	public static int[] trainfset;
	public static int[] testfset;
	public static float[] parameters ;
	
	public static final String BASEDIR_DATA_home = "/home/yiran/wsdm/featuretrain/";
	public static final String BASEDIR_DATA_lab = "./";
	public static String BASEDIR_DATA ;
	public static String[] flist = {"_2012_10","_2012_11","_2013_01","_2013_03","_2013_05","_2013_07","_2013_09","_2013_11","_2014_01","_2014_03","_2014_05","_2014_07","_2014_09","_2014_11","_2015_01","_2015_03","_2015_05","_2015_07","_2015_09"};
	public static String DataFormat= "ExtactedFeatures%s_array.txt";
	public static final String VandalizedFile = "vandalized_array.txt";
	
	public static final String SplitFile = "./data_%d/ExtactedFeatures_array.txt";
	public static final String Split6File = "./data_100%d/ExtactedFeatures_array.txt";
	public static final String SplitVandFile = "./vandalized_array%d.txt"; //vandalized_array.txt
	public static final String VandFile = "./vandalized_array.txt"; //vandalized_array.txt
	
	public static final String ModelPath = "./model/xgb%d.model";
	
	/**
	 * Aggregate array records from file, store in 1D float[] at offset.
	 */
	public static int aggregateDataAsFloat(String filename, float[] data, float[] label, int rowOffset, int expand){
		String line;
		int lpos = rowOffset;
		Random rand = new Random();
		
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
								data[k - 1 + NumFeature * lpos] = tmp ; //reduceFloat(tmp);
							}else{
								data[k - 1 + NumFeature * lpos] = MISSING;
							}
						} catch (Exception e) {
							data[k - 1 + NumFeature * lpos] = MISSING;
						}
					}
					label[lpos] = Float.parseFloat(st[0]);
				} else {
//					System.out.println(filename + ": invalid line at lpos=" + lpos+"\n\n"+line+"\n");
					for (int k = 1; k <= NumFeature; k++) {
						data[k - 1 + NumFeature * lpos] = data[k -1-NumFeature + NumFeature * lpos]; // copy last record 
					}
					label[lpos] = label[lpos-1];
				}
				lpos++;
				///////  add more  vandal
				if (expand>0 && label[lpos-1]>0.5) {
					for(int r=0;r<expandTo-1;r++){
						for (int k = 1; k <= NumFeature; k++) {
							if (MissFeatPos.contains(k) && rand.nextFloat()<0.4) {
								data[k - 1 + NumFeature * lpos] = MISSING;
								continue;
							}
							if (perturbFeatPos.contains(k) && rand.nextFloat()<0.4) {
								float ori = data[k -1-NumFeature + NumFeature * lpos];
								if (ori<=1.0) {
									ori *= 1+(rand.nextFloat()-0.5)/1000.0 ;
								}else{
									ori *= 1+(rand.nextFloat()-0.5)/1000000.0 ;
								}
								data[k - 1 + NumFeature * lpos] = ori;
								continue;
							}
							data[k - 1 + NumFeature * lpos] = data[k -1-NumFeature + NumFeature * lpos]; // copy last record 
						}
						
						label[lpos] = label[lpos-1];
						lpos++;
					}
				}
				
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
//	    System.out.println("Move offset to "+lpos);
	    
		return lpos;
	}
	
	/**
	 * aggregate [rows, cols] of data in files names fn [ full path ].
	 */
	public static int[] aggregateShape(String[] fn){
		int rc[] = new int[2];
		int n ;
		int lines=0, col=0;
		for (n = 0; n < fn.length; n++) {
			String filename =  fn[n];
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
//		System.out.println("We have "+flist.length+" files + 1 vandalized");
//		System.out.println("Skip features (1-119): "+SkipFeatPos.toString());
		
		LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
		
		if(parameters==null){
			params.put("eta", 0.1);
			params.put("max_depth", 6);
		}
		else if (parameters.length==8) { // -1 as default.
			if(parameters[0]>=0)
				params.put("eta", parameters[0]);
			if(parameters[1]>=0)
				params.put("max_depth", (int)parameters[1]);
			if(parameters[2]>=0)
				params.put("gamma", parameters[2]);
			if(parameters[3]>=0)
				params.put("subsample", parameters[3]);
			if(parameters[4]>=0)
				params.put("max_delta_step", parameters[4]);
			if(parameters[5]>=0)
				params.put("scale_pos_weight", parameters[5]);
			if(parameters[6]>=0)
				params.put("min_child_weight", parameters[6]);
			if(parameters[7]>=0)
				params.put("colsample_bytree", parameters[7]);
		}
		else if (parameters.length>0 && parameters.length<8 ){
			params.put("eta", 0.5);
			params.put("max_depth", 6);
			params.put("gamma", 0.01);
			params.put("subsample", 1);
			params.put("max_delta_step", 0);
			params.put("scale_pos_weight", 0.3);
			params.put("min_child_weight", 0.5);
			params.put("colsample_bytree", 0.8);
		}
		System.out.println(params);

		params.put("silent", 1);
		params.put("objective", "binary:logistic");
		params.put("eval_metric","auc");
		params.put("tree_method", "approx");
		params.put("seed", 1);
		

		// set round
		int round = 3;
		
		
		int trainf [];
		if (trainfset==null || trainfset.length==0) {
		   int tmp[] = {0,2,4,6,8};
		   trainf = tmp;
		}else{
			trainf= trainfset;
		}
		for (int i = 0; i < trainf.length; i++) {
//			System.out.println("Train files "+flist[trainf[i]]);
		}
		
		int testf []; 
		if (testfset==null || testfset.length==0) {
			int tmp[] = { 1,3,5,7 };
			testf = tmp;
		} else {
			testf = testfset;
		}
		for (int i = 0; i < testf.length; i++) {
//			System.out.println("Test files "+flist[testf[i]]);
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
		
//		System.out.println("Aggregate: train row="+totalrow+", test row="+testrow+", num feat="+NumFeature);
		
		float[] traindata=new float[totalrow*NumFeature];
		float[] trainlabel=new float[totalrow];
		
		float[] testdata=new float[testrow*NumFeature];
		float[] testlabel=new float[testrow];
		
//		System.out.println("-----  train data  ------");
		int offset = 0;
		for (int i = 0; i < trainf.length; i++) {
			String filename = BASEDIR_DATA + String.format(DataFormat, flist[trainf[i]]);
//			offset = aggregateDataAsFloat(filename, traindata, trainlabel, offset);
//			System.out.println(filename+" -> "+offset);
		}
//		offset = aggregateDataAsFloat(vanfile, traindata, trainlabel, offset);
//		System.out.println(vanfile+" -> "+offset);
		
//		System.out.println("-----  test data  ------");
		offset=0;
		for (int i = 0; i < testf.length; i++) {
			String filename = BASEDIR_DATA + String.format(DataFormat, flist[testf[i]]);
//			offset = aggregateDataAsFloat(filename, testdata, testlabel, offset);
//			System.out.println(filename+" -> "+offset);
		}
		
//		System.out.print("1st rec:\n");
		for(int i=0;i<traindata.length;i++){
			if(i<NumFeature){
//				System.out.print(traindata[i]+" ");
			}
		}
//		System.out.println();
//		System.out.println("last rec:");
		for(int i=traindata.length-NumFeature;i<traindata.length;i++){
//			System.out.print(traindata[i]+" ");
		}
//		System.out.println();
//		System.out.print("first/last label:");
//		System.out.print(trainlabel[0]+",");
//		System.out.println(trainlabel[trainlabel.length-1]);
		
		
		DMatrix trainMat = new DMatrix(traindata, traindata.length/NumFeature, NumFeature, MISSING);
		trainMat.setLabel(trainlabel);
		DMatrix testMat = new DMatrix(testdata, testdata.length/NumFeature, NumFeature, MISSING);
		testMat.setLabel(testlabel);


		HashMap<String, DMatrix> watches = new HashMap<String, DMatrix>();
		watches.put("train", trainMat);
		watches.put("test", testMat);


		// train a boost model
		Booster booster = XGBoost.train(trainMat, params, round, watches, null, null);

		// ground truth of test data
		float[] truth = testMat.getLabel();

//		// save model to modelPath
//		File file = new File("./model");
//		if (!file.exists()) {
//			file.mkdirs();
//		}
//		String modelPath = "./model/xgb.model";
//		booster.saveModel(modelPath);
//		// reload model and data
//		Booster booster2 = XGBoost.loadModel("./model/xgb.model");
		
		float[][] predicts2 = booster.predict(testMat);

		// check the two predicts
		System.out.println("Accuracy: "+getAccuracy(truth, predicts2));
	}
	
	public static void trainSplitData() throws Exception {
		LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
		
		params.put("silent", 1);
		params.put("objective", "binary:logistic");
		params.put("eval_metric","auc");
		params.put("tree_method", "approx");

		
		// set round
		int round = 12;
		int kSplit = 70;
		int ifbreak = 0;
		final String outfile = "./ressql.txt";
		PrintWriter writer = null;
		try{
		     writer = new PrintWriter(outfile, "UTF-8");
		} catch (Exception e) {
		}
		
		for (int fi = 0; fi < kSplit; fi++) {
			params.put("seed", fi);
			
			String trainf[] = {    ///////////////////////////////////////// train normal file
					String.format(Split6File, fi)
					};
			
			int trainsp[] = aggregateShape(trainf);
			int trainrow=trainsp[0];
			NumFeature=trainsp[1]  -1   ;  // no label -1
			
			String trainfv[] = {   ///////////////////////////////////////// train vandal file
					String.format(SplitVandFile, 2),
					String.format(SplitVandFile, 3),
					String.format(SplitVandFile, 4),
					String.format(SplitVandFile, 5),
					String.format(SplitVandFile, 6)
					};
			
			int trainvsp[] = aggregateShape(trainfv);
			int trainvrow=trainvsp[0];
			
			int tfi = (fi+1)%kSplit;
			String testf[] = {    ///////////////////////////////////////// test normal file
					String.format(SplitFile, tfi)
					};
			
			int testsp[] = aggregateShape(testf); 
			int testrow = testsp[0];
			
			String testfv[] = {   ///////////////////////////////////////// test vandal file
					String.format(SplitVandFile, 1)
					};
			
			int testvsp[] = aggregateShape(testfv); 
			int testvrow = testvsp[0];
			
			int totalrow = trainrow+trainvrow;
			testrow = testrow+testvrow;
			
			float[] traindata=new float[totalrow*NumFeature];
			float[] trainlabel=new float[totalrow];
			float[] testdata=new float[testrow*NumFeature];
			float[] testlabel=new float[testrow];
			
			int offset = 0;
			for (int i = 0; i < trainf.length; i++) {
				String filename = trainf[i];
				offset = aggregateDataAsFloat(filename, traindata, trainlabel, offset,0);
			}
			for (int i = 0; i < trainfv.length; i++) {
				String filename = trainfv[i];
				offset = aggregateDataAsFloat(filename, traindata, trainlabel, offset,0);
			}
			offset=0;
			for (int i = 0; i < testf.length; i++) {
				String filename = testf[i];
				offset = aggregateDataAsFloat(filename, testdata, testlabel, offset,0);
			}
			for (int i = 0; i < testfv.length; i++) {
				String filename = testfv[i];
				offset = aggregateDataAsFloat(filename, testdata, testlabel, offset,0); // don't expand 
			}
			
			System.out.println("\nrow:"+trainrow+ ' '+trainvrow+" "+totalrow+", "+testrow);
			System.out.println("last 2 rec:");
			for(int i=traindata.length-2*NumFeature;i<traindata.length-NumFeature;i++){
				System.out.print(traindata[i]+" ");
			}System.out.println(" ");
			for(int i=traindata.length-NumFeature;i<traindata.length;i++){
				System.out.print(traindata[i]+" ");
			}
			System.out.println();
			System.out.print("last 2 label:");
			System.out.print(trainlabel[trainlabel.length-2]+",");
			System.out.println(trainlabel[trainlabel.length-1]);
			
			DMatrix trainMat = new DMatrix(traindata, traindata.length/NumFeature, NumFeature, MISSING);
			trainMat.setLabel(trainlabel);
			DMatrix testMat = new DMatrix(testdata, testdata.length/NumFeature, NumFeature, MISSING);
			testMat.setLabel(testlabel);

			HashMap<String, DMatrix> watches = new HashMap<String, DMatrix>();
			watches.put("train", trainMat);
			watches.put("test", testMat);
			
			for(float eta = 0.552f; eta <=0.552f; eta+=0.002f){
				params.put("eta", eta);// 0.552
				for(int depth = 9; depth <=9; depth+=1 ){
					params.put("max_depth", depth); // 23
					for(float  gamma= 0.0342f;  gamma<=0.0342f; gamma+=0.002f){
						params.put("gamma", gamma);// 0.0342
						for(float  subsample= 0.9733f; subsample <= 0.9733f; subsample +=0.0003f){
							params.put("subsample", subsample);
							for(float step = 0f;  step<=0f; step+=0.05f){
								params.put("max_delta_step", step);//0
								for(float  scale= 0.294f;  scale<= 0.294f; scale +=0.003f){
									params.put("scale_pos_weight",scale);// 0.321 
									for(float  child= 0.875f; child <= 0.875f; child +=0.005f){
										params.put("min_child_weight", child); //0.8802
										for(float  colsample= 0.93f; colsample <=0.93f; colsample+=0.01f){
											params.put("colsample_bytree", colsample);// 0.95
											
											System.out.println(fi+params.toString());

											Booster booster = XGBoost.train(trainMat, params, round, watches, null, null);

////											// save model to modelPath
											File file = new File("./model");
											if (!file.exists()) {
												file.mkdirs();
											}
											String modelPath = String.format(ModelPath, fi);
											booster.saveModel(modelPath);
											
											float[][] predicts2 = booster.predict(testMat);
											
											float acc = getAccuracy(testMat.getLabel(), predicts2);
											System.out.println("Accuracy: "+acc+"\n----------------------------");
											
											writer.println(String.format("INSERT INTO wsdm6 ( fid,eta,max_depth,gamma,subsample,max_delta_step,scale_pos_weight,min_child_weight,colsample_bytree,accuracy ) VALUES ( %d,%.4f,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.6f );", 
													fi,eta,depth,gamma,subsample,step,scale,child,colsample,acc));
											
											writer.flush();
											
											if(ifbreak==1) break;
										}
										if(ifbreak==1) break;
									}
									if(ifbreak==1) break;
								}
								if(ifbreak==1) break;
							}
							if(ifbreak==1) break;
						}
						if(ifbreak==1) break;
					}
					if(ifbreak==1) break;
				}
				if(ifbreak==1) break;
			}
			if(ifbreak==1) break;
		}
		writer.close();

	}

	public static void main(String[] args) throws Exception {
//		System.out.println("args:   "+args[0]+"    "+args[1]);
		
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
		
//		System.out.println("IP: "+My_IP);
		if (My_IP.startsWith("192.168")) {
			BASEDIR_DATA = BASEDIR_DATA_home;
		}else if (My_IP.startsWith("128.")) {
			BASEDIR_DATA = BASEDIR_DATA_lab;
		}else {
			System.out.println("Wrong IP, exit...");
			return;
		}
		
		if (args.length>=1) {
			String trainst[] = args[0].split(",");
			trainfset = new int[trainst.length];
			for (int i = 0; i < trainfset.length; i++) {
				trainfset[i]=Integer.parseInt(trainst[i]);
			}
		}
		if (args.length>=2) {
//			System.out.println("Train: "+args[0]+"     Test: "+args[1]);
			String testst[] = args[1].split(",");
			testfset = new int[testst.length];
			for (int i = 0; i < testfset.length; i++) {
				testfset[i]=Integer.parseInt(testst[i]);
			}
		}
		if (args.length>=3) {
			String para[] = args[2].split(",");
			parameters = new float[para.length];
			for (int i = 0; i < parameters.length; i++) {
				parameters[i]=Float.parseFloat(para[i]);
			}
		}
		
		trainSplitData();
		
//		trainClassifier();
		System.out.println("-------------------------------------------");
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
