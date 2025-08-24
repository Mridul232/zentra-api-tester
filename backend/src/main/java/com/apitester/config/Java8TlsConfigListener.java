package com.apitester.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Servlet Context Listener to configure Java 8 TLS settings at application startup.
 * This ensures that the application can connect to modern HTTPS APIs even when
 * running on Java 8 (JDK 1.8.0_51) and Tomcat 8.5.87.
 */
@WebListener
public class Java8TlsConfigListener implements ServletContextListener {
    
    private static final Logger logger = Logger.getLogger(Java8TlsConfigListener.class.getName());
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing Java 8 TLS configuration for API Tester...");
        
        try {
            // Configure TLS protocols for Java 8 (versions < 1.3)
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.1,TLSv1");
            
            // Explicitly disable TLS 1.3 and other insecure protocols
            System.setProperty("jdk.tls.disabledAlgorithms", 
                "TLSv1.3,SSLv3,RC4,DES,MD5withRSA,DH keySize < 1024,EC keySize < 224");
            
            // Enable Server Name Indication (SNI) for modern HTTPS sites
            System.setProperty("jsse.enableSNIExtension", "true");
            
            // Configure certificate validation settings
            System.setProperty("com.sun.net.ssl.checkRevocation", "false");
            
            // Set strong cipher suites compatible with Java 8 and TLS < 1.3
            System.setProperty("https.cipherSuites", 
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," +
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256," +
                "TLS_RSA_WITH_AES_256_GCM_SHA384," +
                "TLS_RSA_WITH_AES_128_GCM_SHA256");
            
            // Configure security algorithms for Java 8
            System.setProperty("jdk.certpath.disabledAlgorithms", 
                "MD2, MD5, RSA keySize < 1024, DSA keySize < 1024");
            
            // Configure HTTP client settings for better API compatibility
            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", "50");
            System.setProperty("http.maxRedirects", "5");
            
            // Configure DNS cache settings
            System.setProperty("networkaddress.cache.ttl", "60");
            System.setProperty("networkaddress.cache.negative.ttl", "10");
            
            // Log the configuration
            logger.info("Java 8 TLS configuration completed successfully (TLS < 1.3)");
            logger.info("Supported TLS protocols: " + System.getProperty("https.protocols"));
            logger.info("Disabled TLS algorithms: " + System.getProperty("jdk.tls.disabledAlgorithms"));
            logger.info("SNI Extension enabled: " + System.getProperty("jsse.enableSNIExtension"));
            
            // Log system information for debugging
            logger.info("Java version: " + System.getProperty("java.version"));
            logger.info("Java runtime: " + System.getProperty("java.runtime.version"));
            logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            
        } catch (Exception e) {
            logger.severe("Failed to configure Java 8 TLS settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("API Tester application context destroyed");
    }
}
