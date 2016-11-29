
package org.wsdmcup17.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.dumpfiles.MwRevision;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessor;

import com.rf.categ.RandomForestTest;

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
	
	public static final String MODELPATH = "./xgbmodel/xgb%d.model";
	public static final float MISSING = -100.0f;
	public static final int NumXgbModels = 70;
	public static Booster xgblist[];
	public static final int NumFeature =119 ;// num of features, xgboost
	public static ArrayList<Integer> SkipFeatPos = new ArrayList<>(); // skip these features 1-119.
	static{
		SkipFeatPos.add(1); // id
		SkipFeatPos.add(118); // ori label
	}
	private static float [] xgbfeatures=new float[NumFeature];
	private static float [] xgbscores  =new float[NumXgbModels];
	private final String xgbOutfile = "./xgbres.txt";
	private PrintWriter xgbWriter = null;
	
	
	private static final Logger
		LOG = LoggerFactory.getLogger(DummyRevisionClassifier.class);
	
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
	
	
	////////////////add by tuoyu start
	private RandomForestTest RFclassifier;
	///////////////end

	public DummyRevisionClassifier(BlockingQueue<CSVRecord> metaQueue, CSVPrinter resultPrinter) {
		this.resultPrinter = resultPrinter;
		this.metadataQueue = metaQueue;
		try {
			xgblist = new Booster[NumXgbModels];
			for (int i = 0; i < NumXgbModels; i++) {
				xgblist[i] = XGBoost.loadModel(String.format(MODELPATH, i));
			}
			System.out.println("xgb model loaded ! ");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("model fail \n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n ! ");
		}
		try {
			xgbWriter = new PrintWriter(xgbOutfile, "UTF-8");
		} catch (Exception e) {
		}
		//////////////// add by tuoyu start
		RFclassifier = new RandomForestTest();
		//////////////// end
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
		float score = 0;
		
		FeatureExtractor extractor=new FeatureExtractor(revision);
		String extractedrecord=extractor.extractedrecord;
//		System.out.println(extractedrecord);
		
		convertXgbFeatureToFloatArray( extractedrecord, xgbfeatures);
//		for(int i =0; i<xgbfeatures.length; i++){
//			System.out.print(xgbfeatures[i]+" ");
//		}System.out.println("-----");
		
		// reload model and test
		String xgbPrint = revision.getRevisionId()+" ";
		try {
			DMatrix testMat2 = new DMatrix(xgbfeatures, 1, NumFeature, MISSING);
			for (int i = 0; i < xgblist.length; i++) {
				if (xgblist[i] != null) {
					float[][] predicts2 = xgblist[i].predict(testMat2);
					score = predicts2[0][0];
					xgbscores[i] = score;
					xgbPrint +=String.format("%.3f ", score) ;
				} else {
					System.out.println("model null \n ! ");
				}
			}
			xgbWriter.print(xgbPrint); xgbWriter.flush();
		} catch (XGBoostError e) {
			e.printStackTrace();
		}
		
		if (score>0.5) {
			System.out.println("----vandalism !----"+score);
			System.out.println(revision.getRevisionId());
			System.out.println(extractedrecord);
			System.out.println("--------");
		}
		
		
		////////////////add by tuoyu start
		String extractedrecord3= extractedrecord.replaceAll("\\[", "").replaceAll("\\]","").replaceAll(", ", ",");
		//System.out.println(extractedrecord2);
		float RFreuslt=RFclassifier.getEvaluation(extractedrecord3);
		float RFbinaryresult=0;
		if(RFreuslt>0.5){
			RFbinaryresult=1;
		}
		//System.out.println("reuslt="+RFreuslt);
		//RFbinartresult is the output
		/////////// end
	
		xgbWriter.print( String.format("%.5f", RFreuslt)); 
		
		xgbWriter.print("\n"); 
		xgbWriter.flush();
		
		return RFreuslt;
	}
	
	
	
	
	
	
	
	
	
	private int convertXgbFeatureToFloatArray(String extractedrecord, float[] featfloat){ // return success
		int success = 1;
		String extractedrecord2= extractedrecord.replaceAll("\\[", "").replaceAll("\\]","").replaceAll(", ", "~");
		String[] parts = extractedrecord2.split("~");
		int flen = parts.length;
		assert(flen>=NumFeature);
		for(int i = 0; i<NumFeature; i++){
			if (SkipFeatPos.contains(i+1)) {
				featfloat[i] = MISSING;
				continue;
			}
			String f = parts[i];
			try
			{
			  featfloat[i]=Float.parseFloat(f);
			}
			catch(NumberFormatException e)
			{
				String tmpf = "";
				for(int j=0; j<f.length(); j++){
					char c = f.charAt(j);
					if (c >= '0' && c <= '9'){
						tmpf = tmpf+ Character.toString( c);
					}else{
						int ascii = (int) c;
						if (ascii<10) {
							tmpf = tmpf+"00"+ascii;
						}else if (ascii<100) {
							tmpf = tmpf+"0"+ascii;
						}else {
							tmpf = tmpf+ascii;
						}
					}
				}
				if (tmpf.length()>38) {
					tmpf=tmpf.substring(0, 38);
				}
				try{
					featfloat[i]=Float.parseFloat(tmpf);
				}catch(NumberFormatException ee2){
					featfloat[i]=MISSING;
				}
			}
		}
		return success;
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
