package com.apitester.model;

import java.util.Map;

public class ApiResponse {
    private int status;
    private String statusText;
    private Map<String, String> headers;
    private String body;
    private long responseTime;
    private int size;
    
    // Default constructor
    public ApiResponse() {}
    
    // Constructor with parameters
    public ApiResponse(int status, String statusText, Map<String, String> headers, 
                      String body, long responseTime, int size) {
        this.status = status;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
        this.responseTime = responseTime;
        this.size = size;
    }
    
    // Getters and Setters
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getStatusText() {
        return statusText;
    }
    
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public long getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
                "status=" + status +
                ", statusText='" + statusText + '\'' +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                ", responseTime=" + responseTime +
                ", size=" + size +
                '}';
    }
}
