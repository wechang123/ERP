package com.erp.notification.controller;

import com.erp.notification.dto.NotificationMessage;
import com.erp.notification.handler.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationWebSocketHandler webSocketHandler;

    // Approval Request Service에서 호출하여 알림 전송
    @PostMapping("/{employeeId}")
    public ResponseEntity<String> sendNotification(
            @PathVariable String employeeId,
            @RequestBody NotificationMessage message) {

        log.info("Sending notification to employeeId={}: {}", employeeId, message);
        webSocketHandler.sendNotification(employeeId, message);
        return ResponseEntity.ok("Notification sent");
    }

    // 연결 상태 확인
    @GetMapping("/{employeeId}/status")
    public ResponseEntity<Boolean> checkConnection(@PathVariable String employeeId) {
        return ResponseEntity.ok(webSocketHandler.isConnected(employeeId));
    }
}
