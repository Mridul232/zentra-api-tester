# Zentra API Tester

A modern, feature-rich web application for testing REST APIs with comprehensive logging and session management.

![Zentra API Logo](zentraapi-logo.svg)

## Features

- **Modern UI**: Clean, responsive interface with professional design
- **Multiple HTTP Methods**: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
- **Headers Management**: Dynamic header addition/removal
- **Request Body Support**: JSON, XML, and other formats
- **Session Logging**: Comprehensive request/response logging
- **TLS 1.2 Support**: Secure HTTPS API testing
- **Real-time Statistics**: Response time, status, and size tracking
- **Export Functionality**: Download logs and session data

## Technology Stack

- **Backend**: Java 8, Maven, Servlets
- **Frontend**: HTML5, CSS3, JavaScript (ES6)
- **Server**: Apache Tomcat 8.5.87
- **Dependencies**: Jackson, Apache HttpClient, Logback

## Quick Start

### Prerequisites

- Java 8 (JDK 1.8.0_51 or higher)
- Apache Tomcat 8.5.87
- Maven 3.x

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/zentra-api-tester.git
   cd zentra-api-tester
   ```

2. Build the project:
   ```bash
   cd backend
   mvn clean package
   ```

3. Deploy to Tomcat:
   ```bash
   copy target/api-tester.war [TOMCAT_HOME]/webapps/
   ```

4. Start Tomcat and access the application:
   ```
   http://localhost:8080/api-tester/
   ```

## Project Structure

```
zentra-api-tester/
 backend/
    src/main/java/com/apitester/
       config/         # TLS and security configuration
       filter/         # CORS and security filters
       logging/        # Session and security logging
       model/          # Request/Response models
       service/        # API request processing
       servlet/        # HTTP servlets
    src/main/resources/
    pom.xml
 frontend/
    index.html          # Main application UI
    style.css           # Modern styling with animations
    script.js           # Application logic
    zentraapi-logo.svg  # Brand logo
 deployment/
    QUICK_START.md      # Deployment instructions
    deploy.bat          # Windows deployment script
 README.md
```

## API Endpoints

- `GET /api/request` - API status and information
- `POST /api/request` - Process API requests (wrapped response)
- `POST /api/raw-request` - Process API requests (raw response)
- `GET /api/test-predefined` - Test predefined APIs
- `GET /api/test-custom` - Test custom APIs

## Configuration

### TLS/SSL Support
The application includes comprehensive TLS configuration for modern HTTPS APIs:
- TLS 1.2, 1.1, 1.0 support
- SNI extension enabled
- Strong cipher suites
- Certificate validation for testing environments

### Security Features
- CORS headers configuration
- Security headers implementation
- Session-based request tracking
- Comprehensive logging system

## Development

### Build Commands
```bash
# Clean build
mvn clean package

# Run tests
mvn test

# Generate documentation
mvn javadoc:javadoc
```

### Local Development
1. Import project into your IDE
2. Configure Tomcat 8.5.87 server
3. Deploy and run locally
4. Access at `http://localhost:8080/api-tester/`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions, issues, or feature requests, please open an issue on GitHub.

## Screenshots

### Main Interface
The application provides a clean, modern interface for API testing with:
- Logo and branding in the header
- Method selection and URL input
- Headers and request body configuration
- Real-time response display with syntax highlighting
- Session logging with statistics

### Features Showcase
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Dark/Light Theme**: Professional color scheme
- **Animated Elements**: Subtle animations for better UX
- **Real-time Feedback**: Loading states and progress indicators

---

**Powered by Zentra API** - Making API testing simple and powerful.
