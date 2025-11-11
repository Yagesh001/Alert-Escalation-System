// API Base URL
const API_BASE = '/api/v1';

// Store JWT token
let authToken = null;

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    // Check if already logged in
    const token = localStorage.getItem('authToken');
    if (token) {
        authToken = token;
        showMainApp();
        loadDashboard();
    }
});

// Login
async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            authToken = data.data.token;
            localStorage.setItem('authToken', authToken);
            showMainApp();
            loadDashboard();
            showMessage('loginMessage', 'Login successful!', 'success');
        } else {
            showMessage('loginMessage', 'Login failed: ' + data.message, 'error');
        }
    } catch (error) {
        showMessage('loginMessage', 'Error: ' + error.message, 'error');
    }
}

// Logout
function logout() {
    authToken = null;
    localStorage.removeItem('authToken');
    document.getElementById('loginSection').classList.remove('hidden');
    document.getElementById('mainApp').classList.add('hidden');
}

// Show main app
function showMainApp() {
    document.getElementById('loginSection').classList.add('hidden');
    document.getElementById('mainApp').classList.remove('hidden');
}

// Show message
function showMessage(elementId, message, type) {
    const el = document.getElementById(elementId);
    el.textContent = message;
    el.className = `message ${type}`;
    setTimeout(() => {
        el.className = 'message';
    }, 5000);
}

// Show tab
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');
    
    // Load data for specific tabs
    if (tabName === 'dashboard') {
        loadDashboard();
    } else if (tabName === 'alerts') {
        loadActiveAlerts();
    }
}

// Load Dashboard
async function loadDashboard() {
    try {
        const response = await fetch(`${API_BASE}/dashboard/overview`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success) {
            displayDashboard(data.data);
        }
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

// Display Dashboard
function displayDashboard(dashboard) {
    // Stats with color coding - make them clickable
    const statsHtml = `
        <div class="stat-card success" onclick="filterAlertsByStatus('OPEN')" style="cursor: pointer;" title="Click to view Open alerts">
            <h3>${dashboard.totalOpenAlerts || 0}</h3>
            <p>Open Alerts</p>
        </div>
        <div class="stat-card danger" onclick="filterAlertsByStatus('ESCALATED')" style="cursor: pointer;" title="Click to view Escalated alerts">
            <h3>${dashboard.totalEscalatedAlerts || 0}</h3>
            <p>Escalated</p>
        </div>
        <div class="stat-card info" onclick="filterAlertsByStatus('AUTO_CLOSED')" style="cursor: pointer;" title="Click to view Auto-Closed alerts">
            <h3>${dashboard.totalAutoClosedAlerts || 0}</h3>
            <p>Auto-Closed</p>
        </div>
        <div class="stat-card primary" onclick="filterAlertsByStatus('RESOLVED')" style="cursor: pointer;" title="Click to view Resolved alerts">
            <h3>${dashboard.totalResolvedAlerts || 0}</h3>
            <p>Resolved</p>
        </div>
    `;
    document.getElementById('dashboardStats').innerHTML = statsHtml;
    
    // Top Drivers
    if (dashboard.topDrivers && dashboard.topDrivers.length > 0) {
        let driversHtml = '<h3 class="section-title">Top 5 Drivers with Most Alerts</h3>';
        dashboard.topDrivers.forEach((driver, index) => {
            driversHtml += `
                <div class="driver-item">
                    <div style="display: flex; align-items: center;">
                        <div class="driver-rank">${index + 1}</div>
                        <div>
                            <strong style="color: #2c3e50; font-size: 1.05em;">Driver ${driver.driverId}</strong><br>
                            <span style="color: #7f8c8d; font-size: 0.9em;">${driver.totalAlerts} total alerts</span>
                        </div>
                    </div>
                    <span class="badge warning">${driver.totalAlerts}</span>
                </div>
            `;
        });
        document.getElementById('topDrivers').innerHTML = driversHtml;
    }
    
    // Recent Auto-Closed Alerts
    if (dashboard.recentlyAutoClosedAlerts && dashboard.recentlyAutoClosedAlerts.length > 0) {
        let alertsHtml = '<h3 class="section-title">Recently Auto-Closed Alerts</h3>';
        dashboard.recentlyAutoClosedAlerts.slice(0, 5).forEach(alert => {
            alertsHtml += createAlertCard(alert);
        });
        document.getElementById('recentAlerts').innerHTML = alertsHtml;
    }
}

// Load Active Alerts
async function loadActiveAlerts() {
    document.getElementById('alertsList').innerHTML = '<div class="loading">Loading alerts...</div>';
    
    try {
        const response = await fetch(`${API_BASE}/alerts/active`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        const data = await response.json();
        
        if (data.success && data.data.length > 0) {
            let html = '';
            data.data.forEach(alert => {
                html += createAlertCard(alert, true);
            });
            document.getElementById('alertsList').innerHTML = html;
        } else {
            document.getElementById('alertsList').innerHTML = `
                <div style="text-align: center; padding: 60px; background: #f8f9fa; border-radius: 8px; border: 1px solid #e1e8ed;">
                    <div style="font-size: 3em; margin-bottom: 16px; opacity: 0.3;">📭</div>
                    <p style="color: #7f8c8d; font-size: 1.1em;">No active alerts found</p>
                    <p style="color: #95a5a6; font-size: 0.9em; margin-top: 8px;">All systems operating normally</p>
                </div>
            `;
        }
    } catch (error) {
        document.getElementById('alertsList').innerHTML = `
            <div style="background: #fee; padding: 20px; border-radius: 6px; border-left: 4px solid #e74c3c;">
                <p style="color: #c0392b;">Error loading alerts. Please try again.</p>
            </div>
        `;
    }
}

// Create Alert Card
function createAlertCard(alert, showActions = false) {
    const severityClass = alert.severity.toLowerCase();
    const statusBadge = `<span class="badge ${alert.status.toLowerCase().replace('_', '-')}">${alert.status.replace('_', ' ')}</span>`;
    const severityBadge = `<span class="badge ${severityClass}">${alert.severity}</span>`;
    
    const metadata = alert.metadata ? JSON.stringify(alert.metadata, null, 2) : 'N/A';
    
    let actionsHtml = '';
    if (showActions && alert.status !== 'RESOLVED' && alert.status !== 'AUTO_CLOSED') {
        const isEscalated = alert.status === 'ESCALATED';
        const currentSeverity = alert.severity;
        
        actionsHtml = `
            <div style="margin-top: 20px; padding-top: 20px; border-top: 1px solid #e1e8ed;">
                <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center;">
                    <div style="flex: 1; min-width: 200px;">
                        <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #2c3e50; font-size: 0.9em;">Quick Actions:</label>
                        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                            ${!isEscalated ? `
                                <button class="btn" style="background: #e74c3c; color: white; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9em;" 
                                        onclick="escalateAlert('${alert.alertId}', 'CRITICAL')" 
                                        title="Escalate to Critical">
                                    ⚠️ Escalate to Critical
                                </button>
                                <button class="btn" style="background: #f39c12; color: white; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9em;" 
                                        onclick="escalateAlert('${alert.alertId}', 'WARNING')" 
                                        title="Escalate to Warning">
                                    ⚡ Escalate to Warning
                                </button>
                            ` : `
                                <span style="color: #7f8c8d; font-size: 0.85em; padding: 8px;">Already Escalated</span>
                            `}
                            <button class="btn" style="background: #27ae60; color: white; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9em;" 
                                    onclick="resolveAlert('${alert.alertId}')" 
                                    title="Mark as Resolved">
                                ✓ Mark as Resolved
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    return `
        <div class="alert-card ${severityClass}">
            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 16px;">
                <div>
                    <h3 style="color: #2c3e50; margin-bottom: 10px; font-size: 1.15em; font-weight: 600;">${alert.alertType.replace(/_/g, ' ')}</h3>
                    ${statusBadge} ${severityBadge}
                </div>
                <div style="text-align: right;">
                    <small style="color: #95a5a6; font-size: 0.85em; display: block;">ID: ${alert.alertId.substring(0, 8)}...</small>
                    <small style="color: #7f8c8d; font-size: 0.85em;">${new Date(alert.timestamp).toLocaleString()}</small>
                </div>
            </div>
            <div style="color: #5a6c7d; font-size: 0.95em; line-height: 1.8;">
                <p><strong style="color: #2c3e50;">Driver:</strong> ${alert.driverId || 'N/A'}</p>
                <p><strong style="color: #2c3e50;">Vehicle:</strong> ${alert.vehicleId || 'N/A'}</p>
                ${alert.escalatedAt ? `<p><strong style="color: #2c3e50;">Escalated At:</strong> ${new Date(alert.escalatedAt).toLocaleString()}</p>` : ''}
                ${alert.escalationReason ? `<p><strong style="color: #2c3e50;">Escalation Reason:</strong> ${alert.escalationReason}</p>` : ''}
                ${alert.closedAt ? `<p><strong style="color: #2c3e50;">Closed At:</strong> ${new Date(alert.closedAt).toLocaleString()}</p>` : ''}
                ${alert.closureReason ? `<p><strong style="color: #2c3e50;">Closure Reason:</strong> ${alert.closureReason}</p>` : ''}
                ${metadata !== 'N/A' ? `<details style="margin-top: 12px;"><summary style="cursor: pointer; color: #3498db; font-weight: 500;">View Additional Details</summary><pre style="background: #f8f9fa; padding: 16px; border-radius: 6px; margin-top: 12px; overflow-x: auto; border: 1px solid #e1e8ed; font-size: 0.85em;">${metadata}</pre></details>` : ''}
            </div>
            ${actionsHtml}
        </div>
    `;
}

// Create Alert
async function createAlert() {
    const alertType = document.getElementById('alertType').value;
    const severity = document.getElementById('severity').value;
    const driverId = document.getElementById('driverId').value;
    const vehicleId = document.getElementById('vehicleId').value;
    const routeId = document.getElementById('routeId').value;
    const metadataStr = document.getElementById('metadata').value;
    
    let metadata = {};
    if (metadataStr) {
        try {
            metadata = JSON.parse(metadataStr);
        } catch (e) {
            showMessage('createMessage', 'Invalid JSON in metadata field', 'error');
            return;
        }
    }
    
    try {
        const response = await fetch(`${API_BASE}/alerts`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                alertType,
                severity,
                driverId,
                vehicleId,
                routeId,
                metadata
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('createMessage', 'Alert created successfully! ID: ' + data.data.alertId.substring(0, 8), 'success');
            // Clear form
            document.getElementById('driverId').value = '';
            document.getElementById('vehicleId').value = '';
            document.getElementById('routeId').value = '';
            document.getElementById('metadata').value = '';
        } else {
            showMessage('createMessage', 'Error: ' + data.message, 'error');
        }
    } catch (error) {
        showMessage('createMessage', 'Error: ' + error.message, 'error');
    }
}

// Get default resolution message based on alert type
function getDefaultResolutionMessage(alertType) {
    const messages = {
        'OVERSPEEDING': 'Speed limit compliance verified. Driver counseled on safe driving practices.',
        'HARSH_BRAKING': 'Braking incident reviewed. No immediate action required. Monitoring continued.',
        'HARSH_ACCELERATION': 'Acceleration pattern analyzed. Normal operation confirmed.',
        'ROUTE_DEVIATION': 'Route deviation explained and approved. No further action needed.',
        'COMPLIANCE_DOCUMENT_EXPIRY': 'Document renewal completed. Compliance status updated.',
        'COMPLIANCE_LICENSE_INVALID': 'License issue resolved. Valid license verified and updated.',
        'COMPLIANCE_INSURANCE_EXPIRY': 'Insurance renewed. Coverage verified and updated in system.',
        'FEEDBACK_NEGATIVE': 'Customer concern addressed. Follow-up completed.',
        'FEEDBACK_COMPLAINT': 'Complaint investigated and resolved. Customer notified.',
        'MAINTENANCE_OVERDUE': 'Maintenance completed. Vehicle service records updated.',
        'FUEL_THEFT': 'Fuel consumption verified. No theft detected. Normal operation confirmed.'
    };
    return messages[alertType] || 'Alert resolved. Issue addressed and verified.';
}

// Escalate Alert
async function escalateAlert(alertId, severity) {
    const defaultReasons = {
        'CRITICAL': 'Manual escalation to Critical severity - requires immediate attention',
        'WARNING': 'Manual escalation to Warning severity - needs monitoring'
    };
    
    const defaultReason = defaultReasons[severity] || 'Manual escalation requested';
    const reason = prompt(`Enter escalation reason:\n\nDefault: ${defaultReason}`, defaultReason);
    if (!reason) return;
    
    try {
        const response = await fetch(`${API_BASE}/alerts/${alertId}/escalate`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                severity: severity,
                reason: reason
            })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert(`Alert escalated to ${severity} successfully!`);
            loadActiveAlerts();
            loadDashboard();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Resolve Alert
async function resolveAlert(alertId) {
    // First, get the alert to know its type
    let alertType = null;
    try {
        const alertResponse = await fetch(`${API_BASE}/alerts/${alertId}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        const alertData = await alertResponse.json();
        if (alertData.success) {
            alertType = alertData.data.alertType;
        }
    } catch (error) {
        console.error('Error fetching alert:', error);
    }
    
    // Get default message based on alert type
    const defaultMessage = alertType ? getDefaultResolutionMessage(alertType) : 'Alert resolved. Issue addressed and verified.';
    
    const reason = prompt(`Enter resolution reason:\n\nDefault: ${defaultMessage}`, defaultMessage);
    if (!reason) return;
    
    try {
        const response = await fetch(`${API_BASE}/alerts/${alertId}/resolve`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ reason })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert('Alert resolved successfully!');
            loadActiveAlerts();
            loadDashboard();
        } else {
            alert('Error: ' + data.message);
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// Filter alerts by status
async function filterAlertsByStatus(status) {
    // Switch to alerts tab
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.getElementById('alerts-tab').classList.add('active');
    event?.target?.closest('.tab-button')?.classList.add('active') || 
    document.querySelector('.tab-button[onclick*="alerts"]')?.classList.add('active');
    
    // Load all alerts and filter by status
    document.getElementById('alertsList').innerHTML = '<div class="loading">Loading alerts...</div>';
    
    try {
        // Get all alerts (we'll need to add an endpoint or use existing one)
        const response = await fetch(`${API_BASE}/alerts/all?status=${status}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        let alerts = [];
        if (response.ok) {
            const data = await response.json();
            if (data.success) {
                alerts = data.data || [];
            }
        } else {
            // Fallback: get active alerts and filter client-side
            const activeResponse = await fetch(`${API_BASE}/alerts/active`, {
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });
            const activeData = await activeResponse.json();
            if (activeData.success) {
                alerts = activeData.data.filter(alert => alert.status === status) || [];
            }
        }
        
        if (alerts.length > 0) {
            let html = `<div style="margin-bottom: 16px; padding: 12px; background: #e8f4f8; border-radius: 6px; border-left: 4px solid #3498db;">
                <strong>Showing ${alerts.length} alert(s) with status: ${status.replace('_', ' ')}</strong>
                <button onclick="loadActiveAlerts()" style="float: right; padding: 4px 12px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer;">Show All Active</button>
            </div>`;
            alerts.forEach(alert => {
                html += createAlertCard(alert, alert.status === 'OPEN' || alert.status === 'ESCALATED');
            });
            document.getElementById('alertsList').innerHTML = html;
        } else {
            document.getElementById('alertsList').innerHTML = `
                <div style="text-align: center; padding: 60px; background: #f8f9fa; border-radius: 8px; border: 1px solid #e1e8ed;">
                    <div style="font-size: 3em; margin-bottom: 16px; opacity: 0.3;">📭</div>
                    <p style="color: #7f8c8d; font-size: 1.1em;">No alerts found with status: ${status.replace('_', ' ')}</p>
                    <button onclick="loadActiveAlerts()" style="margin-top: 16px; padding: 8px 16px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer;">Show All Active Alerts</button>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error filtering alerts:', error);
        document.getElementById('alertsList').innerHTML = `
            <div style="background: #fee; padding: 20px; border-radius: 6px; border-left: 4px solid #e74c3c;">
                <p style="color: #c0392b;">Error loading alerts. Please try again.</p>
                <button onclick="loadActiveAlerts()" style="margin-top: 12px; padding: 8px 16px; background: #e74c3c; color: white; border: none; border-radius: 4px; cursor: pointer;">Show All Active Alerts</button>
            </div>
        `;
    }
}

// Demo Scenarios
async function runOverspeedingDemo() {
    const resultDiv = document.getElementById('demo1Result');
    resultDiv.innerHTML = '<p style="color: #667eea;">Running demo...</p>';
    
    try {
        const driverId = 'DEMO_DRIVER_' + Date.now();
        let createdCount = 0;
        
        // Create 3 overspeeding alerts
        for (let i = 1; i <= 3; i++) {
            const response = await fetch(`${API_BASE}/alerts`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    alertType: 'OVERSPEEDING',
                    severity: 'WARNING',
                    driverId: driverId,
                    metadata: { speed: 80 + i * 5, limit: 60, location: `Point ${i}` }
                })
            });
            
            if (response.ok) {
                createdCount++;
            } else {
                const errorData = await response.json().catch(() => ({ message: 'Failed to create alert' }));
                throw new Error(`Failed to create alert ${i}: ${errorData.message || response.statusText}`);
            }
            
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        resultDiv.innerHTML = `
            <div style="background: #eafaf1; padding: 16px; border-radius: 6px; border-left: 4px solid #27ae60;">
                <p style="color: #1e8449; margin-bottom: 8px; font-weight: 500;">✓ Demo completed successfully!</p>
                <p style="color: #7f8c8d; font-size: 0.9em;">Created ${createdCount} overspeeding alerts for driver ${driverId}. Navigate to the <strong>Active Alerts</strong> tab to see the escalation.</p>
            </div>
        `;
        
        // Refresh dashboard to show updated data
        setTimeout(() => {
            loadDashboard();
        }, 1000);
        
    } catch (error) {
        console.error('Overspeeding demo error:', error);
        resultDiv.innerHTML = `
            <div style="background: #fee; padding: 16px; border-radius: 6px; border-left: 4px solid #e74c3c;">
                <p style="color: #c0392b; font-weight: 500;">Error: ${error.message}</p>
                <p style="color: #7f8c8d; font-size: 0.85em; margin-top: 8px;">Please check the browser console for more details.</p>
            </div>
        `;
    }
}

async function runComplianceDemo() {
    const resultDiv = document.getElementById('demo2Result');
    resultDiv.innerHTML = '<p style="color: #667eea;">Running demo...</p>';
    
    try {
        const driverId = 'DEMO_DRIVER_' + Date.now();
        
        // Create compliance alert
        const response = await fetch(`${API_BASE}/alerts`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                alertType: 'COMPLIANCE_DOCUMENT_EXPIRY',
                severity: 'WARNING',
                driverId: driverId,
                metadata: { documentType: 'license', expiryDate: '2024-12-31' }
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: 'Failed to create alert' }));
            throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        
        // Validate response structure
        if (!data || !data.success || !data.data || !data.data.alertId) {
            throw new Error('Invalid response from server. Expected alertId but got: ' + JSON.stringify(data));
        }
        
        const alertId = data.data.alertId;
        
        // Wait 2 seconds
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Update condition to trigger auto-close
        const conditionResponse = await fetch(`${API_BASE}/alerts/${alertId}/condition?condition=DOCUMENT_RENEWED`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });
        
        if (!conditionResponse.ok) {
            const errorData = await conditionResponse.json().catch(() => ({ message: 'Failed to update condition' }));
            throw new Error('Failed to update condition: ' + (errorData.message || conditionResponse.statusText));
        }
        
        resultDiv.innerHTML = `
            <div style="background: #eafaf1; padding: 16px; border-radius: 6px; border-left: 4px solid #27ae60;">
                <p style="color: #1e8449; margin-bottom: 8px; font-weight: 500;">✓ Demo completed successfully!</p>
                <p style="color: #7f8c8d; font-size: 0.9em;">Alert created (ID: ${alertId.substring(0, 8)}...) and document renewed condition set. The alert should be auto-closed. Check the <strong>Dashboard</strong> tab or <strong>Auto-Closed</strong> alerts.</p>
            </div>
        `;
        
        // Refresh dashboard to show updated data
        setTimeout(() => {
            loadDashboard();
        }, 1000);
        
    } catch (error) {
        console.error('Compliance demo error:', error);
        resultDiv.innerHTML = `
            <div style="background: #fee; padding: 16px; border-radius: 6px; border-left: 4px solid #e74c3c;">
                <p style="color: #c0392b; font-weight: 500;">Error: ${error.message}</p>
                <p style="color: #7f8c8d; font-size: 0.85em; margin-top: 8px;">Please check the browser console for more details.</p>
            </div>
        `;
    }
}

async function runFeedbackDemo() {
    const resultDiv = document.getElementById('demo3Result');
    resultDiv.innerHTML = '<p style="color: #667eea;">Running demo...</p>';
    
    try {
        const driverId = 'DEMO_DRIVER_' + Date.now();
        let createdCount = 0;
        
        // Create 2 negative feedback alerts
        for (let i = 1; i <= 2; i++) {
            const response = await fetch(`${API_BASE}/alerts`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    alertType: 'FEEDBACK_NEGATIVE',
                    severity: 'INFO',
                    driverId: driverId,
                    metadata: { rating: i, comment: `Complaint ${i}`, passenger: `P00${i}` }
                })
            });
            
            if (response.ok) {
                createdCount++;
            } else {
                const errorData = await response.json().catch(() => ({ message: 'Failed to create alert' }));
                throw new Error(`Failed to create alert ${i}: ${errorData.message || response.statusText}`);
            }
            
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        resultDiv.innerHTML = `
            <div style="background: #eafaf1; padding: 16px; border-radius: 6px; border-left: 4px solid #27ae60;">
                <p style="color: #1e8449; margin-bottom: 8px; font-weight: 500;">✓ Demo completed successfully!</p>
                <p style="color: #7f8c8d; font-size: 0.9em;">Created ${createdCount} negative feedback alerts for driver ${driverId}. Navigate to <strong>Active Alerts</strong> to see the escalation.</p>
            </div>
        `;
        
        // Refresh dashboard to show updated data
        setTimeout(() => {
            loadDashboard();
        }, 1000);
        
    } catch (error) {
        console.error('Feedback demo error:', error);
        resultDiv.innerHTML = `
            <div style="background: #fee; padding: 16px; border-radius: 6px; border-left: 4px solid #e74c3c;">
                <p style="color: #c0392b; font-weight: 500;">Error: ${error.message}</p>
                <p style="color: #7f8c8d; font-size: 0.85em; margin-top: 8px;">Please check the browser console for more details.</p>
            </div>
        `;
    }
}

