package com.polymir.Calibration.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
public class StorageProperties {

	private String location = "upload-dir";
	private String addonParts = "parts-dir";

	public String getAddonParts() {
		return addonParts;
	}

	public void setAddonParts(String addonParts) {
		this.addonParts = addonParts;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
