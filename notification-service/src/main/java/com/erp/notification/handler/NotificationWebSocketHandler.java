package com.erp.notification.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // employeeId -> WebSocketSession 매핑
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // employeeId -> 대기 중인 알림 목록 (최근 30초 이내)
    private final Map<String, List<PendingNotification>> pendingNotifications = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class PendingNotification {
        Object notification;
        long timestamp;

        PendingNotification(Object notification) {
            this.notification = notification;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000; // 30초 후 만료
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String employeeId = extractEmployeeId(session);
        if (employeeId != null) {
            sessions.put(employeeId, session);
            log.info("WebSocket connected: employeeId={}", employeeId);

            // 연결 시 대기 중인 알림 전송
            sendPendingNotifications(employeeId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String employeeId = extractEmployeeId(session);
        if (employeeId != null) {
            // 현재 세션과 동일한 경우에만 제거
            WebSocketSession currentSession = sessions.get(employeeId);
            if (currentSession != null && currentSession.getId().equals(session.getId())) {
                sessions.remove(employeeId);
            }
            log.info("WebSocket disconnected: employeeId={}", employeeId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message: {}", message.getPayload());
    }

    // 특정 직원에게 알림 전송
    public void sendNotification(String employeeId, Object notification) {
        WebSocketSession session = sessions.get(employeeId);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(message));
                log.info("Notification sent to employeeId={}: {}", employeeId, message);
                // 전송 성공 - 대기열에 추가하지 않음
                return;
            } catch (IOException e) {
                log.error("Failed to send notification to employeeId={}", employeeId, e);
            }
        }
        // 세션이 없거나 전송 실패 시에만 대기열에 추가
        addPendingNotification(employeeId, notification);
        log.warn("No active session for employeeId={}, notification queued", employeeId);
    }

    // 대기 알림 추가
    private void addPendingNotification(String employeeId, Object notification) {
        pendingNotifications.computeIfAbsent(employeeId, k -> Collections.synchronizedList(new ArrayList<>()));
        List<PendingNotification> list = pendingNotifications.get(employeeId);

        // 만료된 알림 제거
        list.removeIf(PendingNotification::isExpired);

        // 새 알림 추가 (최대 10개)
        if (list.size() >= 10) {
            list.remove(0);
        }
        list.add(new PendingNotification(notification));
    }

    // 연결 시 대기 중인 알림 전송
    private void sendPendingNotifications(String employeeId, WebSocketSession session) {
        List<PendingNotification> list = pendingNotifications.get(employeeId);
        if (list == null || list.isEmpty()) {
            return;
        }

        // 짧은 지연 후 전송 (연결 안정화)
        new Thread(() -> {
            try {
                Thread.sleep(100); // 100ms 대기 (500ms에서 단축)

                List<PendingNotification> toSend = new ArrayList<>();
                synchronized (list) {
                    list.removeIf(PendingNotification::isExpired);
                    toSend.addAll(list);
                    list.clear();
                }

                for (PendingNotification pn : toSend) {
                    if (session.isOpen()) {
                        try {
                            String message = objectMapper.writeValueAsString(pn.notification);
                            session.sendMessage(new TextMessage(message));
                            log.info("Pending notification sent to employeeId={}: {}", employeeId, message);
                        } catch (IOException e) {
                            log.error("Failed to send pending notification to employeeId={}", employeeId, e);
                            // 실패 시 다시 대기 목록에 추가
                            addPendingNotification(employeeId, pn.notification);
                        }
                    } else {
                        // 세션이 닫힌 경우 대기열에 다시 추가
                        log.warn("Session closed before sending pending notification to employeeId={}", employeeId);
                        addPendingNotification(employeeId, pn.notification);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private String extractEmployeeId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            String query = uri.getQuery();
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "id".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    public boolean isConnected(String employeeId) {
        WebSocketSession session = sessions.get(employeeId);
        return session != null && session.isOpen();
    }
}
