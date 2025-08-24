package com.apitester.logging;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session management utility for logging
 */
public class SessionManager {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SESSION_ID_LENGTH = 16;
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    
    private static SessionManager instance;
    private final SecureRandom random;
    private final ConcurrentHashMap<String, SessionInfo> sessions;
    
    private SessionManager() {
        this.random = new SecureRandom();
        this.sessions = new ConcurrentHashMap<>();
        
        // Start cleanup thread
        startCleanupThread();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Generate a new session ID
     */
    public String generateSessionId() {
        StringBuilder sb = new StringBuilder(SESSION_ID_LENGTH);
        for (int i = 0; i < SESSION_ID_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        String sessionId = sb.toString();
        sessions.put(sessionId, new SessionInfo(System.currentTimeMillis()));
        
        return sessionId;
    }
    
    /**
     * Get or create session ID from request
     */
    public String getOrCreateSessionId(String clientSessionId) {
        if (clientSessionId != null && sessions.containsKey(clientSessionId)) {
            // Update last access time
            sessions.get(clientSessionId).lastAccess = System.currentTimeMillis();
            return clientSessionId;
        }
        
        return generateSessionId();
    }
    
    /**
     * Check if session is valid
     */
    public boolean isValidSession(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null) {
            return false;
        }
        
        if (System.currentTimeMillis() - info.lastAccess > SESSION_TIMEOUT) {
            sessions.remove(sessionId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Start cleanup thread to remove expired sessions
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000); // Check every 5 minutes
                    cleanupExpiredSessions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("SessionCleanup");
        cleanupThread.start();
    }
    
    /**
     * Remove expired sessions
     */
    private void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().lastAccess > SESSION_TIMEOUT
        );
    }
    
    /**
     * Session information holder
     */
    private static class SessionInfo {
        long lastAccess;
        
        SessionInfo(long creationTime) {
            this.lastAccess = creationTime;
        }
    }
}
