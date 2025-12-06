// WebSocket connection for real-time notifications
let ws = null;
let notifications = [];

function connectWebSocket(employeeId) {
    if (ws) {
        ws.close();
    }

    if (!employeeId) {
        console.log('No employee selected, WebSocket not connected');
        return;
    }

    const wsUrl = `ws://localhost:8080/ws?id=${employeeId}`;
    console.log('Connecting to WebSocket:', wsUrl);

    ws = new WebSocket(wsUrl);

    ws.onopen = function() {
        console.log('WebSocket connected for employee:', employeeId);
        // 세션 내 첫 연결 시에만 토스트 표시
        if (!sessionStorage.getItem('wsConnected')) {
            showToast('연결됨', '실시간 알림이 활성화되었습니다', 'success');
            sessionStorage.setItem('wsConnected', 'true');
        }
    };

    ws.onmessage = function(event) {
        console.log('Received notification:', event.data);
        try {
            const notification = JSON.parse(event.data);
            handleNotification(notification);
        } catch (e) {
            console.error('Failed to parse notification:', e);
        }
    };

    ws.onclose = function() {
        console.log('WebSocket disconnected');
        // 3초 후 자동 재연결 시도
        setTimeout(() => {
            const currentEmployee = sessionStorage.getItem('currentEmployeeId');
            if (currentEmployee && currentEmployee === employeeId) {
                console.log('Attempting to reconnect WebSocket for employee:', currentEmployee);
                connectWebSocket(currentEmployee);
            }
        }, 3000);
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
    };
}

function handleNotification(notification) {
    console.log('Processing notification:', notification);

    // Add to notifications list
    notifications.unshift(notification);

    // 알림을 sessionStorage에도 저장 (페이지 새로고침 후에도 표시)
    saveNotificationToStorage(notification);

    updateNotificationBadge();
    updateNotificationList();

    // Determine toast content based on notification type
    let title, message, type;

    switch(notification.type) {
        case 'NEW_APPROVAL':
            title = '새 결재 요청';
            message = notification.message || `새로운 결재 요청이 도착했습니다 (요청 #${notification.requestId})`;
            type = 'info';
            break;
        case 'APPROVAL_RESULT':
            const isApproved = notification.status === 'approved';
            title = isApproved ? '결재 승인' : '결재 반려';
            message = notification.message || `요청 #${notification.requestId}이(가) ${isApproved ? '승인' : '반려'}되었습니다`;
            type = isApproved ? 'success' : 'danger';
            break;
        case 'DEADLINE_REMINDER':
            title = '결재 기한 임박';
            message = notification.message || `요청 #${notification.requestId}의 결재 기한이 곧 만료됩니다`;
            type = 'warning';
            break;
        case 'DEADLINE_OVERDUE':
            title = '결재 기한 초과';
            message = notification.message || `요청 #${notification.requestId}의 결재 기한이 지났습니다`;
            type = 'danger';
            break;
        case 'REQUEST_CANCELLED':
            title = '요청 취소';
            message = notification.message || `요청 #${notification.requestId}이(가) 취소되었습니다`;
            type = 'warning';
            break;
        default:
            title = notification.title || '알림';
            message = notification.message || '새로운 알림이 있습니다';
            type = 'info';
    }

    showToast(title, message, type);
}

function updateNotificationBadge() {
    const badge = document.getElementById('notificationBadge');
    if (badge) {
        if (notifications.length > 0) {
            badge.textContent = notifications.length > 9 ? '9+' : notifications.length;
            badge.classList.remove('d-none');
        } else {
            badge.classList.add('d-none');
        }
    }
}

function updateNotificationList() {
    const list = document.getElementById('notificationList');
    if (list) {
        list.classList.remove('text-muted');
        if (notifications.length === 0) {
            list.classList.add('text-muted');
            list.innerHTML = '알림 없음';
        } else {
            list.innerHTML = notifications.slice(0, 5).map(n => {
                let bgClass, statusText;
                switch(n.type) {
                    case 'NEW_APPROVAL':
                        bgClass = 'bg-info-subtle';
                        statusText = '새 요청';
                        break;
                    case 'APPROVAL_RESULT':
                        bgClass = n.status === 'approved' ? 'bg-success-subtle' : 'bg-danger-subtle';
                        statusText = n.status === 'approved' ? '승인' : '반려';
                        break;
                    case 'DEADLINE_REMINDER':
                        bgClass = 'bg-warning-subtle';
                        statusText = '기한 임박';
                        break;
                    case 'DEADLINE_OVERDUE':
                        bgClass = 'bg-danger-subtle';
                        statusText = '기한 초과';
                        break;
                    case 'REQUEST_CANCELLED':
                        bgClass = 'bg-secondary-subtle';
                        statusText = '취소됨';
                        break;
                    default:
                        bgClass = 'bg-light';
                        statusText = '알림';
                }
                return `
                    <div class="mb-2 p-2 rounded ${bgClass}">
                        <small class="fw-bold">요청 #${n.requestId}</small><br>
                        <small class="text-muted">${statusText}</small>
                    </div>
                `;
            }).join('');
        }
    }
}

function showToast(title, message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toastId = 'toast-' + Date.now();
    const bgClass = {
        'success': 'bg-success',
        'danger': 'bg-danger',
        'warning': 'bg-warning',
        'info': 'bg-info'
    }[type] || 'bg-primary';

    const textClass = type === 'warning' ? 'text-dark' : 'text-white';

    const toastHtml = `
        <div id="${toastId}" class="toast" role="alert">
            <div class="toast-header ${bgClass} ${textClass}">
                <i class="bi bi-bell me-2"></i>
                <strong class="me-auto">${title}</strong>
                <small>방금</small>
                <button type="button" class="btn-close ${type === 'warning' ? '' : 'btn-close-white'}" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: 5000 });
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

// Disconnect on page unload
window.addEventListener('beforeunload', function() {
    if (ws) {
        ws.close();
    }
});

// sessionStorage에 알림 저장 (최대 10개)
function saveNotificationToStorage(notification) {
    let stored = JSON.parse(sessionStorage.getItem('pendingNotifications') || '[]');
    notification.timestamp = Date.now();
    stored.unshift(notification);
    if (stored.length > 10) stored = stored.slice(0, 10);
    sessionStorage.setItem('pendingNotifications', JSON.stringify(stored));
}

// 페이지 로드 시 저장된 알림 표시
function showPendingNotifications() {
    const stored = JSON.parse(sessionStorage.getItem('pendingNotifications') || '[]');
    const now = Date.now();
    const recent = stored.filter(n => now - n.timestamp < 10000); // 10초 이내 알림만

    if (recent.length > 0) {
        console.log('Showing pending notifications:', recent.length);
        recent.forEach(notification => {
            // notifications 배열에 없으면 추가
            if (!notifications.find(n => n.requestId === notification.requestId && n.type === notification.type)) {
                notifications.unshift(notification);
            }
        });
        updateNotificationBadge();
        updateNotificationList();

        // 가장 최근 알림 토스트로 표시
        const latest = recent[0];
        if (latest && now - latest.timestamp < 3000) { // 3초 이내면 토스트 표시
            showToastForNotification(latest);
        }

        // 표시 후 정리
        sessionStorage.setItem('pendingNotifications', JSON.stringify(stored.filter(n => now - n.timestamp >= 10000)));
    }
}

function showToastForNotification(notification) {
    let title, message, type;
    switch(notification.type) {
        case 'NEW_APPROVAL':
            title = '새 결재 요청';
            message = notification.message || `새로운 결재 요청이 도착했습니다`;
            type = 'info';
            break;
        case 'APPROVAL_RESULT':
            const isApproved = notification.status === 'approved';
            title = isApproved ? '결재 승인' : '결재 반려';
            message = notification.message || `요청이 ${isApproved ? '승인' : '반려'}되었습니다`;
            type = isApproved ? 'success' : 'danger';
            break;
        default:
            title = notification.title || '알림';
            message = notification.message || '새 알림';
            type = 'info';
    }
    showToast(title, message, type);
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(showPendingNotifications, 500);
});
