package com.gehc.platform.networktransfer.manager.core;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ge.med.ct.wfplat.system.rawimage.api.ImageStatusDomainObject;
import com.ge.med.ct.wfplat.utilities.ifc.command.Command;
import com.ge.med.ct.wfplat.utilities.ifc.command.ExecutionException;
import com.ge.med.terra.tap.dm.DMEvent;
import com.gehc.platform.networktransfer.command.ExamArchiveCommand;
import com.gehc.platform.networktransfer.command.ExamNetworkCommand;
import com.gehc.platform.networktransfer.command.SeriesArchiveCommand;
import com.gehc.platform.networktransfer.command.SeriesNetworkCommand;
import com.gehc.platform.networktransfer.database.QueryLocalDB;
import com.gehc.platform.networktransfer.manager.util.ControllerState;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;
import com.gehc.platform.networktransfer.manager.util.SeriesType;
import com.gehc.platform.networktransferservice.model.ExamData;
import com.gehc.platform.networktransferservice.model.HostType;
import com.gehc.platform.networktransferservice.model.JobPriority;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.SeriesData;
import com.gehc.platform.networktransferservice.model.TransferType;

public class ExamLevelTaskController extends BaseTaskController {

	private static final Map<SeriesType, Integer> SERIESNUMBERMAP = NetworkTransferConstants.SERIESNUMBERMAP;
	private static final Logger LOG = Logger.getLogger(ExamLevelTaskController.class.getName());
	private static final String ON = "ON";
	private static final String DOSESRFLAG = "DOSESRFLAG";
	private static final long TIMEOUT_INTERVAL = 15L * 1000L;
	private static final long MAX_WAIT_TIME = NetworkTransferConstants.HOUR_MS * MAX_WAIT_HOURS;
	private static final int MAX_TRIES = (int) (MAX_WAIT_TIME / TIMEOUT_INTERVAL);
	private Timer timer = new Timer("ExamLevelTaskController", true);
	private int numRetries = 0;
	private String examUID;
	private String seriesUIDForSC;
	private List<SeriesData> seriesList;
	private long imageCount = 0;

	private boolean isSCImage = false;
	private Command examCommand;
	private Command seriesCommand;
	private QueryLocalDB queryLocalDB = QueryLocalDB.getInstance();

	// Gets set to true if we perform submission after 1 hour of what images we have
	private boolean haveDoneFallbackSubmission = false;

	// Flag to keep track of if we have submitted when all of the images were
	// available
	private boolean haveSubmittedWhenAllImagesAvailable = false;

	public ExamLevelTaskController(NetworkTransferRequest req, DDSCommunicationHandler ddsCommunicationHandler,
			long newImageCount) {

		super(req, newImageCount);
		ExamData examData = req.getExamData();
		examUID = examData.getUid();
		seriesList = examData.getSeriesData();
		isSCImage = seriesList.size() == 1 && isSCImage(getSeriesTypeForJob());
		timer.schedule(timerTask, 0, TIMEOUT_INTERVAL);
		boolean isNetwork = HostType.NETWORK.equals(req.getHostType());
		examCommand = isNetwork ? new ExamNetworkCommand() : new ExamArchiveCommand();
		seriesCommand = isNetwork ? new SeriesNetworkCommand() : new SeriesArchiveCommand();
	}

	@Override
	public synchronized void destroy() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Override
	public TransferType getType() {
		return TransferType.EXAM;
	}

	@Override
	public void processImageInstalledNotification(ImageStatusDomainObject imageInstalledObj) {
		imageCount++;

	}

	@Override
	public void handleJobProgress(DMEvent ev) {
		try {
			submitJobIfDone();
		} catch (ExecutionException e) {
			e.printStackTrace(); // TODO
		}
	}

	@Override
	protected boolean isAllImagesDone() {
		boolean returnValue = false;

		if (isSCImage) {
			seriesUIDForSC = queryLocalDB.querySCImage(examUID, SERIESNUMBERMAP.get(getSeriesTypeForJob()));
			return (seriesUIDForSC != null);
		}

		returnValue = isSCImagesInstalled(examUID, SeriesType.DOSESC);
		if (!returnValue) {
			return returnValue;
		}

		String dicomSROption = System.getProperty(DOSESRFLAG);
		if ((ON.equals(dicomSROption))) {
			returnValue = isSCImagesInstalled(examUID, SeriesType.DOSESR);
			if (!returnValue) {
				return returnValue;
			}
		}

		// To handle whole procedure
		for (SeriesData seriesDataDomainObject : seriesList) {
			String seriesUID = seriesDataDomainObject.getUid();
			SeriesType seriesType = SeriesType.valueOf(seriesDataDomainObject.getType());
			if (isEmpty(seriesUID) && isSCImage(seriesType)) {
				returnValue = isSCImagesInstalled(examUID, seriesType);
			} else {
				int imagesCountForSeries = queryLocalDB.getImageCountForSeries(seriesUID);
				returnValue = (imagesCountForSeries == seriesDataDomainObject.getImageCount().intValue());
			}

			if (!returnValue) {
				break;
			}
		}

		return returnValue;
	}

	private boolean isSCImage(SeriesType seriesTypeParam) {
		return !seriesTypeParam.equals(SeriesType.CTIMAGE);
	}

	private TimerTask timerTask = new TimerTask() {

		@Override
		public void run() {
			try {
				submitJobIfDone();
			} catch (ExecutionException e) {

				String checkUID = getID();

				LOG.severe("@@@EXAM Level Task execution failed for UID:" + checkUID);
			}
		}
	};

	private boolean isEmpty(String seriesUIDLocal) {
		return (null == seriesUIDLocal || "".equals(seriesUIDLocal));
	}

	private boolean isSCImagesInstalled(String examUID, SeriesType seriesType) {
		String seriesUID = queryLocalDB.querySCImage(examUID, SERIESNUMBERMAP.get(seriesType));
		return (seriesUID != null);
	}

	private boolean isAllImagesInstalled() {
		numRetries++;
		return isAllImagesDone();
	}

	private SeriesType getSeriesTypeForJob() {
		return SeriesType.valueOf(seriesList.get(0).getType());
	}

	private void submitJobIfDone() // NOSONAR
			throws ExecutionException {
		TransferType transferTimeType = getType();
		String hostname = super.getHostname();
		JobPriority priority = super.getDomainObject().getPriority();
		/**
		 * Adding the logic below to make sure clean up of controller happens when
		 * images are not generated or failed to transfer after max retries
		 */
		if (numRetries == MAX_TRIES) {
			LOG.log(Level.SEVERE, "Images for job with UID {0} was not found in database even after max retries",
					examUID);
			if (isSCImage && (seriesUIDForSC == null || ControllerState.FAILED.equals(super.getState()))) {
				super.setState(ControllerState.COMPLETED);
				destroy();
				LOG.log(Level.SEVERE,
						"@@@{0} Level Task host {1} for seriesUID: {2}, SeriesType {3} marked COMPLETED for cleanup because its not installed after max retries",
						new Object[] { transferTimeType, hostname, seriesUIDForSC, getSeriesTypeForJob() });
				return;
			} else {
				int imagesCountForExam = queryLocalDB.getImagesCountForExam(examUID);
				if (imagesCountForExam == 0 || ControllerState.FAILED.equals(super.getState())) {
					super.setState(ControllerState.COMPLETED);
					destroy();
					LOG.log(Level.SEVERE,
							"@@@{0} Level Task pushed to host {1} for examUID: {2} marked COMPLETED for cleanup because its not installed after max retries",
							new Object[] { transferTimeType, hostname, examUID });
					return;
				}
			}
		}

		boolean haveTriedForAnHourOrMore = numRetries * TIMEOUT_INTERVAL >= NetworkTransferConstants.HOUR_MS;
		boolean shouldPerformFallbackSubmission = !haveDoneFallbackSubmission && haveTriedForAnHourOrMore;
		boolean allImagesAvailable = isAllImagesInstalled();
		if ((numRetries == MAX_TRIES || shouldPerformFallbackSubmission || allImagesAvailable)
				&& !haveSubmittedWhenAllImagesAvailable) {
			haveDoneFallbackSubmission = true;
			haveSubmittedWhenAllImagesAvailable = allImagesAvailable;
			String priorityString = priority == null ? "" : priority.name();
			if (isSCImage) {
				seriesCommand.execute(hostname, seriesUIDForSC, priorityString);
				LOG.log(Level.SEVERE, "@@@{0} Level Task pushed to host {1} for seriesUID: {2}, SeriesType {3}",
						new Object[] { transferTimeType, hostname, seriesUIDForSC, getSeriesTypeForJob() });
			} else {
				examCommand.execute(hostname, examUID, priorityString);
				LOG.log(Level.SEVERE, "@@@{0} Level Task pushed to host {1} for examUID: {2}",
						new Object[] { transferTimeType, hostname, examUID });
			}

			super.setState(ControllerState.SUBMITTED);
		}
	}

}
