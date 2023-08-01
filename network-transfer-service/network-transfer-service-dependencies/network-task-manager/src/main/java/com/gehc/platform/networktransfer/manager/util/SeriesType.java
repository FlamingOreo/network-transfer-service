package com.gehc.platform.networktransfer.manager.util;
/**
 * 
 * @author dp104662
 *
 */
public enum SeriesType {
	
	CTIMAGE(0),
    DOSESR(1),
    DOSESC(3),
    EKGSC(4),
    SCREENSAVE(5),
    INJECTORSC(6),
	SMARTPREPSC(7),
	DICOMSC(8);

	private int value;

	private SeriesType(int value) {
		this.value = value;
	}

	/**
	 * @return the integer value associated with the enum.
	 */
	public int getValue() {
		return value;
	}

	
}
