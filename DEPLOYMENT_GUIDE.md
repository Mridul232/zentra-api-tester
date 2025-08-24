# API Tester - Java 8 & Tomcat 8.5.87 Deployment Guide

## Project Overview
This API Tester application has been configured to work with:
- **Java 8 (JDK 1.8.0_51)**
- **Tomcat 8.5.87**
- **TLS 1.2 support** for modern HTTPS APIs

## Key Configurations Made

### 1. Java 8 Compatibility
- Maven compiler set to Java 8 (`maven.compiler.source` and `maven.compiler.target` = 1.8)
- All dependencies are Java 8 compatible versions
- TLS configuration specifically designed for Java 8

### 2. TLS Configuration for Modern APIs
The application includes comprehensive TLS configuration to work with modern HTTPS APIs like:
- `https://api.postalpincode.in/pincode/110001`
- `http://api.weatherapi.com/v1/current.json?key=...`
- Any other HTTPS APIs

#### TLS Settings Applied:
- **Protocols**: TLS 1.2, TLS 1.1, TLS 1.0
- **SNI Extension**: Enabled for modern sites
- **Cipher Suites**: Strong encryption suites compatible with Java 8
- **Certificate Validation**: Configured for testing (can be tightened for production)

### 3. Tomcat 8.5.87 Integration
- Servlet API 3.1.0 (compatible with Tomcat 8.5)
- Deployment configuration for Tomcat 8.5 webapps directory
- Proper session management for Tomcat 8.5

## Deployment Instructions

### Prerequisites
1. **Java 8 (JDK 1.8.0_51)** installed
2. **Apache Tomcat 8.5.87** installed
3. **Maven 3.x** for building

### Building the Application
```bash
cd backend
mvn clean package
```

### Deployment Options

#### Option 1: Manual Deployment
1. Copy `target/api-tester.war` to Tomcat's `webapps` directory:
   ```
   C:/Program Files/Apache Software Foundation/Tomcat 8.5/webapps/
   ```
2. Start Tomcat
3. Access the application at: `http://localhost:8080/api-tester/`

#### Option 2: Maven Deployment (if configured)
```bash
mvn tomcat7:deploy
```

### Application Features

#### Quick API Tests
The application includes built-in test buttons for:
1. **Postal API Test (HTTPS)**: Tests `https://api.postalpincode.in/pincode/110001`
2. **Weather API Test (HTTP)**: Tests `http://api.weatherapi.com/v1/current.json?key=...`
3. **Custom HTTPS API**: Fill and test any HTTPS API

#### API Endpoints
- `GET /api/request` - API status and information
- `POST /api/request` - Process API requests (wrapped response)
- `POST /api/raw-request` - Process API requests (raw response)
- `GET /api/test-predefined?type=postal|weather` - Test predefined APIs
- `GET /api/test-custom?url=<url>&method=<method>` - Test custom APIs

## Testing Java 8 & TLS Compatibility

### 1. Access the Application
Open: `http://localhost:8080/api-tester/`

### 2. Quick Tests
Use the "Quick API Tests" section to:
- Test Postal API (HTTPS) - Verifies TLS 1.2 compatibility
- Test Weather API (HTTP) - Verifies basic HTTP functionality
- Test Custom HTTPS APIs - Verifies general HTTPS compatibility

### 3. Manual Testing
1. Enter any HTTPS API URL in the main form
2. Set appropriate headers
3. Click "Send" to test

## Technical Implementation Details

### TLS Configuration
Located in `ApiRequestService.java`:
- System properties set for TLS protocols
- Custom SSL context for Java 8
- HttpClient configured with TLS 1.2 support

### HTTP Client Configuration
- Connection timeouts: 30 seconds
- Read timeouts: 30 seconds
- Redirect handling: Up to 5 redirects
- User Agent: "API-Tester/1.0 (Java/1.8; Tomcat/8.5)"

### Session Management
- Session-based request tracking
- Comprehensive logging
- Security headers implementation

## Troubleshooting

### Common Issues and Solutions

#### 1. TLS Handshake Failures
If you encounter TLS handshake errors:
- Ensure Java 8 is properly installed
- Check that the target API supports TLS 1.2
- Enable SSL debugging: `System.setProperty("javax.net.debug", "ssl:handshake");`

#### 2. Connection Timeouts
- Check network connectivity
- Verify the target API is accessible
- Consider increasing timeout values in `ApiRequestService.java`

#### 3. Certificate Issues
- The application is configured to accept all certificates for testing
- For production, implement proper certificate validation

## Security Considerations

### For Development/Testing
- Certificate validation is relaxed for testing various APIs
- All TLS protocols are enabled
- CORS is configured for cross-origin requests

### For Production
Consider tightening:
- Certificate validation
- Allowed TLS protocols
- CORS configuration
- Input validation

## Dependencies

### Core Dependencies (Java 8 Compatible)
- `javax.servlet-api` 3.1.0
- `jackson-databind` 2.12.7.1
- `httpclient` 4.5.14
- `commons-lang3` 3.12.0
- `logback-classic` 1.2.12

All dependencies are specifically chosen for Java 8 compatibility and security.

## File Structure
```
api-tester/
├── backend/
│   ├── src/main/java/
│   │   └── com/apitester/
│   │       ├── config/Java8TlsConfigListener.java
│   │       ├── service/ApiRequestService.java
│   │       └── servlet/ApiRequestServlet.java
│   ├── pom.xml
│   └── target/api-tester.war
└── frontend/
    ├── index.html (includes Quick API Tests)
    ├── script.js (includes test functions)
    └── style.css (includes test styling)
```

## Support
For issues or questions regarding Java 8/TLS compatibility, check:
1. Application logs in Tomcat
2. Browser developer console
3. Network connectivity to target APIs
