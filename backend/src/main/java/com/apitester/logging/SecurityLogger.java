package com.apitester.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Professional logging system with encryption and masking capabilities
 */
public class SecurityLogger {
    
    private static final String LOG_DIR = System.getProperty("catalina.base") != null ? 
        System.getProperty("catalina.base") + "/webapps/api-tester/logs" : "logs";
    private static final String API_LOG_FILE = "api-requests.log";
    private static final String ENCRYPTED_LOG_FILE = "api-requests-encrypted.log";
    private static final String KEY_FILE = "security.key";
    
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // Sensitive data patterns for masking
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)(password|pwd|pass)\\s*[:=]\\s*[\"']?([^\\s,\"'\\]\\}]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(token|auth|authorization|bearer)\\s*[:=]\\s*[\"']?([^\\s,\"'\\]\\}]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(api[_-]?key|apikey|key)\\s*[:=]\\s*[\"']?([^\\s,\"'\\]\\}]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(secret|private[_-]?key)\\s*[:=]\\s*[\"']?([^\\s,\"'\\]\\}]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(credit[_-]?card|card[_-]?number|ccn)\\s*[:=]\\s*[\"']?([\\d\\s-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(ssn|social[_-]?security)\\s*[:=]\\s*[\"']?([\\d-]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(email|e[_-]?mail)\\s*[:=]\\s*[\"']?([\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(phone|tel|mobile)\\s*[:=]\\s*[\"']?([\\d\\s\\+\\-\\(\\)]+)", Pattern.CASE_INSENSITIVE)
    );
    
    // Headers that should be masked
    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(Arrays.asList(
        "authorization", "x-api-key", "x-auth-token", "cookie", "set-cookie",
        "x-access-token", "x-refresh-token", "bearer", "basic"
    ));
    
    private static SecurityLogger instance;
    private SecretKey encryptionKey;
    private ObjectMapper objectMapper;
    private SimpleDateFormat dateFormat;
    
    private SecurityLogger() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        initializeLogging();
    }
    
    public static synchronized SecurityLogger getInstance() {
        if (instance == null) {
            instance = new SecurityLogger();
        }
        return instance;
    }
    
    private void initializeLogging() {
        try {
            // Create logs directory if it doesn't exist
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // Initialize or load encryption key
            this.encryptionKey = loadOrCreateEncryptionKey();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private SecretKey loadOrCreateEncryptionKey() throws Exception {
        File keyFile = new File(LOG_DIR, KEY_FILE);
        
        if (keyFile.exists()) {
            // Load existing key
            byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
            return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
        } else {
            // Generate new key
            KeyGenerator keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            
            // Save key to file
            Files.write(keyFile.toPath(), secretKey.getEncoded());
            
            return secretKey;
        }
    }
    
    /**
     * Log API request with comprehensive security measures
     */
    public void logApiRequest(String sessionId, String method, String url, 
                             Map<String, String> headers, String requestBody, 
                             int responseStatus, String responseBody, 
                             Map<String, String> responseHeaders, long responseTime) {
        
        try {
            LogEntry logEntry = new LogEntry();
            logEntry.timestamp = dateFormat.format(new Date());
            logEntry.sessionId = sessionId;
            logEntry.method = method;
            logEntry.url = maskSensitiveUrl(url);
            logEntry.requestHeaders = maskSensitiveHeaders(headers);
            logEntry.requestBody = maskSensitiveData(requestBody);
            logEntry.responseStatus = responseStatus;
            logEntry.responseBody = maskSensitiveData(responseBody);
            logEntry.responseHeaders = maskSensitiveHeaders(responseHeaders);
            logEntry.responseTime = responseTime;
            logEntry.logLevel = determineLogLevel(responseStatus);
            
            // Write to both regular and encrypted logs
            writeToRegularLog(logEntry);
            writeToEncryptedLog(logEntry);
            
        } catch (Exception e) {
            System.err.println("Failed to log API request: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String maskSensitiveUrl(String url) {
        if (url == null) return null;
        
        // Mask query parameters that might contain sensitive data
        String maskedUrl = url;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            maskedUrl = pattern.matcher(maskedUrl).replaceAll("$1=***MASKED***");
        }
        return maskedUrl;
    }
    
    private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        if (headers == null) return null;
        
        Map<String, String> maskedHeaders = new HashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (SENSITIVE_HEADERS.contains(key.toLowerCase())) {
                maskedHeaders.put(key, "***MASKED***");
            } else {
                maskedHeaders.put(key, value);
            }
        }
        return maskedHeaders;
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.trim().isEmpty()) return data;
        
        String maskedData = data;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            maskedData = pattern.matcher(maskedData).replaceAll("$1=***MASKED***");
        }
        return maskedData;
    }
    
    private String determineLogLevel(int responseStatus) {
        if (responseStatus >= 500) return "ERROR";
        if (responseStatus >= 400) return "WARN";
        if (responseStatus >= 300) return "INFO";
        return "INFO";
    }
    
    private void writeToRegularLog(LogEntry logEntry) throws IOException {
        File logFile = new File(LOG_DIR, API_LOG_FILE);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(objectMapper.writeValueAsString(logEntry) + "\n");
            writer.flush();
        }
    }
    
    private void writeToEncryptedLog(LogEntry logEntry) throws Exception {
        String jsonData = objectMapper.writeValueAsString(logEntry);
        String encryptedData = encrypt(jsonData);
        
        File encryptedLogFile = new File(LOG_DIR, ENCRYPTED_LOG_FILE);
        try (FileWriter writer = new FileWriter(encryptedLogFile, true)) {
            writer.write(encryptedData + "\n");
            writer.flush();
        }
    }
    
    private String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    public String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, "UTF-8");
    }
    
    /**
     * Log error events
     */
    public void logError(String sessionId, String operation, String error, Exception exception) {
        try {
            ErrorLogEntry errorEntry = new ErrorLogEntry();
            errorEntry.timestamp = dateFormat.format(new Date());
            errorEntry.sessionId = sessionId;
            errorEntry.operation = operation;
            errorEntry.error = error;
            errorEntry.exceptionClass = exception != null ? exception.getClass().getSimpleName() : "Unknown";
            errorEntry.stackTrace = exception != null ? getStackTrace(exception) : "No stack trace available";
            
            File errorLogFile = new File(LOG_DIR, "error.log");
            try (FileWriter writer = new FileWriter(errorLogFile, true)) {
                writer.write(objectMapper.writeValueAsString(errorEntry) + "\n");
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println("Failed to log error: " + e.getMessage());
        }
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\t").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Log entry structure for API requests
     */
    public static class LogEntry {
        public String timestamp;
        public String sessionId;
        public String method;
        public String url;
        public Map<String, String> requestHeaders;
        public String requestBody;
        public int responseStatus;
        public String responseBody;
        public Map<String, String> responseHeaders;
        public long responseTime;
        public String logLevel;
    }
    
    /**
     * Error log entry structure
     */
    public static class ErrorLogEntry {
        public String timestamp;
        public String sessionId;
        public String operation;
        public String error;
        public String exceptionClass;
        public String stackTrace;
    }
}
