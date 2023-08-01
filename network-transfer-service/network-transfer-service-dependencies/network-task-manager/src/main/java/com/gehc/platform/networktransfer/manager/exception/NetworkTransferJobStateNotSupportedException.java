package com.gehc.platform.networktransfer.manager.exception;

/**
 * Exception that is thrown when a network transfer job cannot be found
 * 
 * @author Matthew Flejter
 *
 */
public class NetworkTransferJobStateNotSupportedException extends Exception {

	private static final long serialVersionUID = -2401751554873128187L;
	
	public NetworkTransferJobStateNotSupportedException(String message) {
		super(message);
	}
	
	public NetworkTransferJobStateNotSupportedException(Throwable t) {
		super(t);
	}
	
	public NetworkTransferJobStateNotSupportedException(String message, Throwable t) {
		super(message, t);
	}

}
