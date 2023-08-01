package com.gehc.platform.networktransfer.manager.api;

import com.ge.med.ct.wfplat.system.rawimage.api.ImageStatusDomainObject;
import com.ge.med.terra.tap.dm.DMEvent;
import com.gehc.platform.networktransfer.manager.util.ControllerState;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
import com.gehc.platform.networktransferservice.model.TransferType;

public interface TaskController {

	/**
	 * Method to process image installed notification
	 * 
	 * @param imageInstalledObj
	 */
	public void processImageInstalledNotification(ImageStatusDomainObject imageInstalledObj);
	
	
	/**
	 * Method to handle DmEvent
	 * @param ev
	 */
	public void handleJobProgress(DMEvent ev);

	/**
	 * Method to get current controller state
	 * @return
	 */
	public ControllerState getState();
	
	/**
	 * Method to get total submitted image count for a controller
	 * @return
	 */
	public int getImageCountFromDatabase(String seriesUID);
	
	/**
	 * Get the transfer type of the controller
	 * @return
	 */
	public TransferType getType();
	
	/**
	 * Method to get submitted job domain object to be used for persistence
	 * @return
	 */
	public NetworkTransferRequest getDomainObject();
	
	/**
	 * Terminate the controller
	 */
	public void destroy();
	
	/**
	 * Indicates the characteristic UID for this controller.
	 * @return
	 */
	public String getID();


	/**
	 * @return the totalSubmittedImageCount
	 */
	long getTotalSubmittedImageCount();


	void setTotalSubmittedImageCount(long totalSubmittedImageCount);
}

