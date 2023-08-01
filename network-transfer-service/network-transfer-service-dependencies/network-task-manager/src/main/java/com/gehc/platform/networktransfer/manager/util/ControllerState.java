package com.gehc.platform.networktransfer.manager.util;

public enum ControllerState {
	NOTSTARTED(0),
	SUBMITTED(1),
	INPROGRESS(2),
	PAUSED(3),
	COMPLETED(4),
	FAILED(5);

	private int value;

	private ControllerState(int value) {
		this.value = value;
	}

	/**
	 * @return the integer value associated with the enum.
	 */
	public int getValue() {
		return value;
	}


}
