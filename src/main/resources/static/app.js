const API_BASE = '/api';

// Utility functions
function showMessage(message, type = 'info') {
    const container = document.getElementById('messageContainer');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;
    container.appendChild(messageDiv);
    
    setTimeout(() => {
        messageDiv.style.animation = 'slideIn 0.3s ease reverse';
        setTimeout(() => messageDiv.remove(), 300);
    }, 3000);
}

function showResult(elementId, message, isSuccess = true) {
    const element = document.getElementById(elementId);
    element.textContent = message;
    element.className = `result-message ${isSuccess ? 'success' : 'error'}`;
}

async function apiCall(endpoint, method = 'GET', body = null) {
    try {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json',
            }
        };
        
        if (body) {
            options.body = JSON.stringify(body);
        }
        
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        
        if (!response.ok) {
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                // Response is not JSON, use status text
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        // Handle empty responses
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();
            return data;
        } else {
            // Return empty array or object for non-JSON responses
            return [];
        }
    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    }
}

// Machine Definitions
async function loadDefinitionFromFile() {
    const fileName = document.getElementById('loadDefinitionFile').value;
    if (!fileName) {
        showMessage('Please enter a file name', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/machine-definitions/load-from-file?fileName=${encodeURIComponent(fileName)}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            const data = await response.json();
            showMessage(`Machine definition "${data.id}" loaded successfully!`, 'success');
            await refreshDefinitions();
        } else {
            showMessage('Failed to load machine definition', 'error');
        }
    } catch (error) {
        showMessage(`Error: ${error.message}`, 'error');
    }
}

async function refreshDefinitions() {
    try {
        const definitions = await apiCall('/machine-definitions');
        const select = document.getElementById('machineDefinitions');
        select.innerHTML = '<option value="">Select a definition...</option>';
        
        definitions.forEach(def => {
            const option = document.createElement('option');
            option.value = def.id;
            option.textContent = `${def.id} - ${def.name}`;
            select.appendChild(option);
        });
        
        if (definitions.length > 0) {
            showMessage(`Loaded ${definitions.length} machine definition(s)`, 'success');
        }
    } catch (error) {
        showMessage(`Error loading definitions: ${error.message}`, 'error');
    }
}

async function loadDefinitionDetails() {
    const definitionId = document.getElementById('machineDefinitions').value;
    if (!definitionId) {
        document.getElementById('definitionDetails').innerHTML = '';
        return;
    }
    
    try {
        const definition = await apiCall(`/machine-definitions/${definitionId}`);
        const detailsDiv = document.getElementById('definitionDetails');
        
        let html = `<h4>${definition.name}</h4>`;
        html += `<p><strong>ID:</strong> ${definition.id}</p>`;
        html += `<p><strong>States:</strong> ${definition.states?.length || 0}</p>`;
        html += `<p><strong>Transitions:</strong> ${definition.transitions?.length || 0}</p>`;
        
        if (definition.states && definition.states.length > 0) {
            html += '<h5>States:</h5><ul>';
            definition.states.forEach(state => {
                html += `<li>${state.stateId} (${state.name}) - ${state.type}</li>`;
            });
            html += '</ul>';
        }
        
        if (definition.transitions && definition.transitions.length > 0) {
            html += '<h5>Transitions:</h5>';
            
            // Group transitions by role
            const transitionsByRole = {};
            definition.transitions.forEach(trans => {
                const role = trans.role || 'No Role';
                if (!transitionsByRole[role]) {
                    transitionsByRole[role] = [];
                }
                transitionsByRole[role].push(trans);
            });
            
            // Display grouped by role
            Object.keys(transitionsByRole).sort().forEach(role => {
                const roleTransitions = transitionsByRole[role];
                html += `<div style="margin-bottom: 15px; padding: 10px; background: #f8f9fa; border-radius: 6px; border-left: 4px solid #667eea;">`;
                html += `<strong style="color: #667eea;">${role === 'No Role' ? 'No Role Required' : role}</strong> (${roleTransitions.length} transition${roleTransitions.length !== 1 ? 's' : ''})`;
                html += '<ul style="margin-top: 8px; margin-bottom: 0;">';
                roleTransitions.forEach(trans => {
                    html += `<li>${trans.transitionId}: ${trans.sourceStateId} → ${trans.destinationStateId}`;
                    if (trans.conditionExpression) {
                        html += ` [condition: ${trans.conditionExpression}]`;
                    }
                    html += '</li>';
                });
                html += '</ul></div>';
            });
        }
        
        detailsDiv.innerHTML = html;
    } catch (error) {
        showMessage(`Error loading definition details: ${error.message}`, 'error');
    }
}

// Machine Instances
async function createMachineInstance() {
    const definitionId = document.getElementById('machineDefinitionId').value;
    const contextText = document.getElementById('initialContext').value;
    
    if (!definitionId) {
        showMessage('Please enter a machine definition ID', 'error');
        return;
    }
    
    let initialContext = {};
    if (contextText.trim()) {
        try {
            initialContext = JSON.parse(contextText);
        } catch (e) {
            showMessage('Invalid JSON in initial context', 'error');
            return;
        }
    }
    
    try {
        const machine = await apiCall('/machines', 'POST', {
            machineDefinitionId: definitionId,
            initialContext: initialContext
        });
        
        showResult('createResult', `Machine created successfully! ID: ${machine.id}`, true);
        showMessage(`Machine instance ${machine.id} created!`, 'success');
        document.getElementById('machineId').value = machine.id;
        await loadMachine();
        await refreshMachines();
    } catch (error) {
        showResult('createResult', `Error: ${error.message}`, false);
        showMessage(`Error creating machine: ${error.message}`, 'error');
    }
}

async function loadMachine() {
    const machineId = document.getElementById('machineId').value;
    if (!machineId) {
        showMessage('Please enter a machine ID', 'error');
        return;
    }
    
    try {
        const machine = await apiCall(`/machines/${machineId}`);
        displayMachineDetails(machine);
        await refreshAvailableTransitions();
        await loadHistory();
        document.getElementById('operationsSection').style.display = 'block';
    } catch (error) {
        showMessage(`Error loading machine: ${error.message}`, 'error');
        document.getElementById('machineDetails').innerHTML = `<p class="error">Machine not found</p>`;
    }
}

function displayMachineDetails(machine) {
    const detailsDiv = document.getElementById('machineDetails');
    document.getElementById('currentState').textContent = machine.currentStateId;
    
    // Display context
    const contextDiv = document.getElementById('contextDisplay');
    if (machine.context && Object.keys(machine.context).length > 0) {
        contextDiv.innerHTML = `<div class="json-display">${JSON.stringify(machine.context, null, 2)}</div>`;
    } else {
        contextDiv.innerHTML = '<p style="color: #999;">No context set</p>';
    }
    
    let html = `<h4>Machine #${machine.id}</h4>`;
    html += `<p><strong>Definition:</strong> ${machine.machineDefinitionId}</p>`;
    html += `<p><strong>Current State:</strong> ${machine.currentStateId}</p>`;
    html += `<p><strong>Created:</strong> ${new Date(machine.createdAt).toLocaleString()}</p>`;
    html += `<p><strong>Updated:</strong> ${new Date(machine.updatedAt).toLocaleString()}</p>`;
    
    detailsDiv.innerHTML = html;
}

async function refreshMachines() {
    try {
        // Load all machines instead of filtering by definition
        const machines = await apiCall('/machines');
        const select = document.getElementById('machineInstances');
        select.innerHTML = '<option value="">Select a machine...</option>';
        
        if (machines.length === 0) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = 'No machines found';
            option.disabled = true;
            select.appendChild(option);
            showMessage('No machines found. Create a machine instance first.', 'info');
            return;
        }
        
        machines.forEach(machine => {
            const option = document.createElement('option');
            option.value = machine.id;
            option.textContent = `Machine #${machine.id} - ${machine.machineDefinitionId} - State: ${machine.currentStateId}`;
            select.appendChild(option);
        });
        
        showMessage(`Loaded ${machines.length} machine(s)`, 'success');
    } catch (error) {
        console.error('Error refreshing machines:', error);
        showMessage(`Error loading machines: ${error.message}`, 'error');
        const select = document.getElementById('machineInstances');
        select.innerHTML = '<option value="">Error loading machines</option>';
    }
}

function selectMachineFromList() {
    const machineId = document.getElementById('machineInstances').value;
    if (machineId) {
        document.getElementById('machineId').value = machineId;
        loadMachine();
    }
}

// Machine Operations
async function refreshAvailableTransitions() {
    const machineId = document.getElementById('machineId').value;
    if (!machineId) return;
    
    try {
        const transitionsDiv = document.getElementById('availableTransitions');
        const roleFilter = document.getElementById('roleFilter');
        
        // Save the currently selected role before resetting the dropdown
        const previouslySelectedRole = roleFilter.value;
        
        // Get all transitions (for role dropdown)
        const allTransitions = await apiCall(`/machines/${machineId}/transitions/all`).catch(() => []);
        
        // Group all transitions by role (for dropdown)
        const allRoles = new Set();
        allTransitions.forEach(trans => {
            const role = trans.role || 'No Role';
            allRoles.add(role);
        });
        
        // Update role filter dropdown with all roles
        roleFilter.innerHTML = '<option value="">All Roles</option>';
        Array.from(allRoles).sort().forEach(role => {
            const option = document.createElement('option');
            option.value = role;
            option.textContent = role === 'No Role' ? 'No Role Required' : role;
            roleFilter.appendChild(option);
        });
        
        // Restore the previously selected role if it still exists
        const selectedRole = (previouslySelectedRole && Array.from(allRoles).includes(previouslySelectedRole)) 
            ? previouslySelectedRole 
            : '';
        if (selectedRole) {
            roleFilter.value = selectedRole;
        }
        
        // Get available transitions filtered by selected role
        const roleParam = selectedRole && selectedRole !== '' ? `?role=${encodeURIComponent(selectedRole)}` : '';
        const availableTransitions = await apiCall(`/machines/${machineId}/transitions/available${roleParam}`).catch(() => []);
        
        if (allTransitions.length === 0) {
            transitionsDiv.innerHTML = '<p style="color: #999;">No transitions from current state</p>';
            return;
        }
        
        // Create a Set of available transition IDs for quick lookup
        const availableTransitionIds = new Set(availableTransitions.map(t => t.id));
        
        // Group available transitions by role (for display)
        const transitionsByRole = {};
        availableTransitions.forEach(trans => {
            const role = trans.role || 'No Role';
            if (!transitionsByRole[role]) {
                transitionsByRole[role] = [];
            }
            transitionsByRole[role].push(trans);
        });
        
        // Get selected role filter (use the restored value)
        const currentSelectedRole = selectedRole;
        
        // Display transitions grouped by role
        let html = '';
        const rolesToShow = currentSelectedRole ? [currentSelectedRole] : Array.from(allRoles).sort();
        
        rolesToShow.forEach(role => {
            const roleTransitions = transitionsByRole[role] || [];
            const allRoleTransitions = allTransitions.filter(t => (t.role || 'No Role') === role);
            
            // Skip if no transitions for this role
            if (allRoleTransitions.length === 0) return;
            
            // Role header
            html += `<div class="transition-group">`;
            html += `<div class="transition-group-header">`;
            html += `<h4>${role === 'No Role' ? 'No Role Required' : role} (${roleTransitions.length}/${allRoleTransitions.length} available)</h4>`;
            html += `</div>`;
            html += `<div class="transition-group-items">`;
            
            if (roleTransitions.length === 0) {
                html += `<p style="color: #999; padding: 10px;">No available transitions for this role. Update context with appropriate role to enable transitions.</p>`;
            } else {
                // Show available transitions for this role
                roleTransitions.forEach(trans => {
                    html += `<div class="transition-item" onclick="selectTransition('${trans.id}')">`;
                    html += `<span class="transition-id">${trans.id}</span>`;
                    html += `<span class="transition-name">${trans.name || trans.id}</span>`;
                    html += `<div class="transition-path">${trans.sourceStateId} → ${trans.destinationStateId}</div>`;
                    if (trans.condition) {
                        html += `<div class="transition-condition">Condition: ${trans.condition.expression}</div>`;
                    }
                    html += '</div>';
                });
            }
            
            html += `</div></div>`;
        });
        
        transitionsDiv.innerHTML = html;
    } catch (error) {
        showMessage(`Error loading transitions: ${error.message}`, 'error');
    }
}

function selectTransition(transitionId) {
    document.getElementById('transitionId').value = transitionId;
}

async function executeTransition() {
    const machineId = document.getElementById('machineId').value;
    const transitionId = document.getElementById('transitionId').value;
    
    if (!machineId) {
        showMessage('Please load a machine first', 'error');
        return;
    }
    
    if (!transitionId) {
        showMessage('Please enter a transition ID', 'error');
        return;
    }
    
    try {
        const machine = await apiCall(`/machines/${machineId}/transitions/execute`, 'POST', {
            transitionId: transitionId
        });
        
        showMessage(`Transition ${transitionId} executed successfully!`, 'success');
        displayMachineDetails(machine);
        await refreshAvailableTransitions();
        await loadHistory();
        document.getElementById('transitionId').value = '';
    } catch (error) {
        showMessage(`Error executing transition: ${error.message}`, 'error');
    }
}

async function updateContext() {
    const machineId = document.getElementById('machineId').value;
    const contextText = document.getElementById('contextUpdate').value;
    
    if (!machineId) {
        showMessage('Please load a machine first', 'error');
        return;
    }
    
    if (!contextText.trim()) {
        showMessage('Please enter context data', 'error');
        return;
    }
    
    let context;
    try {
        context = JSON.parse(contextText);
    } catch (e) {
        showMessage('Invalid JSON in context', 'error');
        return;
    }
    
    try {
        const machine = await apiCall(`/machines/${machineId}/context`, 'PUT', {
            context: context
        });
        
        showMessage('Context updated successfully!', 'success');
        displayMachineDetails(machine);
        document.getElementById('contextUpdate').value = '';
        await refreshAvailableTransitions();
        await loadHistory();
    } catch (error) {
        showMessage(`Error updating context: ${error.message}`, 'error');
    }
}

// History
async function loadHistory() {
    const machineId = document.getElementById('machineId').value;
    if (!machineId) {
        return;
    }
    
    const order = document.getElementById('historyOrder')?.value || 'asc';
    
    try {
        const history = await apiCall(`/machines/${machineId}/history?order=${order}`);
        const historyDiv = document.getElementById('historyDisplay');
        
        if (!history || history.length === 0) {
            historyDiv.innerHTML = '<p style="color: #999;">No history available</p>';
            return;
        }
        
        let html = '<div class="history-list">';
        history.forEach((entry, index) => {
            const timestamp = new Date(entry.timestamp).toLocaleString();
            const fromState = entry.fromStateId || '<em>Initial</em>';
            const contextStr = entry.contextSnapshot && Object.keys(entry.contextSnapshot).length > 0
                ? JSON.stringify(entry.contextSnapshot, null, 2)
                : 'No context';
            
            html += `<div class="history-item">`;
            html += `<div class="history-header">`;
            html += `<span class="history-index">#${index + 1}</span>`;
            html += `<span class="history-time">${timestamp}</span>`;
            html += `</div>`;
            html += `<div class="history-transition">`;
            html += `<span class="history-state">${fromState}</span>`;
            html += `<span class="history-arrow">→</span>`;
            html += `<span class="history-state">${entry.toStateId}</span>`;
            html += `<span class="history-transition-id">[${entry.transitionId}]</span>`;
            html += `</div>`;
            html += `<div class="history-context">`;
            html += `<div class="history-context-label">Context Snapshot:</div>`;
            html += `<pre class="context-snapshot">${contextStr}</pre>`;
            html += `</div>`;
            html += `</div>`;
        });
        html += '</div>';
        
        historyDiv.innerHTML = html;
    } catch (error) {
        const historyDiv = document.getElementById('historyDisplay');
        historyDiv.innerHTML = `<p class="error">Error loading history: ${error.message}</p>`;
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    refreshDefinitions();
    refreshMachines();
});

