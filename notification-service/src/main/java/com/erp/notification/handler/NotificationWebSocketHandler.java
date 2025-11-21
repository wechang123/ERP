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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // employeeId -> WebSocketSession 매핑
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String employeeId = extractEmployeeId(session);
        if (employeeId != null) {
            sessions.put(employeeId, session);
            log.info("WebSocket connected: employeeId={}", employeeId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String employeeId = extractEmployeeId(session);
        if (employeeId != null) {
            sessions.remove(employeeId);
            log.info("WebSocket disconnected: employeeId={}", employeeId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 클라이언트로부터의 메시지 처리 (필요시 구현)
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
            } catch (IOException e) {
                log.error("Failed to send notification to employeeId={}", employeeId, e);
            }
        } else {
            log.warn("No active session for employeeId={}", employeeId);
        }
    }

    // URL 쿼리 파라미터에서 employeeId 추출
    // ws://localhost:8084/ws?id={employeeId}
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
