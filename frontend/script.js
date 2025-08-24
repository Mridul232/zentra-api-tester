// API Tester JavaScript
class APITester {
    constructor() {
        this.initializeEventListeners();
        this.currentResponseTab = 'body';
        this.sessionId = this.getStoredSessionId();
        this.requestHistory = [];
        
        // Auto-load logs when page loads
        setTimeout(() => this.refreshLogs(), 1000);
    }

    getStoredSessionId() {
        return localStorage.getItem('apiTesterSessionId') || null;
    }

    setStoredSessionId(sessionId) {
        localStorage.setItem('apiTesterSessionId', sessionId);
        this.sessionId = sessionId;
    }

    initializeEventListeners() {
        // Send button
        document.getElementById('send-btn').addEventListener('click', () => this.sendRequest());

        // Add header button
        document.getElementById('add-header-btn').addEventListener('click', () => this.addHeader());

        // Tab switching for response (keeping response tabs)
        document.querySelectorAll('.response-tabs .tab').forEach(tab => {
            tab.addEventListener('click', (e) => this.switchResponseTab(e.target.dataset.responseTab));
        });

        // Enter key support for URL input
        document.getElementById('url').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendRequest();
            }
        });

        // Logs controls
        document.getElementById('refresh-logs-btn').addEventListener('click', () => this.refreshLogs());
        document.getElementById('clear-logs-btn').addEventListener('click', () => this.clearSession());
        document.getElementById('export-logs-btn').addEventListener('click', () => this.exportLogs());
    }

    setApiRequest(method, url) {
        document.getElementById('method').value = method;
        document.getElementById('url').value = url;
        
        // Clear any existing headers and body for predefined APIs
        this.clearHeaders();
        document.getElementById('request-body').value = '';
        
        // Auto-send for predefined APIs
        this.sendRequest();
    }

    clearHeaders() {
        const headersContainer = document.getElementById('headers-container');
        headersContainer.innerHTML = `
            <div class="header-row">
                <input type="text" placeholder="Header Key" class="header-input header-key">
                <input type="text" placeholder="Header Value" class="header-input header-value">
                <button class="remove-btn" onclick="removeHeader(this)">Remove</button>
            </div>
        `;
    }

    switchResponseTab(tabName) {
        // Update tab buttons
        document.querySelectorAll('.response-tabs .tab').forEach(tab => {
            tab.classList.remove('active');
        });
        document.querySelector(`[data-response-tab="${tabName}"]`).classList.add('active');

        // Update tab panels
        document.querySelectorAll('.response-panel').forEach(panel => {
            panel.classList.remove('active');
        });
        document.getElementById(`response-${tabName}-tab`).classList.add('active');

        this.currentResponseTab = tabName;

        // If switching to logs tab, refresh logs
        if (tabName === 'logs') {
            this.refreshLogs();
        }
    }

    addHeader() {
        const headersContainer = document.getElementById('headers-container');
        const headerRow = document.createElement('div');
        headerRow.className = 'header-row';
        headerRow.innerHTML = `
            <input type="text" placeholder="Header Key" class="header-input header-key">
            <input type="text" placeholder="Header Value" class="header-input header-value">
            <button class="remove-btn" onclick="removeHeader(this)">Remove</button>
        `;
        headersContainer.appendChild(headerRow);
    }

    removeHeader(button) {
        const headerRow = button.parentElement;
        const headersContainer = document.getElementById('headers-container');
        
        // Don't remove if it's the last header row
        if (headersContainer.children.length > 1) {
            headerRow.remove();
        } else {
            // Clear the inputs instead
            headerRow.querySelector('.header-key').value = '';
            headerRow.querySelector('.header-value').value = '';
        }
    }

    getHeaders() {
        const headers = {};
        const headerRows = document.querySelectorAll('.header-row');
        
        headerRows.forEach(row => {
            const key = row.querySelector('.header-key').value.trim();
            const value = row.querySelector('.header-value').value.trim();
            if (key && value) {
                headers[key] = value;
            }
        });
        
        return headers;
    }

    async sendRequest() {
        const url = document.getElementById('url').value.trim();
        const method = document.getElementById('method').value;
        const body = document.getElementById('request-body').value.trim();
        const headers = this.getHeaders();

        // Check for raw response toggle - only use if element exists
        const rawToggle = document.getElementById('raw-response-toggle');
        const isRawResponse = rawToggle ? rawToggle.checked : false;

        // Show a visual indicator of which mode we're using
        const responseElement = document.getElementById('response-body');
        if (isRawResponse) {
            responseElement.style.border = '3px solid #28a745';
            responseElement.style.backgroundColor = '#f8fff9';
        } else {
            responseElement.style.border = '3px solid #007bff';
            responseElement.style.backgroundColor = '#f8f9ff';
        }

        if (!url) {
            alert('Please enter a URL');
            return;
        }

        // Show loading
        this.showLoading(true);
        this.hideResponseStats();

        const startTime = Date.now();

        try {
            // Prepare request data
            const requestData = {
                url: url,
                method: method,
                headers: headers,
                body: body || null
            };

            // Choose endpoint based on toggle - be very explicit
            let endpoint;
            let responseText;
            
            // Prepare request headers with session management
            const requestHeaders = { 'Content-Type': 'application/json' };
            if (this.sessionId) {
                requestHeaders['X-Session-ID'] = this.sessionId;
            }
            
            console.log('Current session ID:', this.sessionId);
            
            if (isRawResponse) {
                endpoint = '/api-tester/api/raw-request';
                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: requestHeaders,
                    body: JSON.stringify(requestData)
                });
                
                // Handle session ID from response
                const newSessionId = response.headers.get('X-Session-ID');
                if (newSessionId && newSessionId !== this.sessionId) {
                    this.setStoredSessionId(newSessionId);
                }
                
                responseText = await response.text();
                
                // Capture original headers from custom response headers
                const originalHeaders = {};
                response.headers.forEach((value, key) => {
                    if (key.startsWith('x-original-')) {
                        // Remove the X-Original- prefix and restore the original header name
                        const originalKey = key.substring(11).replace(/-/g, ' ');
                        originalHeaders[originalKey] = value;
                    }
                });
                
                const endTime = Date.now();
                this.displayRawResponse(responseText, response.status, endTime - startTime, originalHeaders);
            } else {
                endpoint = '/api-tester/api/request';
                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: requestHeaders,
                    body: JSON.stringify(requestData)
                });
                
                // Handle session ID from response
                const newSessionId = response.headers.get('X-Session-ID');
                console.log('Received session ID from response:', newSessionId);
                if (newSessionId && newSessionId !== this.sessionId) {
                    console.log('Updating session ID from', this.sessionId, 'to', newSessionId);
                    this.setStoredSessionId(newSessionId);
                }
                
                const responseData = await response.json();
                console.log('Response data:', responseData);
                const endTime = Date.now();
                this.displayResponse(responseData, endTime - startTime);
                
                // Auto-refresh logs after successful request
                setTimeout(() => this.refreshLogs(), 500);
            }

        } catch (error) {
            console.error('Error:', error);
            const endTime = Date.now();
            this.displayErrorResponse(error.message, endTime - startTime);
        } finally {
            this.showLoading(false);
        }
    }

    displayResponse(responseData, responseTime) {
        // Update response body
        const responseElement = document.getElementById('response-body');
        responseElement.textContent = responseData.body ? JSON.stringify(responseData.body, null, 2) : 'No response body';
        
        // Add visual indicator for wrapped mode
        responseElement.style.border = '3px solid #007bff';
        responseElement.style.backgroundColor = '#f8f9ff';

        // Update response headers
        const headersText = responseData.headers ? 
            Object.entries(responseData.headers).map(([key, value]) => `${key}: ${value}`).join('\n') :
            'No response headers';
        document.getElementById('response-headers').textContent = headersText;

        // Update stats
        this.updateResponseStats(responseData.status, responseTime, responseData.size || 0);
        this.showResponseStats();
    }

    displayRawResponse(responseText, status, responseTime, responseHeaders = null) {
        // For raw responses, display the pre-formatted JSON text directly
        const responseElement = document.getElementById('response-body');
        responseElement.textContent = responseText;
        
        // Add a visual indicator that this is raw mode
        responseElement.style.border = '3px solid #28a745';
        responseElement.style.backgroundColor = '#f8fff9';

        // Display headers if available
        const headersElement = document.getElementById('response-headers');
        if (responseHeaders && Object.keys(responseHeaders).length > 0) {
            const headersText = Object.entries(responseHeaders)
                .map(([key, value]) => `${key}: ${value}`)
                .join('\n');
            headersElement.textContent = headersText;
        } else {
            headersElement.textContent = 'No response headers captured';
        }

        // Calculate size and update stats
        const size = responseText.length;
        this.updateResponseStats(status, responseTime, size);
        this.showResponseStats();
    }

    displayErrorResponse(errorMessage, responseTime) {
        // Update response body with error
        const errorResponse = {
            error: true,
            message: errorMessage,
            timestamp: new Date().toISOString()
        };
        
        document.getElementById('response-body').textContent = JSON.stringify(errorResponse, null, 2);
        document.getElementById('response-headers').textContent = 'No response headers';

        // Update stats
        this.updateResponseStats('Error', responseTime, 0);
        this.showResponseStats();
    }

    updateResponseStats(status, time, size) {
        const statusElement = document.getElementById('status-value');
        statusElement.textContent = status;
        
        // Remove all status classes
        statusElement.classList.remove('status-200', 'status-300', 'status-400', 'status-500');
        
        // Add appropriate status class
        if (typeof status === 'number') {
            if (status >= 200 && status < 300) {
                statusElement.classList.add('status-200');
            } else if (status >= 300 && status < 400) {
                statusElement.classList.add('status-300');
            } else if (status >= 400 && status < 500) {
                statusElement.classList.add('status-400');
            } else if (status >= 500) {
                statusElement.classList.add('status-500');
            }
        } else {
            statusElement.classList.add('status-500');
        }

        document.getElementById('time-value').textContent = `${time} ms`;
        document.getElementById('size-value').textContent = size > 0 ? `${(size / 1024).toFixed(2)} KB` : '0 KB';
    }

    showResponseStats() {
        document.getElementById('response-stats').style.display = 'flex';
    }

    hideResponseStats() {
        document.getElementById('response-stats').style.display = 'none';
    }

    showLoading(show) {
        const loadingElement = document.getElementById('loading');
        const sendButton = document.getElementById('send-btn');
        
        if (show) {
            loadingElement.style.display = 'flex';
            sendButton.textContent = 'Sending...';
            sendButton.disabled = true;
        } else {
            loadingElement.style.display = 'none';
            sendButton.textContent = 'Send';
            sendButton.disabled = false;
        }
    }

    // Logs Management Methods
    async refreshLogs() {
        try {
            console.log('Refreshing logs for session:', this.sessionId);
            
            if (!this.sessionId) {
                // Try to get all logs if no session ID
                console.log('No session ID, fetching all logs');
                const response = await fetch('/api-tester/api/logs/all', {
                    method: 'GET',
                    headers: {}
                });
                
                console.log('All logs response status:', response.status);
                if (response.ok) {
                    const logs = await response.json();
                    console.log('Retrieved all logs:', logs.length, 'entries');
                    console.log('All logs data:', logs);
                    this.displayLogs(logs);
                    this.updateLogsStats(logs);
                    return;
                } else {
                    console.error('Failed to fetch all logs:', response.statusText);
                    this.displayLogsPlaceholder('No session ID available. Error: ' + response.statusText);
                    return;
                }
            }

            const requestHeaders = {};
            if (this.sessionId) {
                requestHeaders['X-Session-ID'] = this.sessionId;
            }

            console.log('Fetching logs from:', `/api-tester/api/logs/session?sessionId=${this.sessionId}`);
            const response = await fetch('/api-tester/api/logs/session?sessionId=' + this.sessionId, {
                method: 'GET',
                headers: requestHeaders
            });

            console.log('Logs response status:', response.status);
            if (!response.ok) {
                // Fallback to all logs if session-specific fails
                console.log('Session logs failed, trying all logs');
                const fallbackResponse = await fetch('/api-tester/api/logs/all', {
                    method: 'GET',
                    headers: {}
                });
                
                console.log('Fallback response status:', fallbackResponse.status);
                if (fallbackResponse.ok) {
                    const logs = await fallbackResponse.json();
                    console.log('Retrieved fallback logs:', logs.length, 'entries');
                    console.log('Fallback logs data:', logs);
                    this.displayLogs(logs);
                    this.updateLogsStats(logs);
                    return;
                }
                
                throw new Error('Failed to fetch logs: ' + response.status + ' - ' + response.statusText);
            }

            const logs = await response.json();
            console.log('Retrieved session logs:', logs.length, 'entries');
            console.log('Session logs data:', logs);
            this.displayLogs(logs);
            
            // Also update stats
            this.updateLogsStats(logs);
            
        } catch (error) {
            console.error('Error refreshing logs:', error);
            this.displayLogsPlaceholder('Error loading logs: ' + error.message);
        }
    }

    clearSession() {
        // Clear session storage
        localStorage.removeItem('apiTesterSessionId');
        this.sessionId = null;
        
        // Clear UI
        this.displayLogsPlaceholder('Session cleared. Send a request to start a new session.');
        
        // Clear stats
        document.getElementById('total-requests').textContent = '0';
        document.getElementById('success-rate').textContent = '0%';
        document.getElementById('avg-response-time').textContent = '0ms';
    }

    async exportLogs() {
        try {
            if (!this.sessionId) {
                alert('No session data to export');
                return;
            }

            const requestHeaders = {};
            if (this.sessionId) {
                requestHeaders['X-Session-ID'] = this.sessionId;
            }

            const response = await fetch('/api-tester/api/logs/session?sessionId=' + this.sessionId, {
                method: 'GET',
                headers: requestHeaders
            });

            if (!response.ok) {
                throw new Error('Failed to fetch logs for export');
            }

            const logs = await response.json();
            
            // Create downloadable JSON file
            const dataStr = JSON.stringify(logs, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });
            const url = URL.createObjectURL(dataBlob);
            
            const link = document.createElement('a');
            link.href = url;
            link.download = `api-logs-${this.sessionId}-${new Date().toISOString().slice(0, 10)}.json`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
            
        } catch (error) {
            console.error('Error exporting logs:', error);
            alert('Error exporting logs: ' + error.message);
        }
    }

    displayLogs(logs) {
        const logsContent = document.getElementById('logs-content');
        
        if (!logs || logs.length === 0) {
            this.displayLogsPlaceholder('No logs found for this session');
            return;
        }

        let html = `
            <div class="session-info">
                <strong>Session ID:</strong> <span class="session-id">${this.sessionId}</span>
            </div>
            <div class="security-notice">
                <strong>Security Notice:</strong> Sensitive data has been automatically masked in these logs for security purposes.
            </div>
        `;

        logs.reverse().forEach((log, index) => {
            const statusClass = this.getStatusClass(log.responseStatus);
            const timestamp = new Date(log.timestamp).toLocaleString();
            
            html += `
                <div class="log-entry ${statusClass}">
                    <div class="log-entry-header">
                        <div>
                            <span class="log-entry-method ${log.method}">${log.method}</span>
                            <span class="log-entry-status ${statusClass}">${log.responseStatus}</span>
                        </div>
                        <div class="log-entry-response-time">${log.responseTime}ms</div>
                    </div>
                    <div class="log-entry-url">${log.url}</div>
                    <div class="log-entry-details">
                        <span class="log-entry-time">${timestamp}</span>
                        <button class="log-entry-toggle" onclick="apiTester.toggleLogDetails(${index})">
                            Show Details
                        </button>
                    </div>
                    <div id="log-details-${index}" class="log-entry-expanded" style="display: none;">
                        <strong>Request Headers:</strong><br>
                        <pre>${JSON.stringify(log.requestHeaders, null, 2)}</pre>
                        <br><strong>Request Body:</strong><br>
                        <pre>${log.requestBody || 'No body'}</pre>
                        <br><strong>Response Headers:</strong><br>
                        <pre>${JSON.stringify(log.responseHeaders, null, 2)}</pre>
                        <br><strong>Response Body (first 500 chars):</strong><br>
                        <pre>${(log.responseBody || '').substring(0, 500)}${log.responseBody && log.responseBody.length > 500 ? '...' : ''}</pre>
                    </div>
                </div>
            `;
        });

        logsContent.innerHTML = html;
    }

    toggleLogDetails(index) {
        const detailsElement = document.getElementById(`log-details-${index}`);
        const button = detailsElement.previousElementSibling.querySelector('.log-entry-toggle');
        
        if (detailsElement.style.display === 'none') {
            detailsElement.style.display = 'block';
            button.textContent = 'Hide Details';
        } else {
            detailsElement.style.display = 'none';
            button.textContent = 'Show Details';
        }
    }

    displayLogsPlaceholder(message) {
        const logsContent = document.getElementById('logs-content');
        logsContent.innerHTML = `<div class="logs-placeholder">${message}</div>`;
    }

    updateLogsStats(logs) {
        if (!logs || logs.length === 0) {
            document.getElementById('total-requests').textContent = '0';
            document.getElementById('success-rate').textContent = '0%';
            document.getElementById('avg-response-time').textContent = '0ms';
            return;
        }

        const totalRequests = logs.length;
        const successfulRequests = logs.filter(log => log.responseStatus >= 200 && log.responseStatus < 300).length;
        const successRate = Math.round((successfulRequests / totalRequests) * 100);
        const avgResponseTime = Math.round(logs.reduce((sum, log) => sum + log.responseTime, 0) / totalRequests);

        document.getElementById('total-requests').textContent = totalRequests;
        document.getElementById('success-rate').textContent = successRate + '%';
        document.getElementById('avg-response-time').textContent = avgResponseTime + 'ms';
    }

    getStatusClass(status) {
        if (status >= 200 && status < 300) return 'success';
        if (status >= 400 && status < 500) return 'warning';
        if (status >= 500) return 'error';
        return 'error';
    }
}

// Global function for removing headers (called from HTML)
function removeHeader(button) {
    apiTester.removeHeader(button);
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.apiTester = new APITester();
    
    // Add some sample headers for demonstration
    setTimeout(() => {
        const keyInput = document.querySelector('.header-key');
        const valueInput = document.querySelector('.header-value');
        if (keyInput && valueInput) {
            keyInput.placeholder = 'e.g., Content-Type';
            valueInput.placeholder = 'e.g., application/json';
        }
    }, 100);
});
