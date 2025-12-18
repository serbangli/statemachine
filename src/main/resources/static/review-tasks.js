const API_BASE = '/api/tasks';
let currentTaskId = null;

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
    element.style.display = 'block';
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
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    } catch (error) {
        console.error('API call failed:', error);
        throw error;
    }
}

// Create Task
async function createTask() {
    const taskId = document.getElementById('taskId').value.trim();
    const ownerId = document.getElementById('ownerId').value.trim();
    const machineDefinitionId = document.getElementById('machineDefinitionId').value.trim();

    if (!taskId || !ownerId || !machineDefinitionId) {
        showMessage('Please fill in all required fields', 'error');
        return;
    }

    try {
        const request = {
            taskId: taskId,
            ownerId: ownerId,
            machineDefinitionId: machineDefinitionId
        };

        const result = await apiCall('', 'POST', request);
        showMessage(`Task "${taskId}" created successfully!`, 'success');
        showResult('createTaskResult', `Task created with Machine ID: ${result.machineId}`, true);
        
        // Auto-load the created task
        document.getElementById('loadTaskId').value = taskId;
        await loadTask();
    } catch (error) {
        showMessage(`Error creating task: ${error.message}`, 'error');
        showResult('createTaskResult', `Error: ${error.message}`, false);
    }
}

// Load Task
async function loadTask() {
    const taskId = document.getElementById('loadTaskId').value.trim();
    
    if (!taskId) {
        showMessage('Please enter a task ID', 'error');
        return;
    }

    try {
        currentTaskId = taskId;
        
        // We need to get the task details. Since there's no direct GET endpoint,
        // we'll try to get available transitions which will validate the task exists
        // and we can infer the task state from the response
        // This will also update the owner action button states
        await loadAvailableTransitions();
        
        showMessage(`Task "${taskId}" loaded successfully!`, 'success');
        
        // Show all sections
        document.getElementById('addReviewerSection').style.display = 'block';
        document.getElementById('submitReviewSection').style.display = 'block';
        document.getElementById('ownerActionsSection').style.display = 'block';
        document.getElementById('transitionsSection').style.display = 'block';
        
        // Try to get task details by attempting to get transitions
        // This will help us understand the current state
        refreshTaskDetails();
    } catch (error) {
        showMessage(`Error loading task: ${error.message}`, 'error');
        document.getElementById('taskDetails').style.display = 'none';
        // Disable all buttons on error
        updateOwnerActionButtons([]);
        updateReviewerActionButtons([], []);
    }
}

// Refresh task details by getting available transitions
async function refreshTaskDetails() {
    if (!currentTaskId) return;
    
    try {
        const transitions = await apiCall(`/${currentTaskId}/transitions/available`);
        
        // Display task info (we'll show what we can infer)
        const taskDetailsDiv = document.getElementById('taskDetails');
        taskDetailsDiv.style.display = 'block';
        
        // We can't get full task details without a GET endpoint, but we can show
        // that the task exists and is loaded
        document.getElementById('taskMachineId').textContent = 'Task loaded (details available after operations)';
        document.getElementById('taskContext').textContent = 'Context will be displayed after operations';
        document.getElementById('taskCreated').textContent = 'N/A';
        document.getElementById('taskUpdated').textContent = 'N/A';
        
        // Try to infer state from transitions if available
        if (transitions && transitions.length > 0) {
            // State might be inferred from transition source states
            document.getElementById('currentTaskState').textContent = 'Active (check transitions for details)';
        } else {
            document.getElementById('currentTaskState').textContent = 'Unknown';
        }
    } catch (error) {
        console.error('Error refreshing task details:', error);
    }
}

// Add Reviewer
async function addReviewer() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    const reviewerId = document.getElementById('reviewerId').value.trim();
    const reviewerName = document.getElementById('reviewerName').value.trim();
    const reviewerEmail = document.getElementById('reviewerEmail').value.trim();

    if (!reviewerId || !reviewerName || !reviewerEmail) {
        showMessage('Please fill in all reviewer fields', 'error');
        return;
    }

    try {
        const request = {
            taskId: currentTaskId,
            userId: reviewerId,
            userName: reviewerName,
            userEmail: reviewerEmail
        };

        const result = await apiCall(`/${currentTaskId}/reviewer`, 'POST', request);
        showMessage(`Reviewer "${reviewerName}" added successfully!`, 'success');
        showResult('addReviewerResult', 'Reviewer added and transition executed', true);
        
        // Update task details
        updateTaskDisplay(result);
        
        // Refresh available transitions (this will also update button states)
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error adding reviewer: ${error.message}`, 'error');
        showResult('addReviewerResult', `Error: ${error.message}`, false);
    }
}

// Submit Review
async function submitReview() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    const reviewerId = document.getElementById('reviewReviewerId').value.trim();
    const reviewerName = document.getElementById('reviewReviewerName').value.trim();
    const reviewerEmail = document.getElementById('reviewReviewerEmail').value.trim();

    if (!reviewerId || !reviewerName || !reviewerEmail) {
        showMessage('Please fill in all reviewer fields', 'error');
        return;
    }

    try {
        const request = {
            taskId: currentTaskId,
            userId: reviewerId,
            userName: reviewerName,
            userEmail: reviewerEmail
        };

        const result = await apiCall(`/${currentTaskId}/review`, 'POST', request);
        showMessage(`Review submitted successfully!`, 'success');
        showResult('submitReviewResult', 'Review submitted and transition executed', true);
        
        // Update task details
        updateTaskDisplay(result);
        
        // Refresh available transitions (this will also update button states)
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error submitting review: ${error.message}`, 'error');
        showResult('submitReviewResult', `Error: ${error.message}`, false);
    }
}

// Owner Actions
async function closeReview() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    try {
        const result = await apiCall(`/${currentTaskId}/review/close`, 'POST');
        showMessage('Review closed successfully!', 'success');
        showResult('ownerActionsResult', 'Review closed', true);
        updateTaskDisplay(result);
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error closing review: ${error.message}`, 'error');
        showResult('ownerActionsResult', `Error: ${error.message}`, false);
    }
}

async function reopenReview() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    try {
        const result = await apiCall(`/${currentTaskId}/review/reopen`, 'POST');
        showMessage('Review reopened successfully!', 'success');
        showResult('ownerActionsResult', 'Review reopened', true);
        updateTaskDisplay(result);
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error reopening review: ${error.message}`, 'error');
        showResult('ownerActionsResult', `Error: ${error.message}`, false);
    }
}

async function rollbackReview() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    try {
        const result = await apiCall(`/${currentTaskId}/review/rollback`, 'POST');
        showMessage('Review rolled back successfully!', 'success');
        showResult('ownerActionsResult', 'Review rolled back', true);
        updateTaskDisplay(result);
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error rolling back review: ${error.message}`, 'error');
        showResult('ownerActionsResult', `Error: ${error.message}`, false);
    }
}

async function startIntegration() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    try {
        const result = await apiCall(`/${currentTaskId}/integration/start`, 'POST');
        showMessage('Integration started successfully!', 'success');
        showResult('ownerActionsResult', 'Integration started', true);
        updateTaskDisplay(result);
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error starting integration: ${error.message}`, 'error');
        showResult('ownerActionsResult', `Error: ${error.message}`, false);
    }
}

async function cancelIntegration() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    try {
        const result = await apiCall(`/${currentTaskId}/integration/cancel`, 'POST');
        showMessage('Integration cancelled successfully!', 'success');
        showResult('ownerActionsResult', 'Integration cancelled', true);
        updateTaskDisplay(result);
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error cancelling integration: ${error.message}`, 'error');
        showResult('ownerActionsResult', `Error: ${error.message}`, false);
    }
}

async function finishIntegration() {
    if (!currentTaskId) {
        showMessage('Please load a task first', 'error');
        return;
    }

    try {
        const result = await apiCall(`/${currentTaskId}/integration/finish`, 'POST');
        showMessage('Integration finished successfully!', 'success');
        showResult('ownerActionsResult', 'Integration finished', true);
        updateTaskDisplay(result);
        await loadAvailableTransitions();
    } catch (error) {
        showMessage(`Error finishing integration: ${error.message}`, 'error');
        showResult('ownerActionsResult', `Error: ${error.message}`, false);
    }
}

// Load Available Transitions
async function loadAvailableTransitions() {
    if (!currentTaskId) {
        const taskId = document.getElementById('loadTaskId').value.trim();
        if (!taskId) {
            return;
        }
        currentTaskId = taskId;
    }

    try {
        // Get transitions filtered by owner role to determine which buttons to enable
        const ownerTransitions = await apiCall(`/${currentTaskId}/transitions/available?role=owner`);
        
        // Get transitions filtered by reviewer role
        const reviewerTransitions = await apiCall(`/${currentTaskId}/transitions/available?role=reviewer`);
        
        // Update owner action buttons based on available transitions
        updateOwnerActionButtons(ownerTransitions || []);
        
        // Update reviewer action buttons based on available transitions
        updateReviewerActionButtons(reviewerTransitions || [], ownerTransitions || []);
        
        // Display all available transitions (respecting role filter)
        const roleFilter = document.getElementById('transitionRoleFilter').value;
        const endpoint = `/${currentTaskId}/transitions/available${roleFilter ? `?role=${roleFilter}` : ''}`;
        const transitions = await apiCall(endpoint);
        
        const transitionsList = document.getElementById('availableTransitionsList');
        
        if (!transitions || transitions.length === 0) {
            transitionsList.innerHTML = '<p style="color: #999; padding: 10px;">No available transitions for this task.</p>';
            return;
        }

        // Group transitions by role
        const transitionsByRole = {};
        transitions.forEach(trans => {
            const role = trans.role || 'No Role';
            if (!transitionsByRole[role]) {
                transitionsByRole[role] = [];
            }
            transitionsByRole[role].push(trans);
        });

        let html = '';
        Object.keys(transitionsByRole).sort().forEach(role => {
            html += `<div class="transition-group">`;
            html += `<div class="transition-group-header">`;
            html += `<h4>${role === 'No Role' ? 'No Role Required' : role} (${transitionsByRole[role].length} available)</h4>`;
            html += `</div>`;
            html += `<div class="transition-group-items">`;
            
            transitionsByRole[role].forEach(trans => {
                html += `<div class="transition-item">`;
                html += `<div><span class="transition-id">${trans.id}</span><span class="transition-name">${trans.name}</span></div>`;
                html += `<div class="transition-path">${trans.sourceStateId} → ${trans.destinationStateId}</div>`;
                if (trans.condition) {
                    html += `<div class="transition-condition">Condition: ${trans.condition.expression || 'N/A'}</div>`;
                }
                if (trans.role) {
                    html += `<div class="transition-role">Role: ${trans.role}</div>`;
                }
                html += `</div>`;
            });
            
            html += `</div></div>`;
        });

        transitionsList.innerHTML = html;
    } catch (error) {
        const transitionsList = document.getElementById('availableTransitionsList');
        transitionsList.innerHTML = `<p style="color: #dc3545; padding: 10px;">Error loading transitions: ${error.message}</p>`;
        // Disable all buttons on error
        updateOwnerActionButtons([]);
        updateReviewerActionButtons([], []);
    }
}

// Update Owner Action Buttons based on available transitions
function updateOwnerActionButtons(availableTransitions) {
    // Map transition IDs to button IDs
    const transitionToButtonMap = {
        'o_t1': 'btnCloseReview',
        'o_t2': 'btnReopenReview',        
        'o_t3': 'btnRollbackReview',
        'o_t4': 'btnStartIntegration',
        'o_t5': 'btnCancelIntegration',
        'o_t6': 'btnFinishIntegration'
    };
    
    // Create a set of available transition IDs for quick lookup
    const availableTransitionIds = new Set(availableTransitions.map(t => t.id));
    
    // Update each button
    Object.entries(transitionToButtonMap).forEach(([transitionId, buttonId]) => {
        const button = document.getElementById(buttonId);
        if (button) {
            const isAvailable = availableTransitionIds.has(transitionId);
            button.disabled = !isAvailable;
            
            // Update visual style based on availability
            if (isAvailable) {
                button.style.opacity = '1';
                button.style.cursor = 'pointer';
                button.title = '';
            } else {
                button.style.opacity = '0.5';
                button.style.cursor = 'not-allowed';
                button.title = 'This action is not available in the current state';
            }
        }
    });
}

// Update Reviewer Action Buttons based on available transitions
function updateReviewerActionButtons(reviewerTransitions, ownerTransitions) {
    // Map transition IDs to button IDs
    // o_t0 is used for adding reviewer (owner action that triggers when reviewer is added)
    // r_t0 is used for submitting review (reviewer action)
    const addReviewerTransitionIds = ['o_t0']; // Owner transition triggered when adding reviewer
    const submitReviewTransitionIds = ['r_t0', 'r_t2']; // Reviewer transitions for submitting review
    
    // Create sets of available transition IDs for quick lookup
    const availableReviewerTransitionIds = new Set(reviewerTransitions.map(t => t.id));
    const availableOwnerTransitionIds = new Set(ownerTransitions.map(t => t.id));
    
    // Update Add Reviewer button (check if o_t0 is available in owner transitions)
    const addReviewerButton = document.getElementById('btnAddReviewer');
    if (addReviewerButton) {
        const isAddReviewerAvailable = addReviewerTransitionIds.some(id => availableOwnerTransitionIds.has(id));
        addReviewerButton.disabled = !isAddReviewerAvailable;
        
        if (isAddReviewerAvailable) {
            addReviewerButton.style.opacity = '1';
            addReviewerButton.style.cursor = 'pointer';
            addReviewerButton.title = '';
        } else {
            addReviewerButton.style.opacity = '0.5';
            addReviewerButton.style.cursor = 'not-allowed';
            addReviewerButton.title = 'Adding reviewer is not available in the current state';
        }
    }
    
    // Update Submit Review button (check if reviewer transitions are available)
    const submitReviewButton = document.getElementById('btnSubmitReview');
    if (submitReviewButton) {
        const isSubmitReviewAvailable = submitReviewTransitionIds.some(id => availableReviewerTransitionIds.has(id));
        submitReviewButton.disabled = !isSubmitReviewAvailable;
        
        if (isSubmitReviewAvailable) {
            submitReviewButton.style.opacity = '1';
            submitReviewButton.style.cursor = 'pointer';
            submitReviewButton.title = '';
        } else {
            submitReviewButton.style.opacity = '0.5';
            submitReviewButton.style.cursor = 'not-allowed';
            submitReviewButton.title = 'Submitting review is not available in the current state';
        }
    }
}

// Update Task Display
function updateTaskDisplay(taskResponse) {
    if (!taskResponse) return;

    const taskDetailsDiv = document.getElementById('taskDetails');
    taskDetailsDiv.style.display = 'block';

    document.getElementById('currentTaskState').textContent = taskResponse.currentStateId || 'Unknown';
    document.getElementById('taskMachineId').textContent = taskResponse.machineId || 'N/A';
    
    if (taskResponse.context) {
        document.getElementById('taskContext').textContent = JSON.stringify(taskResponse.context, null, 2);
    } else {
        document.getElementById('taskContext').textContent = 'No context';
    }
    
    document.getElementById('taskCreated').textContent = taskResponse.createdAt || 'N/A';
    document.getElementById('taskUpdated').textContent = taskResponse.updatedAt || 'N/A';
}

// Allow Enter key to submit forms
document.addEventListener('DOMContentLoaded', function() {
    // Create task form
    const taskIdInput = document.getElementById('taskId');
    const ownerIdInput = document.getElementById('ownerId');
    const machineDefInput = document.getElementById('machineDefinitionId');
    
    [taskIdInput, ownerIdInput, machineDefInput].forEach(input => {
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                createTask();
            }
        });
    });

    // Load task form
    const loadTaskIdInput = document.getElementById('loadTaskId');
    loadTaskIdInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            loadTask();
        }
    });
});
