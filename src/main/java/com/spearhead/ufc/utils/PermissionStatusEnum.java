/**
 * 
 */
package com.spearhead.ufc.utils;

/**
 * @author manikandan.m
 * 
 *         special permission status with value
 * 
 *         it for static values
 */
public enum PermissionStatusEnum {

	PENDING(1), APPROVED(2), REJECTED(3);

	public final int permissionStatusId;

	private PermissionStatusEnum(int permissionStatusId) {
		this.permissionStatusId = permissionStatusId;
	}

	public static String getValue(Integer value) {
		for (PermissionStatusEnum e : PermissionStatusEnum.values()) {
			if (e.permissionStatusId == value) {
				return e.name();
			}
		}
		return null;
	}

}
