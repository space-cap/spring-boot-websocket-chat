package com.ezlevup.chatsocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        logger.info("웹소켓 연결: {}", session.getId());
        logger.info("현재 연결된 세션 수: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("받은 메시지: {}", payload);
        
        // 모든 연결된 세션에 메시지 브로드캐스트
        broadcastMessage(message, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        logger.info("웹소켓 연결 종료: {}", session.getId());
        logger.info("현재 연결된 세션 수: {}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("웹소켓 전송 오류 - 세션 ID: {}, 오류: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    private void broadcastMessage(TextMessage message, WebSocketSession senderSession) {
        sessions.parallelStream()
                .filter(session -> session.isOpen() && !session.equals(senderSession))
                .forEach(session -> {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        logger.error("메시지 전송 실패 - 세션 ID: {}, 오류: {}", session.getId(), e.getMessage());
                        sessions.remove(session);
                    }
                });
    }

    public Set<WebSocketSession> getSessions() {
        return sessions;
    }
}