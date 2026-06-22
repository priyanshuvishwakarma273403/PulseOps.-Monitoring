package com.pulseops.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class RealtimeDashboardHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> orgSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long orgId = getOrgId(session);
        if (orgId != null) {
            orgSessionMap.computeIfAbsent(orgId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WebSocket session established: {} for Organization: {}", session.getId(), orgId);
        } else {
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException e) {
                log.error("Failed to close invalid session", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long orgId = getOrgId(session);
        if (orgId != null && orgSessionMap.containsKey(orgId)) {
            orgSessionMap.get(orgId).remove(session);
            log.info("WebSocket session closed: {} for Organization: {}", session.getId(), orgId);
        }
    }

    public void broadcastToOrg(Long orgId, String message) {
        Set<WebSocketSession> sessions = orgSessionMap.get(orgId);
        if (sessions != null && !sessions.isEmpty()) {
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.error("Failed to send message to session: {}", session.getId(), e);
                }
            }
        }
    }

    private Long getOrgId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query != null && query.contains("orgId=")) {
            try {
                String val = query.split("orgId=")[1].split("&")[0];
                return Long.parseLong(val);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
