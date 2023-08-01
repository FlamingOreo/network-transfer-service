/*
 * NetworkTransferServiceImpl.java
 *
 * Copyright (c) 2023 by General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
package com.gehc.platform.networktransferservice.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.gehc.platform.networktransfer.manager.api.NetworkTaskManager;
import com.gehc.platform.networktransfer.manager.exception.NetworkTransferJobNotFoundException;
import com.gehc.platform.networktransferservice.api.NetworkTransferService;
import com.gehc.platform.networktransferservice.exception.NetworkTransferServiceNotFoundException;
import com.gehc.platform.networktransferservice.model.NetworkTransferJob;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;


/**
 * Implementation of {@link NetworkTransferService } API,will be called by the {@link NetworkTransferService } Controller.
 */
@Component
public class NetworkTransferServiceImpl implements NetworkTransferService {
	@Autowired
	private NetworkTaskManager taskManager;
	
	private static final String CSE_PROPERTIES_FILE = "cse.properties";
	private static final Logger LOGGER = Logger.getLogger(NetworkTransferServiceImpl.class.getName());
	private static final String SYSTEM_PROPERTIES_FILE = "/usr/g/ctuser/wf/resources/system.properties";
	
	
	
	public NetworkTransferServiceImpl() {
		// Load CSE Properties
		ClassPathResource cpr = new ClassPathResource(CSE_PROPERTIES_FILE);
		Properties prop = new Properties();
		try (InputStream is = cpr.getInputStream()) {
			prop.load(is);
			for (Entry<Object, Object> entry : prop.entrySet()) {
				LOGGER.log(Level.SEVERE, "Setting {0} to {1}", new Object[] {entry.getKey(), entry.getValue()});
				System.setProperty(entry.getKey().toString(), entry.getValue().toString());
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read in CSE Properties", e);
		}
		
		File file = new File(SYSTEM_PROPERTIES_FILE);
		prop = new Properties();
		try (InputStream is = new FileInputStream(file)) {
			prop.load(is);
			for (Entry<Object, Object> entry : prop.entrySet()) {
				LOGGER.log(Level.SEVERE, "Setting {0} to {1}", new Object[] {entry.getKey(), entry.getValue()});
				System.setProperty(entry.getKey().toString(), entry.getValue().toString());
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read in CSE Properties", e);
		}
	}

    
    @Override
    public void deleteJob(String id) {
        try {
			taskManager.delete(id);
		} catch (NetworkTransferJobNotFoundException e) {
			throw new NetworkTransferServiceNotFoundException(e.getMessage());
		}
    }

    @Override
    public List<NetworkTransferJob> getAllJobs() {
        
        return taskManager.findAll();
    }


    
    @Override
    public NetworkTransferJob getJobById(String id) {
    	try {
			return taskManager.find(id);
		} catch (NetworkTransferJobNotFoundException e) {
			throw new NetworkTransferServiceNotFoundException(e.getMessage());
		}
        
    }


    
    @Override
    public NetworkTransferJob retryJob(String id) {
    	try {
			return taskManager.find(id);
		} catch (NetworkTransferJobNotFoundException e) {
			throw new NetworkTransferServiceNotFoundException(e.getMessage());
		}
       
    }


    
    @Override
    public NetworkTransferJob submitTransferTask(NetworkTransferRequest body) {
        return taskManager.submit(body);
    }


    
    @Override
    public void updateJobState(Object body, String id) {

    }


}
