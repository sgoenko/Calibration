package com.polymir.Calibration.storage;

public enum Part {
	HEADER("header"),
	PARAMETERS("parameters"),
	LOCAL_TAGS("localTags"),
	ROUTINES("routines"),
	FOOTER("footer");

	String value;

	Part(String value) {
		this.value = value;
	}
}
