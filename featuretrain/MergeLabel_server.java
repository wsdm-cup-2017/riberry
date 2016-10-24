package com.mergelabel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

//java -jar MergeLabel_server.jar wdvc16_truth.csv ExtactedFeatures_2012_10 ExtactedFeatures_2012_11 ExtactedFeatures_2013_01 ExtactedFeatures_2013_03 ExtactedFeatures_2013_05 ExtactedFeatures_2013_07 ExtactedFeatures_2013_09 ExtactedFeatures_2013_11 ExtactedFeatures_2014_01 ExtactedFeatures_2014_03 ExtactedFeatures_2014_05 ExtactedFeatures_2014_07 ExtactedFeatures_2014_09 ExtactedFeatures_2014_11 ExtactedFeatures_2015_01 ExtactedFeatures_2015_03 ExtactedFeatures_2015_05 ExtactedFeatures_2015_07 ExtactedFeatures_2015_09 ExtactedFeatures_2015_11 ExtactedFeatures_2016_01

public class MergeLabel_server{
	final static double  TestRatio=0.1;
	public static void main(String args[]){
		File labels=new File(""+args[0]);
		BufferedReader lreader;
		try {
			lreader = new BufferedReader(new FileReader(labels));
			String lline = null;
			lline = lreader.readLine();
			
			for(int filecount=1;filecount<args.length;filecount++){
				File features=new File(""+args[filecount]);
				String trainfile=""+args[filecount]+"_TrainData";
				PrintWriter trainwriter;
				int Tcount=0;
				int linecount=0;
				trainwriter = new PrintWriter(trainfile, "UTF-8");
				BufferedReader freader = new BufferedReader(new FileReader(features));
				String fline = null;
				boolean firstline=true;
				while((fline = freader.readLine()) != null ){

					linecount++;
					if(firstline){
						firstline=false;
						continue;
					}
					lline = lreader.readLine();
					
					String[] ftmpstr=fline.split(",");
				    String[] ltmpstr=lline.split(",");
				    if(ftmpstr[0].equals(ltmpstr[0]) && ftmpstr.length==119){
				    	String outputstr="";
				    	for(int i=0;i<ftmpstr.length;i++){
				    		outputstr=outputstr+ftmpstr[i]+",";
				    	}
				    	if(ltmpstr[1].equals("F")){
				    		outputstr=outputstr+"0";
				    	}else{
				    		outputstr=outputstr+"1";
				    		Tcount++;
				    	}

				    	trainwriter.println(outputstr);
//				    	/////////////
//				    	if(ltmpstr[1].equals("T")){
//				    		for(int p=0;p<999;p++){
//						    		trainwriter.println(outputstr);
//						    		Tcount++;
//				    		}
//				    	}
//				    	/////////////
				    	
				    }
				}
				trainwriter.close();
				freader.close();
				

				System.out.println("Tcount="+Tcount+" linecount="+linecount);
				
				
			}
			lreader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}
	

	
}
	
	