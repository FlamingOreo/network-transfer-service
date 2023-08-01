package com.gehc.platform.networktransfer.database;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ge.hc.nuevo.sessions.autotasks.NuevoSystem;
import com.ge.med.terra.tap.dm.DMSession;
import com.ge.med.terra.tap.dm.DMSystem;
import com.gehc.platform.networktransfer.manager.util.NetworkTransferConstants;

/**
 * Class to provide all types of DBSession
 * @author lg070170
 * 
 */
public class DBSession {

	private static final DBSession INSTANCE = new DBSession();
	private static final Logger log = Logger.getLogger(DBSession.class.getName());

	private DMSession localSession = null;
	// Key = hostname_type
	private Map<String, DMSession> remoteSessionMap = new HashMap<>();
	private DMSession jobManagementSession = null;
	private DMSystem sys = null;
	
	public static DBSession getInstance() {
		return INSTANCE;
	}

	private DBSession() {
		DMSystem.insertSystem(NetworkTransferConstants.DBX,
				NetworkTransferConstants.LOCALDBSESSION);
		DMSession.insertSession(NetworkTransferConstants.JOBSESSION,
				NetworkTransferConstants.NUJOBSESSION);
		DMSystem.insertSystem(NetworkTransferConstants.NUEVO, new NuevoSystem());
		sys = DMSystem.getDMSystem(NetworkTransferConstants.NUEVO);
	}

	/**
	 * Facade to creates DB Session
	 * 
	 * @param sessionArguments
	 * @return
	 */
	public DMSession getLocalDBSession() {
		if (localSession == null) {
				localSession = new DMSession(new String[] { "dbx", "dbexpress", "true" });
		}
		return localSession;
	}
	
	/**
	 * Method to get remote connection to hosts 
	 * @param hostname
	 * @param type
	 * @return
	 */
	public DMSession getRemoteDBSession(String hostname, String type) {
		String key = hostname + "_" + type;
		if (!remoteSessionMap.containsKey(key)) {			
			DMSession jobNWSes = sys.getSystemSession(type, hostname);
			remoteSessionMap.put(key, jobNWSes);
		} 
		return remoteSessionMap.get(key);
	}
	
	/**
	 * Method to get job management session connection
	 * @return
	 */
	public DMSession getJobManagementSession() {
		if (jobManagementSession == null) {
			jobManagementSession = new DMSession(new String[]{NetworkTransferConstants.NUJOBSESSION, "", ""});
		}
		return jobManagementSession;
	}
	
	public void closeAllSessions() {
		//TODO: Need to close all sessions
	}
}


