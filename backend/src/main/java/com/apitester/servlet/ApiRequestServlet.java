package com.apitester.servlet;

import com.apitester.model.ApiRequest;
import com.apitester.model.ApiResponse;
import com.apitester.service.ApiRequestService;
import com.apitester.logging.SecurityLogger;
import com.apitester.logging.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet({"/api/request", "/api/raw-request"})
public class ApiRequestServlet extends HttpServlet {
    
    private ApiRequestService apiRequestService;
    private ObjectMapper objectMapper;
    private SecurityLogger securityLogger;
    private SessionManager sessionManager;
        
    @Override
    public void init() throws ServletException {
        apiRequestService = new ApiRequestService();
        objectMapper = new ObjectMapper();
        securityLogger = SecurityLogger.getInstance();
        sessionManager = SessionManager.getInstance();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        setCORSHeaders(response);
        
        String sessionId = null;
        
        try {
            // Get or create session ID
            sessionId = request.getHeader("X-Session-ID");
            if (sessionId == null) {
                sessionId = sessionManager.generateSessionId();
                response.setHeader("X-Session-ID", sessionId);
            } else {
                sessionId = sessionManager.getOrCreateSessionId(sessionId);
                response.setHeader("X-Session-ID", sessionId);
            }
            
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                requestBody.append(line);
            }
            
            ApiRequest apiRequest = objectMapper.readValue(requestBody.toString(), ApiRequest.class);
            ApiResponse apiResponse = apiRequestService.processRequest(apiRequest, sessionId);
            
            // Set response content type
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Check if this is a raw request endpoint
            String requestURI = request.getRequestURI();
            boolean isRawRequest = requestURI.endsWith("/raw-request");
            
            PrintWriter out = response.getWriter();
            
            if (isRawRequest) {
                // For raw requests, return only the response body as proper JSON
                response.setStatus(apiResponse.getStatus());
                
                // Add original response headers as custom headers with prefix
                if (apiResponse.getHeaders() != null) {
                    for (Map.Entry<String, String> header : apiResponse.getHeaders().entrySet()) {
                        String headerName = "X-Original-" + header.getKey().replace(" ", "-");
                        response.setHeader(headerName, header.getValue());
                    }
                }
                
                // Try to parse and reformat the JSON for better presentation
                String responseBody = apiResponse.getBody();
                try {
                    // Parse the JSON string and write it back as formatted JSON
                    Object jsonObject = objectMapper.readValue(responseBody, Object.class);
                    String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                    out.print(formattedJson);
                } catch (Exception jsonException) {
                    // If it's not valid JSON, return as is
                    out.print(responseBody);
                }
            } else {
                // For regular requests, return the full ApiResponse object
                out.print(objectMapper.writeValueAsString(apiResponse));
            }
            out.flush();
            
        } catch (Exception e) {
            // Log the error
            if (sessionId != null) {
                securityLogger.logError(sessionId, "SERVLET_ERROR", e.getMessage(), e);
            }
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            
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
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set CORS headers
        setCORSHeaders(response);
        
        // Set response content type
        response.setContentType("application/json");
        
        // Check the request URI to determine which endpoint was called
        String requestURI = request.getRequestURI();
        boolean isRawRequest = requestURI.endsWith("/raw-request");
        
        String sessionId = request.getHeader("X-Session-ID");
        if (sessionId == null) {
            sessionId = sessionManager.generateSessionId();
            response.setHeader("X-Session-ID", sessionId);
        }
        
        if (isRawRequest) {
            // For raw endpoint, return simple status
            try (PrintWriter out = response.getWriter()) {
                out.write("{\n" +
                        "  \"message\": \"API Tester Raw Service is running\",\n" +
                        "  \"version\": \"1.0.0\",\n" +
                        "  \"endpoint\": \"POST /api/raw-request - Returns raw API response body\",\n" +
                        "  \"java_version\": \"1.8 compatible\",\n" +
                        "  \"tomcat_version\": \"8.5 compatible\",\n" +
                        "  \"tls_support\": \"TLS 1.2, 1.1, 1.0\"\n" +
                        "}");
            }
        } else {
            // Create a simple API info response for regular endpoint
            ApiResponse apiInfo = new ApiResponse();
            apiInfo.setStatus(200);
            apiInfo.setStatusText("OK");
            apiInfo.setBody("{\n" +
                    "  \"message\": \"API Tester Service is running\",\n" +
                    "  \"version\": \"1.0.0\",\n" +
                    "  \"java_version\": \"1.8 compatible\",\n" +
                    "  \"tomcat_version\": \"8.5 compatible\",\n" +
                    "  \"tls_support\": \"TLS 1.2, 1.1, 1.0\",\n" +
                    "  \"endpoints\": {\n" +
                    "    \"POST /api/request\": \"Process API requests (wrapped response)\",\n" +
                    "    \"POST /api/raw-request\": \"Process API requests (raw response)\",\n" +
                    "    \"GET /api/request\": \"Get API status\"\n" +
                    "  }\n" +
                    "}");
            apiInfo.setHeaders(null); // No custom headers for this response
            apiInfo.setResponseTime(0);
            apiInfo.setSize(0);
            
            // Write JSON response
            try (PrintWriter out = response.getWriter()) {
                String jsonResponse = objectMapper.writeValueAsString(apiInfo);
                out.write(jsonResponse);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = response.getWriter()) {
                    out.write("{\"error\": \"Failed to generate API info: " + e.getMessage() + "\"}");
                }
            }
        }
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Session-ID");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Expose-Headers", "X-Session-ID");
    }
}
