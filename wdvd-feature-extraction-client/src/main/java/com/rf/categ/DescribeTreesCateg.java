package com.rf.categ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class DescribeTreesCateg {
//method to take the txt fle as input and pass those values to random forests
	BufferedReader BR = null;
	//String path;
	public DescribeTreesCateg(){//String path
		//this.path=path;
	}
	public ArrayList<ArrayList<String>> CreateInputCateg(String[] path){

		ArrayList<ArrayList<String>> DataInput = new ArrayList<ArrayList<String>>();//65009961
		
		try {
		 for(int f=0;f<path.length;f++){
			 String sCurrentLine;
				BR = new BufferedReader(new FileReader(path[f]));
				
				while ((sCurrentLine = BR.readLine()) != null) {
					ArrayList<Integer> Sp=new ArrayList<Integer>();int i;
					if(sCurrentLine!=null){
						if(sCurrentLine.indexOf(",")>=0){
							//has comma
							
							sCurrentLine=","+sCurrentLine+",";
							char[] c =sCurrentLine.toCharArray();
							for(i=0;i<sCurrentLine.length();i++){
								if(c[i]==',')
									Sp.add(i);
							}ArrayList<String> DataPoint=new ArrayList<String>(Sp.size());
							//System.out.println("size= "+Sp.size());
							for(i=0;i<Sp.size()-1;i++){
								if(!(i==5 || (i>=13 && i<=23) || (i>=32 && i<=40) || (i>=42 && i<=56) || (i>=72 && i<=78)|| i>=119)){
									continue;
								}
								DataPoint.add(sCurrentLine.substring(Sp.get(i)+1, Sp.get(i+1)).trim());
							}DataInput.add(DataPoint);//System.out.println(DataPoint);
						}
						else if(sCurrentLine.indexOf(" ")>=0){
							//has spaces
							sCurrentLine=" "+sCurrentLine+" ";
							for(i=0;i<sCurrentLine.length();i++){
								if(Character.isWhitespace(sCurrentLine.charAt(i)))
									Sp.add(i);
							}ArrayList<String> DataPoint=new ArrayList<String>();
							for(i=0;i<Sp.size()-1;i++){
								DataPoint.add(sCurrentLine.substring(Sp.get(i), Sp.get(i+1)).trim());
							}DataInput.add(DataPoint);//System.out.println(DataPoint);
						}
					}
				}System.out.println("Input generated");
				if (BR != null)BR.close();
		 }
		

	} catch (IOException e) {
		e.printStackTrace();
	} finally {
		try {
			if (BR != null)BR.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	return DataInput;
}
}
