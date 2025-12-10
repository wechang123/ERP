// Common JavaScript functions

document.addEventListener('DOMContentLoaded', function() {
    // Load employees for the dropdown
    loadEmployeesForDropdown();

    // Setup employee selection change handler
    const employeeSelect = document.getElementById('currentEmployeeId');
    if (employeeSelect) {
        employeeSelect.addEventListener('change', function() {
            const employeeId = this.value;
            sessionStorage.setItem('currentEmployeeId', employeeId);
            connectWebSocket(employeeId);
        });
    }

    // 요청자 자동 선택 (결재 요청 폼에서)
    setRequesterFromCurrentUser();

    // 결재 처리 페이지 자동 리다이렉트
    autoRedirectToApprovalQueue();
});

function loadEmployeesForDropdown() {
    fetch('/api/employees')
        .then(response => response.json())
        .then(employees => {
            const select = document.getElementById('currentEmployeeId');
            if (select) {
                const savedId = sessionStorage.getItem('currentEmployeeId');
                employees.forEach(emp => {
                    const option = document.createElement('option');
                    option.value = emp.id;
                    option.textContent = `${emp.name} (${emp.department})`;
                    if (savedId && emp.id.toString() === savedId) {
                        option.selected = true;
                    }
                    select.appendChild(option);
                });

                // Connect WebSocket if employee was previously selected
                if (savedId) {
                    connectWebSocket(savedId);
                }
            }
        })
        .catch(error => {
            console.error('Failed to load employees:', error);
        });
}

// 상단바 사용자 선택에 따라 요청자 자동 선택
function setRequesterFromCurrentUser() {
    const requesterId = document.getElementById('requesterId');
    const currentId = sessionStorage.getItem('currentEmployeeId');
    if (requesterId && currentId) {
        // 드롭다운 로드 후 선택되도록 약간의 지연
        setTimeout(() => {
            requesterId.value = currentId;
        }, 100);
    }
}

// 결재 처리 페이지에서 자동 리다이렉트
function autoRedirectToApprovalQueue() {
    // /process/select 페이지인지 확인
    if (window.location.pathname === '/process/select') {
        const currentId = sessionStorage.getItem('currentEmployeeId');
        if (currentId) {
            window.location.href = '/process/' + currentId;
        }
    }
}

// Confirmation dialog
function confirmAction(message) {
    return confirm(message);
}

// Format date
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Status badge HTML (cancelled 상태 추가)
function getStatusBadge(status) {
    const statusLabels = {
        'pending': '대기',
        'approved': '승인',
        'rejected': '반려',
        'cancelled': '취소됨',
        'in_progress': '진행 중'
    };
    const classes = {
        'pending': 'bg-warning text-dark',
        'approved': 'bg-success',
        'rejected': 'bg-danger',
        'cancelled': 'bg-secondary',
        'in_progress': 'bg-info text-dark'
    };
    const badgeClass = classes[status] || 'bg-secondary';
    const label = statusLabels[status] || status;
    return `<span class="badge ${badgeClass}">${label}</span>`;
}

// Show loading overlay
function showLoading() {
    const overlay = document.createElement('div');
    overlay.id = 'loadingOverlay';
    overlay.className = 'spinner-overlay';
    overlay.innerHTML = `
        <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">로딩 중...</span>
        </div>
    `;
    document.body.appendChild(overlay);
}

// Hide loading overlay
function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.remove();
    }
}

// AJAX form submission
function submitFormAjax(formId, successCallback) {
    const form = document.getElementById(formId);
    if (!form) return;

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        showLoading();

        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());

        fetch(form.action, {
            method: form.method || 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            hideLoading();
            if (successCallback) {
                successCallback(result);
            }
        })
        .catch(error => {
            hideLoading();
            console.error('Form submission error:', error);
            showToast('오류', '폼 제출에 실패했습니다', 'danger');
        });
    });
}
