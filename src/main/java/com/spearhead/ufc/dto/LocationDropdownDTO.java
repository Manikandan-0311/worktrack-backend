package com.spearhead.ufc.dto;

public class LocationDropdownDTO {
	public Integer locationId;
	public String locationName;

	public LocationDropdownDTO(Integer locationId, String locationName) {
		this.locationId = locationId;
		this.locationName = locationName;
	}
}