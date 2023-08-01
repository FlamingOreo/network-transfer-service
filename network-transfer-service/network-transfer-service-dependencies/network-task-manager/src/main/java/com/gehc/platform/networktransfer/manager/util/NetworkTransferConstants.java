package com.gehc.platform.networktransfer.manager.util;
import java.util.HashMap;
import java.util.Map;

import com.ge.med.terra.tap.dm.DMJob;
import com.gehc.platform.networktransferservice.model.TransferState;

public final class NetworkTransferConstants {
	
	
	public static final String NETWORK_COMMAND_EXAM = "NETWORK_COMMAND_EXAM";
	public static final String NETWORK_COMMAND_SERIES = "NETWORK_COMMAND_SERIES";
	public static final String NETWORK_COMMAND_IMAGE = "NETWORK_COMMAND_IMAGE";
	
	public static final String ARCHIVE_COMMAND_EXAM = "ARCHIVE_COMMAND_EXAM";
	public static final String ARCHIVE_COMMAND_SERIES = "ARCHIVE_COMMAND_SERIES";
	public static final String ARCHIVE_COMMAND_IMAGE = "ARCHIVE_COMMAND_IMAGE";
	
	public static final String NETWORKS = "NETWORKS";
	public static final String REMOTE_ARCHIVES = "REMOTE_ARCHIVES";
	public static final String JOBSESSION = "nvo-jobsession";
	public static final String NUJOBSESSION = "com.ge.hc.nuevo.sessions.autotasks.NuJobSubmissionSession";
	public static final String LOCALDBSESSION = "com.ge.hc.nuevo.sessions.dbexpress.DBSession";
	public static final String DBX = "dbx";
	public static final String NUEVO = "nuevo";
	public static final long HOUR_MS = 3600000;
	
	private NetworkTransferConstants() {
	}

	@SuppressWarnings("serial")
	public static final Map<Integer, TransferState> JOBSTATEMAP = new HashMap<Integer, TransferState>() {
		{
			put(DMJob.STATE_SUCEEDED, TransferState.COMPLETED );
			put(DMJob.STATE_CANCELLED, TransferState.FAILED );
			put(DMJob.STATE_FAILED, TransferState.FAILED );
			put(DMJob.STATE_INPROGRESS, TransferState.INPROGRESS );
			put(DMJob.STATE_PAUSED, TransferState.PAUSED );
			put(DMJob.STATE_PENDING, TransferState.PENDING );
		}
	};
	
	@SuppressWarnings("serial")
	public static final Map<Integer, ControllerState> CONTROLLERSTATEMAP = new HashMap<Integer, ControllerState>() {
		{
			put(DMJob.STATE_SUCEEDED, ControllerState.COMPLETED );
			put(DMJob.STATE_CANCELLED, ControllerState.FAILED );
			put(DMJob.STATE_FAILED, ControllerState.FAILED );
			put(DMJob.STATE_INPROGRESS, ControllerState.INPROGRESS );
			put(DMJob.STATE_PAUSED, ControllerState.PAUSED );
			put(DMJob.STATE_PENDING, ControllerState.SUBMITTED );
		}
	};
	
	@SuppressWarnings("serial")
	public static final Map<SeriesType, Integer> SERIESNUMBERMAP = new HashMap<SeriesType, Integer>(){
		{
			put(SeriesType.DOSESC, 999);
			put(SeriesType.DOSESR, 997);
			put(SeriesType.INJECTORSC, 996);
			put(SeriesType.EKGSC, 599);
			put(SeriesType.SCREENSAVE, 99);
			put(SeriesType.SMARTPREPSC, 299);
			put(SeriesType.DICOMSC, 995);
		}
	};


	
	

	

}
