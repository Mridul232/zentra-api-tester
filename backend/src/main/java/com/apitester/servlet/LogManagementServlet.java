package com.apitester.servlet;

import com.apitester.logging.LogViewer;
import com.apitester.logging.SecurityLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet({"/api/logs/*", "/api/logs/all"})
public class LogManagementServlet extends HttpServlet {
    
    private LogViewer logViewer;
    private ObjectMapper objectMapper;
    private SecurityLogger securityLogger;
    
    // Simple security constants
    private static final String ADMIN_KEY_HASH = "sha256:8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"; // "admin123" hashed
    private static final String DECRYPTION_TOKEN = "DECRYPT_API_LOGS_2025";
    
    @Override
    public void init() throws ServletException {
        logViewer = new LogViewer();
        objectMapper = new ObjectMapper();
        securityLogger = SecurityLogger.getInstance();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        setCORSHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        String sessionId = request.getHeader("X-Session-ID");
        
        try {
            PrintWriter out = response.getWriter();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get recent logs
                int limit = parseInt(request.getParameter("limit"), 50);
                List<SecurityLogger.LogEntry> logs = logViewer.getRecentLogs(limit);
                out.print(objectMapper.writeValueAsString(logs));
                
            } else if (pathInfo.equals("/all")) {
                // Get ALL logs (for debugging session issues)
                System.out.println("LogManagementServlet: Fetching all logs");
                List<SecurityLogger.LogEntry> logs = logViewer.getRecentLogs(1000); // Get up to 1000 logs
                System.out.println("LogManagementServlet: Retrieved " + logs.size() + " total logs");
                out.print(objectMapper.writeValueAsString(logs));
                
            } else if (pathInfo.equals("/stats")) {
                // Get log statistics
                LogViewer.LogStatistics stats = logViewer.getLogStatistics();
                out.print(objectMapper.writeValueAsString(stats));
                
            } else if (pathInfo.equals("/errors")) {
                // Get error logs
                int limit = parseInt(request.getParameter("limit"), 20);
                List<SecurityLogger.ErrorLogEntry> errorLogs = logViewer.getErrorLogs(limit);
                out.print(objectMapper.writeValueAsString(errorLogs));
                
            } else if (pathInfo.equals("/session")) {
                // Get logs by session ID
                String targetSessionId = request.getParameter("sessionId");
                System.out.println("LogManagementServlet: Requested session ID: " + targetSessionId);
                if (targetSessionId != null) {
                    List<SecurityLogger.LogEntry> logs = logViewer.getLogsBySessionId(targetSessionId);
                    System.out.println("LogManagementServlet: Found " + logs.size() + " logs for session: " + targetSessionId);
                    out.print(objectMapper.writeValueAsString(logs));
                } else {
                    System.out.println("LogManagementServlet: No sessionId parameter provided");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{ \"error\": \"sessionId parameter is required\" }");
                }
                
            } else if (pathInfo.equals("/decrypted")) {
                // Get decrypted logs (admin function with security checks)
                System.out.println("LogManagementServlet: Decrypted logs access attempt from IP: " + request.getRemoteAddr());
                
                // Security Check 1: Authentication Header
                String authHeader = request.getHeader("Authorization");
                String adminKey = request.getHeader("X-Admin-Key");
                
                if (!isAuthorizedForDecryption(authHeader, adminKey, request)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{ \"error\": \"Unauthorized access to encrypted logs. Admin privileges required.\" }");
                    
                    // Log unauthorized access attempt
                    if (sessionId != null) {
                        securityLogger.logError(sessionId, "UNAUTHORIZED_DECRYPTION_ACCESS", 
                            "Unauthorized attempt to access encrypted logs from IP: " + request.getRemoteAddr(), null);
                    }
                    return;
                }
                
                // Security Check 2: Rate Limiting (simple implementation)
                String clientIP = request.getRemoteAddr();
                if (!checkRateLimit(clientIP)) {
                    response.setStatus(429); // 429 Too Many Requests
                    out.print("{ \"error\": \"Rate limit exceeded. Too many decryption requests.\" }");
                    return;
                }
                
                System.out.println("LogManagementServlet: Fetching decrypted logs for authorized user");
                int limit = parseInt(request.getParameter("limit"), 50);
                try {
                    List<SecurityLogger.LogEntry> logs = logViewer.getDecryptedLogs(limit);
                    System.out.println("LogManagementServlet: Retrieved " + logs.size() + " decrypted logs");
                    
                    // Log successful decryption access
                    if (sessionId != null) {
                        securityLogger.logApiRequest(sessionId, "GET", "/api/logs/decrypted", 
                            null, "", 200, "Decrypted logs accessed successfully", null, 0);
                    }
                    
                    out.print(objectMapper.writeValueAsString(logs));
                } catch (Exception e) {
                    System.out.println("LogManagementServlet: Error decrypting logs: " + e.getMessage());
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{ \"error\": \"Failed to decrypt logs: " + e.getMessage() + "\" }");
                }
                
            } else if (pathInfo.equals("/daterange")) {
                // Get logs by date range
                String startDateStr = request.getParameter("startDate");
                String endDateStr = request.getParameter("endDate");
                
                if (startDateStr != null && endDateStr != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date startDate = dateFormat.parse(startDateStr);
                    Date endDate = dateFormat.parse(endDateStr);
                    
                    List<SecurityLogger.LogEntry> logs = logViewer.getLogsByDateRange(startDate, endDate);
                    out.print(objectMapper.writeValueAsString(logs));
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{ \"error\": \"startDate and endDate parameters are required\" }");
                }
                
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{ \"error\": \"Log endpoint not found\" }");
            }
            
            out.flush();
            
        } catch (Exception e) {
            if (sessionId != null) {
                securityLogger.logError(sessionId, "LOG_SERVLET_ERROR", e.getMessage(), e);
            }
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print("{ \"error\": \"" + e.getMessage() + "\" }");
            out.flush();
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        setCORSHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        String sessionId = request.getHeader("X-Session-ID");
        
        try {
            PrintWriter out = response.getWriter();
            
            if (pathInfo != null && pathInfo.equals("/cleanup")) {
                // Clean old logs
                int daysToKeep = parseInt(request.getParameter("daysToKeep"), 30);
                logViewer.cleanOldLogs(daysToKeep);
                out.print("{ \"message\": \"Log cleanup completed\", \"daysKept\": " + daysToKeep + " }");
                
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{ \"error\": \"Log operation not found\" }");
            }
            
            out.flush();
            
        } catch (Exception e) {
            if (sessionId != null) {
                securityLogger.logError(sessionId, "LOG_SERVLET_POST_ERROR", e.getMessage(), e);
            }
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print("{ \"error\": \"" + e.getMessage() + "\" }");
            out.flush();
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        setCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Session-ID, X-Admin-Key");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Expose-Headers", "X-Session-ID");
        
        // Additional security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }
    
    private int parseInt(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // Rate limiting configuration
    private static final Map<String, Integer> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_HOUR = 10;
    
    /**
     * Check if the request is authorized for decryption access
     */
    private boolean isAuthorizedForDecryption(String authHeader, String adminKey, HttpServletRequest request) {
        // Method 1: Check Authorization header with Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (DECRYPTION_TOKEN.equals(token)) {
                return true;
            }
        }
        
        // Method 2: Check X-Admin-Key header with hashed password
        if (adminKey != null) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(adminKey.getBytes("UTF-8"));
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                String hashedKey = "sha256:" + hexString.toString();
                if (ADMIN_KEY_HASH.equals(hashedKey)) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Error hashing admin key: " + e.getMessage());
            }
        }
        
        // Method 3: Check if request comes from localhost (development only)
        String remoteAddr = request.getRemoteAddr();
        if ("127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr) || "localhost".equals(request.getRemoteHost())) {
            // Still require at least a basic auth parameter for localhost
            String localAuth = request.getParameter("local_auth");
            return "dev_access_2025".equals(localAuth);
        }
        
        return false;
    }
    
    /**
     * Simple rate limiting implementation
     */
    private boolean checkRateLimit(String clientIP) {
        String key = clientIP + "_" + (System.currentTimeMillis() / (60 * 60 * 1000)); // Per hour
        int currentCount = rateLimitMap.getOrDefault(key, 0);
        
        if (currentCount >= MAX_REQUESTS_PER_HOUR) {
            return false;
        }
        
        rateLimitMap.put(key, currentCount + 1);
        
        // Cleanup old entries (simple cleanup)
        if (rateLimitMap.size() > 1000) {
            rateLimitMap.clear();
        }
        
        return true;
    }
}
