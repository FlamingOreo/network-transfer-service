package com.gehc.platform.networktransfer.database;

import java.util.logging.Logger;

import com.ge.med.terra.tap.dm.DMObject;
import com.ge.med.terra.tap.dm.DMQuery;
import com.ge.med.terra.tap.dm.DMSession;
import com.ge.med.terra.tap.dm.DMTag;

/**
 * 
 * @author dp104662
 *
 */

public class QueryLocalDB {
	private static final QueryLocalDB INSTANCE = new QueryLocalDB();
	private Logger log = Logger.getLogger(QueryLocalDB.class.getName());
	private DMSession dmSession = null;
	public static final DMTag SERIES_UID_TAG = new DMTag(0x0020, 0x000E);
	
	public static QueryLocalDB getInstance() {
		return INSTANCE;
	}
	
	public DMObject querySeries(String seriesUID) {

		// query for series Instance uid.
		DMQuery dmq2 = new DMQuery("(0x20,0x0E) = \"" + seriesUID + "\"");
		DMObject[] series = dmSession.getRelated("series", dmq2);

		DMObject singleSeries = null;
		if(series.length > 0) {
			singleSeries = series[0];
		}
		return singleSeries;
	}

	
	private QueryLocalDB() {
		dmSession = DBSession.getInstance().getLocalDBSession();
		
	}
	
	public int getImageCountForSeries(String seriesUID) {
		DMObject series = querySeries(seriesUID);
		if (series == null) {
			return 0;
		}
		
		int count = series.getRelated("image").length;
		return count;
	}
	
	/**
	 * Query localDB to get Exam
	 * 
	 * @param studyUID
	 * @return
	 */
	public DMObject queryExam(String studyUID) {
		// query for study Instance uid.
		DMQuery dmq1 = new DMQuery("(0x20,0x0D) = \"" + studyUID + "\"");
		// Get All Exams.
		DMObject[] exams = dmSession.getRelated("study", dmq1);
		DMObject singleStudy = null;
		if(exams.length > 0) {
			singleStudy = exams[0]; 
		}
		
		return singleStudy;
	}
	
	public int getImagesCountForExam(String examUID) {
		int count = 0;
		
		DMObject exam = queryExam(examUID);
		if(exam == null){
			return 0;
		}
		
		DMObject[] series = exam.getRelated("series");
		for (DMObject dmObject : series) {
			int length = dmObject.getRelated("image").length;
			log.info("Series Level Count  " + length + " images");			
			count += length;
		}
		return count;
	}
	
	/**
	 * Query local DB to get Image
	 * 
	 * @param imageUID
	 * @return
	 */
	public DMObject queryImage(String imageUID) {
		// query for series Instance uid.
		DMQuery dmq3 = new DMQuery("(0x8,0x18) = \"" + imageUID + "\"");
		DMObject[] images = dmSession.getRelated("image", dmq3);
		
		DMObject singleImage = images[0];
		return singleImage;
	}



	
	public String querySCImage(String examUID, int seriesNumber) {
		DMObject exam = queryExam(examUID);
		if(exam == null){
			log.severe("queryExam returns null for examID: " + examUID);
			return null;
		}
		// query for series number
		DMQuery dmq2 = new DMQuery("(0x20,0x11) = \""
				+ seriesNumber + "\"");
		DMObject[] series = exam.getRelated("series", dmq2);
		if ((series != null) && (series.length > 0)) {
			Object localVal = series[0].getValue(SERIES_UID_TAG);
			if(localVal != null) {
				return localVal.toString();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

}
