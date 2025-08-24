package com.apitester.logging;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility for viewing and managing logs
 */
public class LogViewer {
    
    private static final String LOG_DIR = System.getProperty("catalina.base") != null ? 
        System.getProperty("catalina.base") + "/webapps/api-tester/logs" : "logs";
    private static final String API_LOG_FILE = "api-requests.log";
    private static final String ENCRYPTED_LOG_FILE = "api-requests-encrypted.log";
    private static final String ERROR_LOG_FILE = "error.log";
    
    private final SecurityLogger securityLogger;
    private final ObjectMapper objectMapper;
    
    public LogViewer() {
        this.securityLogger = SecurityLogger.getInstance();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get recent log entries
     */
    public List<SecurityLogger.LogEntry> getRecentLogs(int limit) throws IOException {
        List<SecurityLogger.LogEntry> logs = new ArrayList<>();
        File logFile = new File(LOG_DIR, API_LOG_FILE);
        
        System.out.println("LogViewer: Getting recent logs from: " + logFile.getAbsolutePath());
        System.out.println("LogViewer: Log file exists: " + logFile.exists());
        
        if (!logFile.exists()) {
            System.out.println("LogViewer: Log file does not exist, returning empty list");
            return logs;
        }
        
        // Read the entire file content
        String content = new String(java.nio.file.Files.readAllBytes(logFile.toPath()));
        System.out.println("LogViewer: Read " + content.length() + " characters from log file");
        
        // Split on JSON object boundaries - look for }\n{ pattern, but also handle single objects
        List<String> jsonObjects = new ArrayList<>();
        
        if (content.trim().isEmpty()) {
            System.out.println("LogViewer: File is empty");
            return logs;
        }
        
        // Split by pattern: }\n{  (end of one object, start of another)
        String[] parts = content.split("\\}\\s*\\n\\s*\\{");
        
        System.out.println("LogViewer: Split into " + parts.length + " parts");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            
            // Fix the JSON by adding missing braces
            if (i == 0 && parts.length > 1) {
                // First part: add closing brace
                part = part + "}";
            } else if (i == parts.length - 1 && parts.length > 1) {
                // Last part: add opening brace
                part = "{" + part;
            } else if (parts.length > 1) {
                // Middle parts: add both braces
                part = "{" + part + "}";
            }
            // If there's only one part, it should be a complete JSON object
            
            jsonObjects.add(part);
        }
        
        System.out.println("LogViewer: Created " + jsonObjects.size() + " JSON objects");
        
        List<SecurityLogger.LogEntry> allEntries = new ArrayList<>();
        
        for (int i = 0; i < jsonObjects.size(); i++) {
            String jsonStr = jsonObjects.get(i);
            
            try {
                SecurityLogger.LogEntry entry = objectMapper.readValue(jsonStr, SecurityLogger.LogEntry.class);
                allEntries.add(entry);
                System.out.println("LogViewer: Successfully parsed entry " + (i+1) + " for session: " + entry.sessionId);
            } catch (Exception e) {
                System.out.println("LogViewer: Failed to parse JSON object " + (i+1) + ": " + e.getMessage());
                System.out.println("LogViewer: JSON content preview: " + jsonStr.substring(0, Math.min(200, jsonStr.length())));
                // Skip malformed entries
            }
        }
        
        // Get the last 'limit' entries
        int start = Math.max(0, allEntries.size() - limit);
        for (int i = start; i < allEntries.size(); i++) {
            logs.add(allEntries.get(i));
        }
        
        System.out.println("LogViewer: Returning " + logs.size() + " log entries");
        return logs;
    }
    
    /**
     * Get logs by date range
     */
    public List<SecurityLogger.LogEntry> getLogsByDateRange(Date startDate, Date endDate) throws IOException {
        List<SecurityLogger.LogEntry> logs = new ArrayList<>();
        File logFile = new File(LOG_DIR, API_LOG_FILE);
        
        if (!logFile.exists()) {
            return logs;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    SecurityLogger.LogEntry entry = objectMapper.readValue(line, SecurityLogger.LogEntry.class);
                    Date entryDate = dateFormat.parse(entry.timestamp);
                    
                    if (entryDate.after(startDate) && entryDate.before(endDate)) {
                        logs.add(entry);
                    }
                } catch (Exception e) {
                    // Skip malformed entries
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Get logs by session ID
     */
    public List<SecurityLogger.LogEntry> getLogsBySessionId(String sessionId) throws IOException {
        List<SecurityLogger.LogEntry> logs = new ArrayList<>();
        File logFile = new File(LOG_DIR, API_LOG_FILE);
        
        System.out.println("LogViewer: Looking for logs in: " + logFile.getAbsolutePath());
        System.out.println("LogViewer: Log file exists: " + logFile.exists());
        
        if (!logFile.exists()) {
            System.out.println("LogViewer: Log file does not exist, returning empty list");
            return logs;
        }
        
        // Read the entire file content
        String content = new String(java.nio.file.Files.readAllBytes(logFile.toPath()));
        System.out.println("LogViewer: Read " + content.length() + " characters from log file");
        
        // Split on JSON object boundaries - look for }\n{ pattern, but also handle single objects
        List<String> jsonObjects = new ArrayList<>();
        
        if (content.trim().isEmpty()) {
            System.out.println("LogViewer: File is empty");
            return logs;
        }
        
        // Split by pattern: }\n{  (end of one object, start of another)
        String[] parts = content.split("\\}\\s*\\n\\s*\\{");
        
        System.out.println("LogViewer: Split into " + parts.length + " parts");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            
            // Fix the JSON by adding missing braces
            if (i == 0 && parts.length > 1) {
                // First part: add closing brace
                part = part + "}";
            } else if (i == parts.length - 1 && parts.length > 1) {
                // Last part: add opening brace
                part = "{" + part;
            } else if (parts.length > 1) {
                // Middle parts: add both braces
                part = "{" + part + "}";
            }
            // If there's only one part, it should be a complete JSON object
            
            jsonObjects.add(part);
        }
        
        System.out.println("LogViewer: Created " + jsonObjects.size() + " JSON objects");
        
        int matchingEntries = 0;
        
        for (int i = 0; i < jsonObjects.size(); i++) {
            String jsonStr = jsonObjects.get(i);
            
            try {
                SecurityLogger.LogEntry entry = objectMapper.readValue(jsonStr, SecurityLogger.LogEntry.class);
                if (sessionId.equals(entry.sessionId)) {
                    logs.add(entry);
                    matchingEntries++;
                    System.out.println("LogViewer: Found matching log entry for session: " + sessionId);
                }
            } catch (Exception e) {
                System.out.println("LogViewer: Failed to parse JSON object " + (i+1) + ": " + e.getMessage());
                // Skip malformed entries
            }
        }
        
        System.out.println("LogViewer: Found " + matchingEntries + " matching entries for session: " + sessionId);
        return logs;
    }
    
    /**
     * Get error logs
     */
    public List<SecurityLogger.ErrorLogEntry> getErrorLogs(int limit) throws IOException {
        List<SecurityLogger.ErrorLogEntry> logs = new ArrayList<>();
        File logFile = new File(LOG_DIR, ERROR_LOG_FILE);
        
        if (!logFile.exists()) {
            return logs;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            List<String> lines = new ArrayList<>();
            
            // Read all lines
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            // Get last 'limit' lines
            int start = Math.max(0, lines.size() - limit);
            for (int i = start; i < lines.size(); i++) {
                try {
                    SecurityLogger.ErrorLogEntry entry = objectMapper.readValue(lines.get(i), SecurityLogger.ErrorLogEntry.class);
                    logs.add(entry);
                } catch (Exception e) {
                    // Skip malformed entries
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Decrypt and view encrypted logs (admin function)
     */
    public List<SecurityLogger.LogEntry> getDecryptedLogs(int limit) throws Exception {
        List<SecurityLogger.LogEntry> logs = new ArrayList<>();
        File encryptedLogFile = new File(LOG_DIR, ENCRYPTED_LOG_FILE);
        
        if (!encryptedLogFile.exists()) {
            return logs;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(encryptedLogFile))) {
            String line;
            List<String> lines = new ArrayList<>();
            
            // Read all lines
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            // Get last 'limit' lines
            int start = Math.max(0, lines.size() - limit);
            for (int i = start; i < lines.size(); i++) {
                try {
                    String decryptedLine = securityLogger.decrypt(lines.get(i));
                    SecurityLogger.LogEntry entry = objectMapper.readValue(decryptedLine, SecurityLogger.LogEntry.class);
                    logs.add(entry);
                } catch (Exception e) {
                    // Skip malformed entries
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Get log statistics
     */
    public LogStatistics getLogStatistics() throws IOException {
        LogStatistics stats = new LogStatistics();
        File logFile = new File(LOG_DIR, API_LOG_FILE);
        
        if (!logFile.exists()) {
            return stats;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    SecurityLogger.LogEntry entry = objectMapper.readValue(line, SecurityLogger.LogEntry.class);
                    stats.totalRequests++;
                    
                    if (entry.responseStatus >= 200 && entry.responseStatus < 300) {
                        stats.successfulRequests++;
                    } else if (entry.responseStatus >= 400 && entry.responseStatus < 500) {
                        stats.clientErrors++;
                    } else if (entry.responseStatus >= 500) {
                        stats.serverErrors++;
                    }
                    
                    stats.totalResponseTime += entry.responseTime;
                    
                    // Track methods
                    stats.methodCounts.put(entry.method, 
                        stats.methodCounts.getOrDefault(entry.method, 0) + 1);
                    
                } catch (Exception e) {
                    // Skip malformed entries
                }
            }
        }
        
        if (stats.totalRequests > 0) {
            stats.averageResponseTime = stats.totalResponseTime / stats.totalRequests;
        }
        
        return stats;
    }
    
    /**
     * Clean old logs (older than specified days)
     */
    public void cleanOldLogs(int daysToKeep) throws IOException {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        
        // Clean regular log file
        cleanLogFile(new File(LOG_DIR, API_LOG_FILE), cutoffTime, dateFormat);
        
        // Clean encrypted log file
        cleanLogFile(new File(LOG_DIR, ENCRYPTED_LOG_FILE), cutoffTime, dateFormat);
        
        // Clean error log file
        cleanLogFile(new File(LOG_DIR, ERROR_LOG_FILE), cutoffTime, dateFormat);
    }
    
    private void cleanLogFile(File logFile, long cutoffTime, SimpleDateFormat dateFormat) throws IOException {
        if (!logFile.exists()) {
            return;
        }
        
        File tempFile = new File(logFile.getParent(), logFile.getName() + ".tmp");
        List<String> linesToKeep = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // Parse timestamp from log entry
                    if (line.contains("\"timestamp\"")) {
                        // Extract timestamp
                        int timestampStart = line.indexOf("\"timestamp\":\"") + 13;
                        int timestampEnd = line.indexOf("\"", timestampStart);
                        String timestamp = line.substring(timestampStart, timestampEnd);
                        
                        Date logDate = dateFormat.parse(timestamp);
                        if (logDate.getTime() >= cutoffTime) {
                            linesToKeep.add(line);
                        }
                    }
                } catch (Exception e) {
                    // Keep line if we can't parse it
                    linesToKeep.add(line);
                }
            }
        }
        
        // Write cleaned content to temp file
        try (FileWriter writer = new FileWriter(tempFile)) {
            for (String line : linesToKeep) {
                writer.write(line + "\n");
            }
        }
        
        // Replace original file with cleaned file
        if (!logFile.delete() || !tempFile.renameTo(logFile)) {
            throw new IOException("Failed to replace log file");
        }
    }
    
    /**
     * Log statistics data structure
     */
    public static class LogStatistics {
        public int totalRequests = 0;
        public int successfulRequests = 0;
        public int clientErrors = 0;
        public int serverErrors = 0;
        public long totalResponseTime = 0;
        public long averageResponseTime = 0;
        public Map<String, Integer> methodCounts = new HashMap<>();
    }
}
