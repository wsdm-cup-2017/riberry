
package org.wsdmcup17.demo;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.dumpfiles.MwRevision;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessor;

import de.upb.wdqa.wdvd.FeatureExtractor;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

/**
 * A Wikidata Toolkit-based revision processor that classifies all revisions
 * as non-vandalism and hence sends a score 0f 0.0 to the server for each
 * revision.
 */
public class DummyRevisionClassifier implements MwRevisionProcessor {
	
	public static final String MODELPATH = "/home/yiran/wsdm/riberry/wsdm-classify/model/xgb.model";
	public static final float MISSING = -100.0f;

	private static final Logger
		LOG = LoggerFactory.getLogger(DummyRevisionClassifier.class);
	private Booster booster2 ;
	
	private static final String
		LOG_MSG_STARTING = "Starting...",
		LOG_MSG_CURRENT_STATUS = "Current status:",
		LOG_MSG_FINAL_RESULT = "Final result:",
		LOG_MSG_NUM_REVISIONS = "   Number of revisions: %s",
		LOG_MSG_NUM_REVISIONS_REGISTERED = 
			"   Number of registered revisions: %s";

	private static final int
		LOG_STATISTICS_INTERVAL = 10000;
	
	private CSVPrinter resultPrinter;
	private BlockingQueue<CSVRecord> metadataQueue;
	private long lastLogTime;
	private int numRevisions;
	private int numRevisionsFromRegisteredUsers;
	

	public DummyRevisionClassifier(
		BlockingQueue<CSVRecord> metaQueue, CSVPrinter resultPrinter
	) {
		this.resultPrinter = resultPrinter;
		this.metadataQueue = metaQueue;
		try {
			booster2 = XGBoost.loadModel(MODELPATH);
			System.out.println("model loaded ! ");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("model fail \n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n ! ");
		}
	}

	@Override
	public void startRevisionProcessing(
		String siteName, String baseUrl, Map<Integer, String> namespaces
	) {
		LOG.info(LOG_MSG_STARTING);
		lastLogTime = System.currentTimeMillis();
	}

	@Override
	public void processRevision(MwRevision revision) {
		// Retrieve corresponding metadata from metadata queue.
		CSVRecord metadata = getMetadata();

		// Classify revision
		float classificationScore = classifyRevision(revision, metadata);
		
		// Send result so server.
		sendClassificationResult(revision.getRevisionId(), classificationScore);
		
		// Update statistics for logging.
		updateStatistics(revision, metadata);
	}
	
	private float classifyRevision(MwRevision revision, CSVRecord metadata) {
		// This is where an actual classification based on  the revision and
		// its associated metadata should happen. Instead, we just assign a
		// score of 0.0, effectively classifying the revision as non-vandalism.
		final int startpos = 0; // start from 13th feature
		
		FeatureExtractor extractor=new FeatureExtractor(revision);
		String extractedrecord=extractor.extractedrecord;
//		System.out.println(extractedrecord);
		String extractedrecord2= extractedrecord.replaceAll("\\[", "").replaceAll("\\]","").replaceAll(", ", "~");
		String[] parts = extractedrecord2.split("~");
		String featsvm = "0 ";
		int flen = parts.length;
		assert(flen==119);
		
		float[] featfloat = new float[flen];
		
		for(int i = startpos; i<parts.length; i++){
			if(i==0||i==7||i==10||i==11){
				featfloat[i]=MISSING; // didn't use this feature
				continue;
			}
			String f = parts[i];
			featsvm = featsvm+ (i+1) +":";
			try
			{
			  featfloat[i]=Float.parseFloat(f);
			  featsvm = featsvm+f+" ";
			}
			catch(NumberFormatException e)
			{
				String tmpf = "";
				for(int j=0; j<f.length(); j++){
					char c = f.charAt(j);
					if (c >= '0' && c <= '9'){
						featsvm = featsvm+ Character.toString( c);
						tmpf = tmpf+ Character.toString( c);
					}else{
						int ascii = (int) c;
						if (ascii<10) {
							featsvm = featsvm+"00"+ascii;
							tmpf = tmpf+"00"+ascii;
						}else if (ascii<100) {
							featsvm = featsvm+"0"+ascii;
							tmpf = tmpf+"0"+ascii;
						}else {
							featsvm = featsvm+ascii;
							tmpf = tmpf+ascii;
						}
					}
				}
				featsvm = featsvm+" ";
				if (tmpf.length()>38) {
					tmpf=tmpf.substring(0, 38);
				}
				try{
					featfloat[i]=Float.parseFloat(tmpf);
				}catch(NumberFormatException ee2){
					featfloat[i]=0;
				}
			}
		}
//		System.out.println(featsvm);
		float score = 0;
//		for(int i =0; i<featfloat.length; i++){
//			System.out.print(featfloat[i]+" ");
//		}System.out.println("-----");

		// reload model and data
//		Booster booster2;
		try {
//			booster2 = XGBoost.loadModel("./model/xgb.model");
//			DMatrix testMat2 = new DMatrix("./model/dtest.buffer");
			DMatrix testMat2 = new DMatrix(featfloat, 1, flen, MISSING);
			
			if (booster2 != null) {
				float[][] predicts2 = booster2.predict(testMat2);
				score = predicts2[0][0];
			} else {
				System.out.println("model null \n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n ! ");
			}
			
		} catch (XGBoostError e) {
			e.printStackTrace();
		}
		
		if (score<=0.5) {
			//score=0.0f;
//			System.out.println(featfloat[0]);
//			System.out.println(featfloat[1]);
//			System.out.println(featfloat[4]);
//			System.out.println(featfloat[5]);
//			System.out.println(featfloat[117]);
//			System.out.println(featfloat[118]);			
//			System.out.println("------");

		}else{
			System.out.println("----vandalism !----"+score);
			System.out.println(revision.getRevisionId());
			System.out.println(featsvm);
			System.out.println(extractedrecord);
			System.out.println("--------");
			score=1.0f;
		}
		return score;
	}

	private void sendClassificationResult(
		long revisionId, float classificationScore
	) {
		try {
			resultPrinter.print(revisionId);
			resultPrinter.print(classificationScore);
			resultPrinter.println();
			resultPrinter.flush();
		}
		catch (IOException e) {
			LOG.error("", e);
		}
	}
	
	private CSVRecord getMetadata() {
		try {
			return metadataQueue.take();
		}
		catch (InterruptedException e) {
			LOG.error("", e);
			return null;
		}
	}

	@Override
	public void finishRevisionProcessing() {
		try {
			resultPrinter.close();
		}
		catch (IOException e) {
			LOG.error("", e);
		}
		LOG.info(LOG_MSG_FINAL_RESULT);
		printStatistics();
	}

	private void updateStatistics(MwRevision mwRevision, CSVRecord metadata) {
		numRevisions++;
		if (mwRevision.hasRegisteredContributor()) {
			numRevisionsFromRegisteredUsers++;
		}
		long currentTime = System.currentTimeMillis();
		if (currentTime > lastLogTime + LOG_STATISTICS_INTERVAL) {
			LOG.info(LOG_MSG_CURRENT_STATUS);
			printStatistics();
			lastLogTime = currentTime;
		}
	}
	
	private void printStatistics() {
		LOG.info(String.format(LOG_MSG_NUM_REVISIONS, numRevisions));
		LOG.info(String.format(LOG_MSG_NUM_REVISIONS_REGISTERED,
				numRevisionsFromRegisteredUsers));
	}
}
