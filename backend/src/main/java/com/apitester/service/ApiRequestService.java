package com.apitester.service;

import com.apitester.model.ApiRequest;
import com.apitester.model.ApiResponse;
import com.apitester.logging.SecurityLogger;
import com.apitester.logging.SessionManager;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ApiRequestService {
    
    private static final Logger logger = Logger.getLogger(ApiRequestService.class.getName());
    private static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    
    // Configure TLS for Java 8 compatibility with TLS versions below 1.3
    static {
        // Enable TLS 1.2, 1.1, and 1.0 only (excluding TLS 1.3 as requested)
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        
        // Explicitly disable TLS 1.3 (though Java 8 doesn't have it by default)
        System.setProperty("jdk.tls.disabledAlgorithms", "TLSv1.3,SSLv3,RC4,DES,MD5withRSA,DH keySize < 1024");
        
        // Enable Server Name Indication (SNI) for modern HTTPS sites like api.postalpincode.in
        System.setProperty("jsse.enableSNIExtension", "true");
        
        // Disable SSL/TLS hostname verification for testing (can be enabled for production)
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        
        // Enable strong cipher suites compatible with Java 8 and TLS < 1.3
        System.setProperty("https.cipherSuites", 
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," +
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256," +
            "TLS_RSA_WITH_AES_256_GCM_SHA384," +
            "TLS_RSA_WITH_AES_128_GCM_SHA256");
        
        // Enable AlgorithmConstraints to work with Java 8
        System.setProperty("jdk.certpath.disabledAlgorithms", "MD2, MD5, RSA keySize < 1024");
        
        // For debugging TLS issues (can be removed in production)
        // System.setProperty("javax.net.debug", "ssl:handshake");
        
        logger.info("TLS configuration initialized for Java 8 compatibility with TLS versions < 1.3");
    }
    
    private final SecurityLogger securityLogger;
    private final SessionManager sessionManager;
    private final CloseableHttpClient httpClient;
    
    public ApiRequestService() {
        this.securityLogger = SecurityLogger.getInstance();
        this.sessionManager = SessionManager.getInstance();
        this.httpClient = createSecureHttpClient();
    }
    
    private CloseableHttpClient createSecureHttpClient() {
        try {
            // Create SSL context compatible with Java 8 - using TLSv1.2 which is well supported
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            
            // For production, you might want to use proper certificate validation
            // For development/testing with various APIs, we'll use a permissive trust manager
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        // Accept all client certificates for testing
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        // Accept all server certificates for testing (allows connection to all APIs)
                    }
                }
            };
            
            // Initialize SSL context with the permissive trust manager
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // Create SSL socket factory with Java 8 compatible TLS versions (< 1.3)
            // This configuration supports apis like postalpincode.in and weatherapi.com
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, 
                new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"},  // Java 8 compatible versions < 1.3
                null,  // Use default cipher suites
                NoopHostnameVerifier.INSTANCE  // Disable hostname verification for testing
            );
            
            // Configure request timeouts and behavior
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setRedirectsEnabled(true)
                .setMaxRedirects(5)  // Increased for some APIs that might redirect
                .setCircularRedirectsAllowed(false)
                .build();
            
            // Build HTTP client with enhanced configuration for Java 8 and various APIs
            return HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent("API-Tester/1.0 (Java/1.8; Tomcat/8.5)")
                .setMaxConnTotal(100)  // Pool connections for better performance
                .setMaxConnPerRoute(20)
                .build();
                
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // Fallback to default client with basic configuration for Java 8
            logger.warning("Failed to create secure HTTP client, falling back to default: " + e.getMessage());
            try {
                RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                    .setConnectTimeout(CONNECTION_TIMEOUT)
                    .setSocketTimeout(READ_TIMEOUT)
                    .setRedirectsEnabled(true)
                    .setMaxRedirects(5)
                    .setCircularRedirectsAllowed(false)
                    .build();
                
                return HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setUserAgent("API-Tester/1.0 (Java/1.8; Tomcat/8.5)")
                    .setMaxConnTotal(50)
                    .setMaxConnPerRoute(10)
                    .build();
            } catch (Exception fallbackException) {
                logger.warning("Fallback HTTP client creation failed, using system default: " + fallbackException.getMessage());
                return HttpClients.createDefault();
            }
        }
    }
    
    public ApiResponse processRequest(ApiRequest apiRequest) {
        return processRequest(apiRequest, null);
    }
    
    public ApiResponse processRequest(ApiRequest apiRequest, String sessionId) {
        long startTime = System.currentTimeMillis();
        
        // Generate or get session ID
        if (sessionId == null) {
            sessionId = sessionManager.generateSessionId();
        } else {
            sessionId = sessionManager.getOrCreateSessionId(sessionId);
        }
        
        try {
            // Use the URL directly as provided by the user
            String url = apiRequest.getUrl();
            logger.info("Processing API request for URL: " + url);
            
            // Validate URL format
            if (url != null && !url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
                // Assume https if no protocol specified (for better security)
                url = "https://" + url;
                logger.info("Added HTTPS protocol to URL: " + url);
            }
            
            HttpRequestBase httpRequest = createHttpRequest(apiRequest.getMethod(), url);
            
            // Set headers
            if (apiRequest.getHeaders() != null) {
                for (Map.Entry<String, String> header : apiRequest.getHeaders().entrySet()) {
                    if (header.getKey() != null && !header.getKey().trim().isEmpty() && 
                        header.getValue() != null && !header.getValue().trim().isEmpty()) {
                        httpRequest.setHeader(header.getKey().trim(), header.getValue().trim());
                    }
                }
            }
            
            // Set request body for POST, PUT, PATCH
            if (httpRequest instanceof HttpPost || httpRequest instanceof HttpPut || httpRequest instanceof HttpPatch) {
                String body = apiRequest.getBody();
                if (body != null && !body.trim().isEmpty()) {
                    StringEntity entity = new StringEntity(body, "UTF-8");
                    entity.setContentType("application/json");
                    
                    if (httpRequest instanceof HttpPost) {
                        ((HttpPost) httpRequest).setEntity(entity);
                    } else if (httpRequest instanceof HttpPut) {
                        ((HttpPut) httpRequest).setEntity(entity);
                    } else if (httpRequest instanceof HttpPatch) {
                        ((HttpPatch) httpRequest).setEntity(entity);
                    }
                }
            }
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                int responseCode = response.getStatusLine().getStatusCode();
                String responseMessage = response.getStatusLine().getReasonPhrase();
                
                // Get response headers
                Map<String, String> responseHeaders = new HashMap<>();
                for (org.apache.http.Header header : response.getAllHeaders()) {
                    responseHeaders.put(header.getName(), header.getValue());
                }
                
                // Get response body
                String responseBody = "";
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    responseBody = EntityUtils.toString(entity, "UTF-8");
                }
                
                // Calculate response size
                int responseSize = responseBody.getBytes("UTF-8").length;
                
                // Create response object
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setStatus(responseCode);
                apiResponse.setStatusText(responseMessage);
                apiResponse.setHeaders(responseHeaders);
                apiResponse.setBody(responseBody);
                apiResponse.setResponseTime(responseTime);
                apiResponse.setSize(responseSize);
                
                // Log the API request
                securityLogger.logApiRequest(
                    sessionId,
                    apiRequest.getMethod(),
                    url,
                    apiRequest.getHeaders(),
                    apiRequest.getBody(),
                    responseCode,
                    responseBody,
                    responseHeaders,
                    responseTime
                );
                
                return apiResponse;
            }
            
        } catch (Exception e) {
            // Handle errors
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log the error
            securityLogger.logError(sessionId, "API_REQUEST", e.getMessage(), e);
            
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setStatus(0);
            errorResponse.setStatusText("Request Failed");
            errorResponse.setBody("{ \"error\": \"" + e.getMessage() + "\", \"type\": \"" + e.getClass().getSimpleName() + "\" }");
            errorResponse.setResponseTime(responseTime);
            errorResponse.setSize(0);
            
            Map<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("Content-Type", "application/json");
            errorResponse.setHeaders(errorHeaders);
            
            // Log the failed API request
            securityLogger.logApiRequest(
                sessionId,
                apiRequest.getMethod(),
                apiRequest.getUrl(),
                apiRequest.getHeaders(),
                apiRequest.getBody(),
                0,
                errorResponse.getBody(),
                errorHeaders,
                responseTime
            );
            
            return errorResponse;
        }
    }
    
    private HttpRequestBase createHttpRequest(String method, String url) {
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(url);
            case "POST":
                return new HttpPost(url);
            case "PUT":
                return new HttpPut(url);
            case "DELETE":
                return new HttpDelete(url);
            case "PATCH":
                return new HttpPatch(url);
            case "HEAD":
                return new HttpHead(url);
            case "OPTIONS":
                return new HttpOptions(url);
            default:
                return new HttpGet(url);
        }
    }
    
    public void shutdown() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
