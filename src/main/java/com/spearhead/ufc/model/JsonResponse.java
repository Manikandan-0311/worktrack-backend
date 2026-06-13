package com.spearhead.ufc.model;

public class JsonResponse {
    private boolean success;
    private Object responseData;
    private String message;

    public JsonResponse() {}

    public JsonResponse(boolean success, Object responseData, String message) {
        this.success = success;
        this.responseData = responseData;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Object getResponseData() { return responseData; }
    public void setResponseData(Object responseData) { this.responseData = responseData; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // Static factory method for convenience
    public static JsonResponse of(boolean success, Object responseData, String message) {
        return new JsonResponse(success, responseData, message);
    }
}
