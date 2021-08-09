package com.polymir.Calibration.storage;

public enum Part {
	HEADER("header"),
	PARAMETERS("parameters"),
	ROUTINES("routines"),
	FOOTER("footer");

	String value;

	Part(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
}
