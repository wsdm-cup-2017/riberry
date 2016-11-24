package com.rf.categ;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class RandomForestTest {
	private static ArrayList<DTreeCateg2> trees2; //remember to remove static!
	public RandomForestTest() {
		System.out.println("Restoring forest."); 
		try
		{
			File dir=new File("");
		    FileInputStream myFileInputStream = new FileInputStream(dir.getAbsolutePath() + "/RFmodel.ser");
		    ObjectInputStream myObjectInputStream = new ObjectInputStream(myFileInputStream);
		    trees2 = (ArrayList<DTreeCateg2>) myObjectInputStream.readObject(); 
		    myObjectInputStream.close();
		}
		catch (Exception e)
		{
			System.out.println("Error when loading from file."); 
		}
		
	}
	
	public static float getEvaluation(String record){//use this function in client
		ArrayList<ArrayList<String>> DataInput = new ArrayList<ArrayList<String>>();//65009961
		ArrayList<Integer> Sp=new ArrayList<Integer>();
		
		if(record.indexOf(",")>=0){
			String myrecord=","+record+",";
			char[] c =myrecord.toCharArray();
			for(int i=0;i<myrecord.length();i++){
				if(c[i]==',')
					Sp.add(i);
			}ArrayList<String> DataPoint=new ArrayList<String>(Sp.size());
			//System.out.println("size= "+Sp.size());
			for(int i=0;i<Sp.size()-1;i++){
				if(!(i==5 || (i>=13 && i<=23) || (i>=32 && i<=40) || (i>=42 && i<=56) || (i>=72 && i<=78)|| i==119)){
					continue;
				}
				DataPoint.add(myrecord.substring(Sp.get(i)+1, Sp.get(i+1)).trim());
			}DataInput.add(DataPoint);
			//System.out.println(DataPoint.size());
		}else if(record.indexOf(" ")>=0){
			//has spaces
			String myrecord=" "+record+" ";
			for(int i=0;i<myrecord.length();i++){
				if(Character.isWhitespace(myrecord.charAt(i)))
					Sp.add(i);
			}ArrayList<String> DataPoint=new ArrayList<String>();
			for(int i=0;i<Sp.size()-1;i++){
				DataPoint.add(myrecord.substring(Sp.get(i), Sp.get(i+1)).trim());
			}DataInput.add(DataPoint);//System.out.println(DataPoint);
		}
		
		
		
		int treee=1;
		
		ArrayList<ArrayList<String>> Prediction= new ArrayList<ArrayList<String>>();
		for(DTreeCateg2 DTC : trees2){
			DTC.CalculateClasses_NoClass(DataInput, treee);treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		
		float positivecount=0;
		for(int i=0;i<Prediction.size();i++){
			if(Prediction.get(i).get(0).trim().equalsIgnoreCase("1")){
				positivecount++;
			}
		}
		positivecount=positivecount/(float)Prediction.size();
		
		return positivecount;
	}
	
	public static void main(String[] args){//run to test for a given record.
		System.out.println("Restoring forest."); 
		try
		{
			File dir=new File("");
		    FileInputStream myFileInputStream = new FileInputStream(dir.getAbsolutePath() + "/RFmodel.ser");
		    ObjectInputStream myObjectInputStream = new ObjectInputStream(myFileInputStream);
		    trees2 = (ArrayList<DTreeCateg2>) myObjectInputStream.readObject(); 
		    myObjectInputStream.close();
		}
		catch (Exception e)
		{
			System.out.println("Error when loading from file."); 
		}
		String record="16,713,Denny,16,2012-10-29T17:03:21Z,NA,MISC,NA,15,Africa,NA,NA,Africa,-1,-1,-1,-1,-1,NA,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,NA,F,F,NA,NA,NA,0,0.5,NA,0,NA,NA,NA,NA,NA,NA,NA,F,T,NA,NA,NA,NA,NA,NA,F,F,NA,1,F,F,1,1,0,0,0,0,0,0,F,31,NA,1,pageCreation,NA,NA,NA,179,NA,F,NA,NA,NA,NA,NA,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,F,-1,-1,-1,-1,-1,-1,F,F,F,F,F,F,F,F,F,F";
		float result = getEvaluation(record);
		System.out.println("result="+result);
		
	}
	
	
}