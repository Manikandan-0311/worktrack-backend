package com.spearhead.ufc.dto;

import java.util.List;

public class EmployeeLocationAccessDTO {
    private Integer orgId;
    private Integer defaultLocationId;
    private List<Integer> enabledLocationIds;

    public Integer getOrgId() {
        return orgId;
    }

    public void setOrgId(Integer orgId) {
        this.orgId = orgId;
    }

    public Integer getDefaultLocationId() {
        return defaultLocationId;
    }

    public void setDefaultLocationId(Integer defaultLocationId) {
        this.defaultLocationId = defaultLocationId;
    }

    public List<Integer> getEnabledLocationIds() {
        return enabledLocationIds;
    }

    public void setEnabledLocationIds(List<Integer> enabledLocationIds) {
        this.enabledLocationIds = enabledLocationIds;
    }
}
