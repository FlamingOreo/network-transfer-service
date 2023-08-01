package com.gehc.platform.networktransfer.manager.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ge.med.ct.wfplat.system.rawimage.api.ImageStatusDomainObject;
import com.ge.med.ct.wfplat.utilities.ifc.command.Command;
import com.ge.med.terra.tap.dm.DMEvent;
import com.gehc.platform.networktransfer.command.ImageArchiveCommand;
import com.gehc.platform.networktransfer.command.ImageNetworkCommand;
import com.gehc.platform.networktransfer.manager.util.ControllerState;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;
import com.gehc.platform.networktransfer.manager.util.SeriesType;
import com.gehc.platform.networktransferservice.model.HostType;
import com.gehc.platform.networktransferservice.model.JobKey;
import com.gehc.platform.networktransferservice.model.JobPriority;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.TransferType;

public class ImageLevelTaskController extends BaseTaskController {

	private static final long DELAY = NetworkTransferConstants.HOUR_MS * MAX_WAIT_HOURS;
	// Expecting a string in format 1 of 2 kind here. In Japanese it could be
	// reversed hence adding multiple patterns
	private static final Pattern N_OF_M_PATTERN = Pattern.compile("^\\d+(.*)\\d+");
	private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
	private String seriesUID; // This will be used to identify that image installed is for this series.
	private JobKey jobkey;
	private Command imageCommand;
	private String uniqueKey = null;
	private StringBuilder imageUIDString = null;
	private List<String> uidList = new ArrayList<>();
	private static final Logger LOG = Logger.getLogger(ImageLevelTaskController.class.getName());
	private static final int AUTOTRANSFER_MIN_CHUNKSIZE = Integer
			.parseInt(System.getProperty("AUTOTRANSFER_MIN_CHUNKSIZE", "25"));
	private Map<Long, Long> completedJobsMap = new HashMap<>();
	private long numSubmissionsExpected = 0;
	private long actualSubmission = 0;
	private String lastProgressStatus = "0";
	private Timer timer = new Timer("ImageLevelTaskController", true);
	
	@Override
	public void processImageInstalledNotification(ImageStatusDomainObject imageInstalledObj) {
		if (!isConditionMet(imageInstalledObj)) {
			return;
		}

		executeImageCommand(imageInstalledObj.getImageKey().getImageUID());
	}

	@Override
	public void handleJobProgress(DMEvent event) {
		long jobHandle = Long.parseLong(event.getProperty("jobHandle"));
		LOG.log(Level.INFO, "Properties: {0}", event.getProperties());
		if (!isJobMatching(event.getProperty("content_uid"), event.getProperty("target"))) {
			return;
		}

		int state = Integer.parseInt(event.getProperty("job_status"));
		String progressString = getProgressString(event.getProperty("progress_description"), jobHandle);

		handleState(state, progressString, getSeriesType());
	}

	public ImageLevelTaskController(NetworkTransferRequest domainObject,
			DDSCommunicationHandler ddsCommunicationHandler, long newImageCount) {
		super(domainObject, newImageCount);
		seriesUID = domainObject.getExamData().getSeriesData().get(0).getUid();
		boolean isNetwork = HostType.NETWORK.equals(domainObject.getHostType());
		imageCommand = isNetwork ? new ImageNetworkCommand() : new ImageArchiveCommand();
		jobkey = domainObject.getJobKey();
		setExpectedNumberOfSubmissions(0, newImageCount);
		// This is added to keep ImageLevelTaskController alive for specified time.
		// There is no way to identify when user has moved to next series and no more
		// images will be added to it.
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				setState(ControllerState.COMPLETED);
			}
		}, DELAY);
	}
	
	@Override
	public TransferType getType() {
		return TransferType.IMAGE;
	}

	@Override
	public void destroy() {
		timer.cancel();
	}

	@Override
	public void setTotalSubmittedImageCount(long totalSubmittedImageCount) {
		setExpectedNumberOfSubmissions(super.getTotalSubmittedImageCount(), totalSubmittedImageCount);
		super.setTotalSubmittedImageCount(totalSubmittedImageCount);

		// Check to see if this new image count indicates the series is complete and
		// submit if needed
		if (isAllImagesDone()) {
			executeImageCommand(null);
		}
	}

	/**
	 * Method to set number of expected submissions for image level transfer
	 * 
	 * @param newTotal
	 * @param l
	 */
	private void setExpectedNumberOfSubmissions(long currentTotal, long newTotal) {
		long diff = newTotal - currentTotal;

		numSubmissionsExpected += diff / AUTOTRANSFER_MIN_CHUNKSIZE;

		// If we are not exact multiple one extra transfer is needed
		if (diff % AUTOTRANSFER_MIN_CHUNKSIZE != 0) {
			numSubmissionsExpected += 1;
		}

		// For re-submission account for one more submission as we might start from
		// middle
		if (currentTotal > 0 && currentTotal % AUTOTRANSFER_MIN_CHUNKSIZE != 0) {
			numSubmissionsExpected += 1;
		}

		LOG.log(Level.SEVERE, "@@@{0} Level Task submitted for seriesUID: {1} numSubmissionsExpected :{2}",
				new Object[] { getType(), seriesUID, numSubmissionsExpected });
	}

	private boolean isConditionMet(ImageStatusDomainObject imageInstalledDomainObject) {
		String imageseriesUID = imageInstalledDomainObject.getSeriesUID();
		/*
		 * Need to compare seriesUID for image level transfer as this is a function of
		 * series level instance. Also the imagecount should be done at series level and
		 * not at exam level
		 */
		return seriesUID.equals(imageseriesUID);
	}

	/**
	 * Executes the imageCommand for the specified imageUID
	 * 
	 * @param imageUID The imageUID to submit. If null, does not submit unless to
	 *                 flush.
	 */
	private void executeImageCommand(String imageUID) {

		if (imageUIDString == null) {
			imageUIDString = new StringBuilder();
		}
		// execute(hostname, uid, flush(Y/N), taskId)
		// If all images are done or we reached the threshold flush it else
		// keep adding
		String hostname = super.getHostname();
		JobPriority priority = super.getDomainObject().getPriority();
		String priorityString = priority == null ? "" : priority.name();

		if (getExpectedImageCount() != 0 && actualSubmission < numSubmissionsExpected
				&& (isAllImagesDone() || getExpectedImageCount() % AUTOTRANSFER_MIN_CHUNKSIZE == 0)) {
			try {
				imageCommand.execute(hostname, imageUID, "true", getUniqueKey(), priorityString);
				imageUIDString.append(imageUID);
			} catch (Exception e) {
				LOG.log(Level.INFO, "Querying image must have failed for UID {0}", imageUID);
			}
			actualSubmission++; // to track total submissions so that we do not end up submitting again.
			super.setState(ControllerState.SUBMITTED);
			LOG.log(Level.SEVERE, "@@@{0} Level job pushed to host {1} for UIDs {2}",
					new Object[] { getType(), hostname, imageUIDString });
			// Submit and reset
			uidList.add(imageUIDString.toString());
			imageUIDString = new StringBuilder();
		} else if (imageUID != null) {
			try {
				imageCommand.execute(hostname, imageUID, "false", getUniqueKey(), priorityString);
				imageUIDString.append(imageUID + "\\");
			} catch (Exception e) {
				LOG.severe("Querying image must have failed for UID: " + imageUID);
			}
		}
	}

	/**
	 * Method to prepare unique Key for image batch transfer
	 * 
	 * @return uniqueKey
	 */
	private String getUniqueKey() {
		if (uniqueKey == null) {
			uniqueKey = jobkey.getHostname() + "_" + jobkey.getSessionKey() + "_" + jobkey.getId();
		}
		return uniqueKey;
	}

	/**
	 * Method to get job ID and add a job monitor
	 * 
	 * @param target
	 * @param UID
	 */
	private boolean isJobMatching(String uid, String target) {
		return (uidList.contains(uid) && super.getHostname().equals(target));
	}

	/**
	 * Method to get correct progress String based on progress status received.
	 * 
	 * @param progressString
	 * @return
	 */
	private String getProgressString(String progressString, long jobHandle) {
		if (progressString != null) {
			Matcher nOfMMatcher = N_OF_M_PATTERN.matcher(progressString.trim());
			if (nOfMMatcher.find()) {
				// Append previous success amount count to send correct progress for IMAGE case
				Matcher numberMatcher = ONLY_DIGITS.matcher(nOfMMatcher.group());
				// Fetch first integer
				numberMatcher.find();
				int first = Integer.parseInt(numberMatcher.group());
				// Fetch second integer
				numberMatcher.find();
				int second = Integer.parseInt(numberMatcher.group());

				if (second >= first) {
					progressString = String.valueOf(getTransferredImageCount() + first);
				} else {
					progressString = String.valueOf(getTransferredImageCount() + second);
				}
				// all images transferred for the set add
				if (second > 0 && second == first && !completedJobsMap.containsKey(jobHandle)) {
					completedJobsMap.put(jobHandle, (long) second);
				}
			} else {
				// This means that new event has now new status or failure. In both cases status
				// should remain same.
				progressString = lastProgressStatus;
			}
		} else {
			// This is to avoid any error condition on the UI side.
			progressString = lastProgressStatus;
		}
		LOG.log(Level.INFO, "ImageLevel progressString sent: {0}", progressString);
		return progressString;
	}

	protected void handleSuccess(long jobHandle, String progressString, SeriesType seriesType) {
		// override to nothing as we keep this controller open until timeout
	}

	/**
	 * Get total number of images transferred
	 * 
	 * @return
	 */
	private long getTransferredImageCount() {
		long count = 0;
		for (long value : completedJobsMap.values()) {
			count += value;
		}
		return count;
	}

}
