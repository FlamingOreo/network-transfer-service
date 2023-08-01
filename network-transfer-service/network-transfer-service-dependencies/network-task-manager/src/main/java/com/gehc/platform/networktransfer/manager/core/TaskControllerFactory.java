package com.gehc.platform.networktransfer.manager.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gehc.platform.networktransfer.manager.api.TaskController;
import com.gehc.platform.networktransferservice.model.NetworkTransferRequest;
/**
 * Creates controller for a given request
 * 
 * @author dp104662
 *
 */
@Component
public class TaskControllerFactory {
	@Autowired
	private DDSCommunicationHandler ddsCommunicationHandler;

	// TODO implement, add parameters to method
	public TaskController createController(NetworkTransferRequest domainObject, long newImageCount) {
		switch(domainObject.getTransferType()) {
		case SERIES:
			return new SeriesTaskController(domainObject, ddsCommunicationHandler, newImageCount);
		case EXAM:
			return new ExamLevelTaskController(domainObject, ddsCommunicationHandler, newImageCount);
		}
		return null;
	}
}
