package com.gehc.platform.networktransfer.manager.exception;

/**
 * Exception that is thrown when a network transfer job cannot be found
 * 
 * @author Matthew Flejter
 *
 */
public class NetworkTransferJobNotFoundException extends Exception {

	private static final long serialVersionUID = -2401751554873128187L;
	
	public NetworkTransferJobNotFoundException(String message) {
		super(message);
	}
	
	public NetworkTransferJobNotFoundException(Throwable t) {
		super(t);
	}
	
	public NetworkTransferJobNotFoundException(String message, Throwable t) {
		super(message, t);
	}

}
