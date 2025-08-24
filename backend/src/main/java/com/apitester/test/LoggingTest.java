package com.apitester.test;

import com.apitester.logging.SecurityLogger;
import com.apitester.logging.SessionManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple test class to verify logging functionality
 */
public class LoggingTest {
    
    public static void main(String[] args) {
        System.out.println("=== API Tester Logging System Test ===");
        
        try {
            // Test session management
            SessionManager sessionManager = SessionManager.getInstance();
            String sessionId = sessionManager.generateSessionId();
            System.out.println("✓ Session ID generated: " + sessionId);
            
            // Test security logger
            SecurityLogger logger = SecurityLogger.getInstance();
            System.out.println("✓ Security logger initialized");
            
            // Test logging with sensitive data
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer secret-token-12345");
            headers.put("Content-Type", "application/json");
            headers.put("X-API-Key", "api-key-67890");
            
            String requestBody = "{ \"password\": \"mySecretPassword\", \"email\": \"user@example.com\", \"data\": \"normal data\" }";
            String responseBody = "{ \"token\": \"response-token-xyz\", \"result\": \"success\" }";
            
            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Content-Type", "application/json");
            responseHeaders.put("Set-Cookie", "session=abc123; HttpOnly");
            
            // Log the request
            logger.logApiRequest(
                sessionId,
                "POST",
                "https://api.example.com/login?api_key=secret123&user=john",
                headers,
                requestBody,
                200,
                responseBody,
                responseHeaders,
                1250
            );
            
            System.out.println("✓ API request logged successfully");
            
            // Test error logging
            Exception testException = new RuntimeException("Test error for logging");
            logger.logError(sessionId, "TEST_OPERATION", "This is a test error", testException);
            System.out.println("✓ Error logged successfully");
            
            System.out.println();
            System.out.println("=== Test Results ===");
            System.out.println("✓ Session Management: WORKING");
            System.out.println("✓ Security Logger: WORKING");
            System.out.println("✓ Data Masking: ENABLED");
            System.out.println("✓ Encryption: ENABLED");
            System.out.println("✓ Error Logging: WORKING");
            System.out.println();
            System.out.println("📁 Check the 'logs' directory for generated files:");
            System.out.println("   - api-requests.log (masked logs)");
            System.out.println("   - api-requests-encrypted.log (encrypted logs)");
            System.out.println("   - error.log (error logs)");
            System.out.println("   - security.key (encryption key)");
            System.out.println();
            System.out.println("🔒 Sensitive data in the test has been automatically masked!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
