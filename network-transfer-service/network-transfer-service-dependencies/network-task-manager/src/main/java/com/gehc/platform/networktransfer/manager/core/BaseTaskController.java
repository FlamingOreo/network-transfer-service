package com.gehc.platform.networktransfer.manager.core;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.med.terra.tap.dm.DMJob;
import com.gehc.platform.networktransfer.database.QueryLocalDB;
import com.gehc.platform.networktransfer.manager.api.TaskController;
import com.gehc.platform.networktransfer.manager.util.ControllerState;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;
import com.gehc.platform.networktransfer.manager.util.SeriesType;
import com.gehc.platform.networktransferservice.model.JobKey;
import com.gehc.platform.networktransferservice.model.NetworkTransferJob;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.TransferState;
import com.gehc.platform.networktransferservice.model.TransferType;

public abstract class BaseTaskController implements TaskController {
	
	
	private static final Logger LOG = Logger.getLogger(BaseTaskController.class
			.getName());
	protected NetworkTransferRequest req;
	protected AtomicLong totalSubmittedImageCount = new AtomicLong(0);
	protected AtomicReference<ControllerState> controllerState = new AtomicReference<>();
	protected long expectedImageCount;
	protected static final int MAX_WAIT_HOURS = 12;
	protected static final int MAX_RETRIES = Integer.parseInt(System.getProperty("MAX_RETRIES", "240"));
	private JobKey jobkey = null;
	private long lastProgressPercent = 0; 
	private static final Pattern N_OF_M_PATTERN = Pattern.compile("^\\d+(.*)\\d+");
	private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
	private String lastProgressStatus = "0";
	private static final String FAILURE_TOKEN = "failure";
	private DDSCommunicationHandler ddsCommunicationHandler;
	private TransferType transferTimeType;
	private long imageCount = 0;
	

	protected BaseTaskController(NetworkTransferRequest request, long imageCount) {
		this.req = request;
		this.expectedImageCount = imageCount;
		setState(ControllerState.NOTSTARTED);		
		this.totalSubmittedImageCount.compareAndSet(this.totalSubmittedImageCount.get(), 0);
	}

	@Override
	public ControllerState getState() {
		return controllerState.get();
	}

	@Override
	public NetworkTransferRequest getDomainObject() {
		return req;
	}

	@Override
	public String getID() {
		return req.getJobKey().getId();
	}

	@Override
	public long getTotalSubmittedImageCount() {
		return this.totalSubmittedImageCount.get();
	}

	@Override
	public void setTotalSubmittedImageCount(long totalSubmittedImageCount) {
		this.totalSubmittedImageCount.compareAndSet(this.totalSubmittedImageCount.get(), totalSubmittedImageCount);
	}
	
	@Override
	public int getImageCountFromDatabase(String seriesUID) {
		QueryLocalDB queryLocalDB = QueryLocalDB.getInstance();
		return queryLocalDB.getImageCountForSeries(seriesUID);
	}
	
	protected void setState(ControllerState state) {
		this.controllerState.compareAndSet(this.controllerState.get(), state);
	}
	
	protected String getHostname() {
		return req.getJobKey().getHostname();
	}
	
	protected long getExpectedImageCount() {
		return expectedImageCount;
	}
	
	protected String getSeriesType() {
		return req.getExamData().getSeriesData().get(0).getType();
	}
	
	protected void handleState(int currentState, String progressString, String seriesType) {
		if (currentState == DMJob.STATE_SUCEEDED) {
			handleSuccess(progressString, SeriesType.valueOf(seriesType));
			return;
		}

		if (NetworkTransferConstants.JOBSTATEMAP.get(currentState) != null) {
			this.sendSubmitTaskResponse(progressString, getID(), NetworkTransferConstants.JOBSTATEMAP.get(currentState),
					SeriesType.valueOf(seriesType));
			setState(NetworkTransferConstants.CONTROLLERSTATEMAP.get(currentState));
		}
	}
	
	protected boolean isAllImagesDone() {
		
		return (imageCount == totalSubmittedImageCount.get());
	}
	
	private void handleSuccess(String progressString, SeriesType seriesType) {
		if(isAllImagesDone()) {
			this.sendSubmitTaskResponse(progressString, getID(), TransferState.COMPLETED, seriesType);
			setState( ControllerState.COMPLETED);
			destroy();
		} 
	}

	private void sendSubmitTaskResponse(String progressString, String id, TransferState transferState,
			SeriesType seriesType) {
		int prog = Integer.parseInt(progressString);
		NetworkTransferJob nwTaskResponseObj = new NetworkTransferJob();
		nwTaskResponseObj.setJobKey(jobkey);
		nwTaskResponseObj.setStatus(transferState);
		nwTaskResponseObj.setType(seriesType.toString());
		nwTaskResponseObj.setPercentComplete(BigDecimal.valueOf(prog));
		
		long newProgressPercent = getProgressPercent(nwTaskResponseObj, lastProgressPercent);
		// If there is a change in percentage or it failed or completed
		if(lastProgressPercent != newProgressPercent || TransferState.FAILED.
				equals(transferState) || TransferState.COMPLETED.equals(transferState)) {
			lastProgressPercent = newProgressPercent;
			// Update the progress with percent
			nwTaskResponseObj.setPercentComplete(BigDecimal.valueOf(newProgressPercent));
			ddsCommunicationHandler.submitResponse(nwTaskResponseObj);
			LOG.severe("@@@" + transferTimeType.toString()
					+ " Level transfer "
					+ newProgressPercent + "% completed for jobKey: " + this.jobkey);
		}

	}
	/**
	 * Method to calculate percent from progress
	 * @param lastProgress 
	 * @param nwTaskResponseObj
	 * @return
	 */
	private long getProgressPercent(
			NetworkTransferJob response, long lastProgress) {
		BigDecimal percentComplete= response.getPercentComplete();
		String percentCompleteStr = percentComplete.toString();
		if (response.getStatus().equals(TransferState.COMPLETED)) {
			return 100;
		} 
		if (percentCompleteStr != null && percentCompleteStr.contains(FAILURE_TOKEN)) {
			response.setStatus(TransferState.FAILED);
			return 0;
		} 
		if (percentCompleteStr != null) {
				Matcher m = ONLY_DIGITS.matcher(percentCompleteStr.trim());
			if (m.find()) {
				String progressString = m.group();
				Integer progress = Integer.valueOf(progressString);
				if (progress > 0 && totalSubmittedImageCount.get() > 0) {
					return (progress * 100) / totalSubmittedImageCount.get();
				}
			}
		}
		// progress string is null so return last progress status
		return lastProgress;
	}
	
	/**
	 * Method to get correct progress String based on progress status received.
	 * @param progressString
	 * @param jobState 
	 * @return
	 */
	private String getProgressString(String progressString, int jobState) {
		if(TransferState.FAILED.equals(NetworkTransferConstants.JOBSTATEMAP.get(jobState))) {
			// This is done because network management first sent success and then failure
			return "0";
		}
		if (progressString != null) {
			Matcher nOfMMatcher = N_OF_M_PATTERN.matcher(progressString.trim());
			if(nOfMMatcher.find()) {
				// Append previous success amount count to send correct progress for IMAGE case
				Matcher numberMatcher = ONLY_DIGITS.matcher(nOfMMatcher.group());
				// Fetch first integer
				numberMatcher.find();
				int first = Integer.parseInt(numberMatcher.group());
				//Fetch second integer
				numberMatcher.find();
				int second = Integer.parseInt(numberMatcher.group());
				
				//Set accordingly
				if(second >= first) {
					progressString = String.valueOf(first);
				} else {
					progressString = String.valueOf(second);
				}
			} else {
				// This means that new event has now new status or failure. In both cases status should remain same.
				progressString = lastProgressStatus;
			}
		} else {
			// This is to avoid any error condition on the UI side.
			progressString = lastProgressStatus;
		}
		return progressString;
	}

	
	


}
