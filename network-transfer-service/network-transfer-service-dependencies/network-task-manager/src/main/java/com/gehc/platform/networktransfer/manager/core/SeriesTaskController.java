package com.gehc.platform.networktransfer.manager.core;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.med.ct.wfplat.system.rawimage.api.ImageStatusDomainObject;
import com.ge.med.ct.wfplat.utilities.ifc.command.Command;
import com.ge.med.terra.tap.dm.DMEvent;
import com.gehc.platform.networktransfer.command.SeriesArchiveCommand;
import com.gehc.platform.networktransfer.command.SeriesNetworkCommand;
import com.gehc.platform.networktransfer.database.QueryLocalDB;
import com.gehc.platform.networktransfer.manager.util.ControllerState;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;
import com.gehc.platform.networktransferservice.model.HostType;
import com.gehc.platform.networktransferservice.model.JobPriority;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.TransferState;
import com.gehc.platform.networktransferservice.model.TransferType;

/**
 * Manages a series level network task
 * 
 * @author dp104662
 *
 */
public class SeriesTaskController extends BaseTaskController {

	private Timer timer = new Timer("SeriesTaskController", true);
	private NetworkTransferRequest networkTransferRequest;
	private long imageCount = 0;
	private long timeWaited = 0;
	private static final Pattern N_OF_M_Pattern = Pattern.compile("^\\d+(.*)\\d+");
	private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
	private String lastProgressStatus = "0";
	private long lastProgressPercent = 0;
	private static final String FAILURE_TOKEN = "failure";
	private AtomicLong totalSubmittedImageCount = new AtomicLong(0);
	private DDSCommunicationHandler ddsCommunicationHandler;
	private static final Logger LOG = Logger.getLogger(SeriesTaskController.class.getName());
	private String seriesUID;
	private static final long MAX_TIMEOUT_MS = NetworkTransferConstants.HOUR_MS * MAX_WAIT_HOURS;
	private long imagesSubmitted = 0;
	private Command seriesCommand;


	public SeriesTaskController(NetworkTransferRequest req, DDSCommunicationHandler ddsCommunicationHandler,
			long imageCount) {
		super(req, imageCount);
		this.ddsCommunicationHandler = ddsCommunicationHandler;
		boolean isNetwork = HostType.NETWORK.equals(req.getHostType());
		seriesCommand = isNetwork ? new SeriesNetworkCommand() : new SeriesArchiveCommand();
		setState(ControllerState.NOTSTARTED);
		timer.schedule(timeoutTimerTask, NetworkTransferConstants.HOUR_MS, NetworkTransferConstants.HOUR_MS);
		this.totalSubmittedImageCount.compareAndSet(this.totalSubmittedImageCount.get(), imageCount);
	}

	@Override
	public void processImageInstalledNotification(ImageStatusDomainObject imageInstalledObj) {
		imageCount++;
	}

	@Override
	public void handleJobProgress(DMEvent event) {
		if (!isJobMatching(event.getProperty("content_uid"), event.getProperty("target"))) {
			return;
		}

		int jobState = Integer.parseInt(event.getProperty("job_status"));
		String progressString = getProgressString(event.getProperty("progress_description"), jobState);
		lastProgressStatus = progressString;
		handleState(jobState, progressString, getSeriesType());
	}
	
	@Override
	public TransferType getType() {
		return TransferType.SERIES;
	}
	
	@Override
	public void destroy() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	private TimerTask timeoutTimerTask = new TimerTask() {
		@Override
		public void run() {
			timeWaited += NetworkTransferConstants.HOUR_MS ;
			LOG.log(Level.SEVERE,
					"Images for job with UID {0} was not found in database after {1} ms. Total time waited is {2} ms.",
					new Object[] { req.getExamData().getSeriesData().get(0).getUid(), NetworkTransferConstants.HOUR_MS, timeWaited });
			try {
				submitJobIfDone();
			} catch (ExecutionException e) {
				LOG.severe("@@@SERIES Level Task timeout timer execution failed for seriesUID:" + req.getExamData().getSeriesData().get(0).getUid());
			}
		}
	};
	
	private synchronized void submitJobIfDone() throws ExecutionException {
		String hostname = super.getHostname();
		QueryLocalDB queryLocalDB = QueryLocalDB.getInstance();
		int imagesCountForSeries = queryLocalDB.getImageCountForSeries(seriesUID);
		
		/**
		 * Adding the logic below to make sure clean up of controller happens when images are not generated
		 */
		if((imagesCountForSeries == 0 && timeWaited >= MAX_TIMEOUT_MS) || super.getState() == ControllerState.FAILED) {
			// Case 1: waited for the maximum amount of time and still no images are available.
			super.setState(ControllerState.COMPLETED);
			destroy();
			LOG.log(Level.SEVERE,
					"@@@{0} Level Task pushed to host {1} for seriesUID {2} marked COMPLETED for cleanup because no images installed after timeout or task has previously failed",
					new Object[] { this.getType(), hostname,
							seriesUID });
			return;
		} else if (imagesCountForSeries == 0) {
			// Case 2: did not reach the maximum wait time and no images are available.
			return;
		}
		
		// Case 3: some amount of images are available, send what we have.
		setImageCountFromDatabase();
		if (imagesSubmitted == getTotalSubmittedImageCount()) {
			// Nothing changed from last submission so just return
			return;
		}
		
		LOG.log(Level.SEVERE, "@@@{0} Level Task pushed to host {1} for seriesUID: {2}", new Object[] { this.getType(), hostname, seriesUID });
		JobPriority priority = super.getDomainObject().getPriority();
		String priorityString = priority == null ? "" : priority.name();
		try {
			seriesCommand.execute(hostname, seriesUID, priorityString);
		} catch (com.ge.med.ct.wfplat.utilities.ifc.command.ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.setState(ControllerState.SUBMITTED);
		
		if (isAllImagesDone()) {
			destroy();
		} else {
			imagesSubmitted = getTotalSubmittedImageCount();
			LOG.log(Level.SEVERE, "@@@AUTOSTORE: all images are not available. Submitted {0} out of the expected {1}", new Object[] { imagesSubmitted, super.getTotalSubmittedImageCount() });
		}

	}
	
	private void setImageCountFromDatabase() {
		int imagesCountForSeries = getImageCountFromDatabase(seriesUID);
		
		/* Need to relax the constraint here little bit, 
		 because if series has more images generated by mistake then 
		 it should be transferred and not stalled.*/
		if(imagesCountForSeries > getTotalSubmittedImageCount()){
			setTotalSubmittedImageCount(imagesCountForSeries);
		}
		LOG.log(Level.SEVERE,
				"@@@{0} imagesCountForSeries {1} super.getTotalSubmittedImageCount() {2}",
				new Object[] { this.getType(),
						imagesCountForSeries,
						super.getTotalSubmittedImageCount() });
		setTotalSubmittedImageCount(imagesCountForSeries);
	}


	private boolean isJobMatching(String uid, String target) {
		return (getID().equals(uid) && networkTransferRequest.getJobKey().getHostname().equals(target));
	}
	
	/**
	 * Method to get progress String based on progress status received
	 * 
	 * @param progressString
	 * @param jobState
	 * @return
	 */
	private String getProgressString(String progressString, int jobState) {
		if (TransferState.FAILED.equals(NetworkTransferConstants.JOBSTATEMAP.get(jobState))) {
			return "0";
		}
		if (progressString != null) {
			Matcher nOfMMatcher = N_OF_M_Pattern.matcher(progressString.trim());
			if (nOfMMatcher.find()) {
				Matcher numberMatcher = ONLY_DIGITS.matcher(nOfMMatcher.group());
				numberMatcher.find();
				// Fetch first integer
				int first = Integer.parseInt(numberMatcher.group());
				// Fetch second integer
				numberMatcher.find();
				int second = Integer.parseInt(numberMatcher.group());

				if (second >= first) {
					progressString = String.valueOf(first);
				} else {
					progressString = String.valueOf(second);
				}

			} else {
				progressString = lastProgressStatus;
			}
		} else {
			progressString = lastProgressStatus;
		}
		return progressString;
	}
}
