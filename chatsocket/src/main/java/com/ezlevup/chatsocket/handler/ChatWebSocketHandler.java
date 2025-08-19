package com.ezlevup.chatsocket.handler;

import com.ezlevup.chatsocket.model.ChatMessage;
import com.ezlevup.chatsocket.model.ChatRoom;
import com.ezlevup.chatsocket.model.ChatRoomRepository;
import com.ezlevup.chatsocket.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 세션 제한 확인 (최대 1000개 세션)
        if (sessions.size() >= 1000) {
            logger.warn("최대 세션 수 초과, 연결 거부: {}", session.getId());
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        
        sessions.add(session);
        logger.info("웹소켓 연결: {}", session.getId());
        logger.info("현재 연결된 세션 수: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        // 메시지 크기 제한 (최대 1KB)
        if (payload.length() > 1024) {
            logger.warn("메시지 크기 초과 - 세션 ID: {}, 크기: {}bytes", session.getId(), payload.length());
            sendErrorMessage(session, "메시지가 너무 깁니다. (최대 1KB)");
            return;
        }
        
        logger.info("받은 메시지: {}", payload.length() > 100 ? payload.substring(0, 100) + "..." : payload);
        
        // PING 메시지 처리
        if (payload.contains("\"type\":\"PING\"")) {
            logger.debug("PING 메시지 수신 - 세션 ID: {}", session.getId());
            return; // PING 메시지는 무시
        }
        
        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            
            // 메시지 내용 검증
            if (chatMessage.getMessage() != null && chatMessage.getMessage().length() > 500) {
                sendErrorMessage(session, "메시지 내용이 너무 깁니다. (최대 500자)");
                return;
            }
            
            handleMessageByType(session, chatMessage);
        } catch (Exception e) {
            logger.error("메시지 파싱 오류 - 세션 ID: {}, 오류: {}", session.getId(), e.getMessage());
            sendErrorMessage(session, "잘못된 메시지 형식입니다.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        handleUserLeave(session);
        sessions.remove(session);
        logger.info("웹소켓 연결 종료: {}", session.getId());
        logger.info("현재 연결된 세션 수: {}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("웹소켓 전송 오류 - 세션 ID: {}, 오류: {}", session.getId(), exception.getMessage());
        handleUserLeave(session);
        sessions.remove(session);
    }

    private void handleMessageByType(WebSocketSession session, ChatMessage chatMessage) {
        switch (chatMessage.getType()) {
            case ENTER:
                handleUserEnter(session, chatMessage);
                break;
            case TALK:
                handleUserTalk(session, chatMessage);
                break;
            case QUIT:
                handleUserQuit(session, chatMessage);
                break;
            default:
                logger.warn("알 수 없는 메시지 타입: {}", chatMessage.getType());
        }
    }

    private void handleUserEnter(WebSocketSession session, ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
        String sender = chatMessage.getSender();
        
        if (roomId == null || sender == null) {
            sendErrorMessage(session, "방 ID와 사용자명은 필수입니다.");
            return;
        }
        
        // 세션에 사용자 정보 저장
        session.getAttributes().put("sender", sender);
        
        ChatRoom room = chatRoomRepository.getOrCreateRoom(roomId, "Room " + roomId.substring(0, Math.min(8, roomId.length())));
        room.addSession(session);
        sessionRoomMap.put(session.getId(), roomId);
        
        logger.info("사용자 입장: {} -> 방: {}", sender, roomId);
        
        ChatMessage enterMessage = new ChatMessage(MessageType.ENTER, roomId, sender, sender + "님이 입장하셨습니다.");
        broadcastToRoom(roomId, enterMessage, null);
    }

    private void handleUserTalk(WebSocketSession session, ChatMessage chatMessage) {
        String roomId = sessionRoomMap.get(session.getId());
        if (roomId == null) {
            sendErrorMessage(session, "채팅방에 입장 후 메시지를 보내주세요.");
            return;
        }
        
        chatMessage.setRoomId(roomId);
        logger.info("채팅 메시지: {} -> 방: {}, 내용: {}", chatMessage.getSender(), roomId, chatMessage.getMessage());
        
        // 모든 사용자에게 메시지 브로드캐스트 (자신 포함)
        broadcastToRoom(roomId, chatMessage, null);
    }

    private void handleUserQuit(WebSocketSession session, ChatMessage chatMessage) {
        handleUserLeave(session);
    }

    private void handleUserLeave(WebSocketSession session) {
        String roomId = sessionRoomMap.remove(session.getId());
        if (roomId != null) {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findRoomById(roomId);
            if (roomOpt.isPresent()) {
                ChatRoom room = roomOpt.get();
                room.removeSession(session);
                
                String sessionInfo = (String) session.getAttributes().get("sender");
                String sender = sessionInfo != null ? sessionInfo : "사용자";
                
                logger.info("사용자 퇴장: {} -> 방: {}", sender, roomId);
                
                ChatMessage quitMessage = new ChatMessage(MessageType.QUIT, roomId, sender, sender + "님이 퇴장하셨습니다.");
                broadcastToRoom(roomId, quitMessage, null);
                
                if (room.isEmpty()) {
                    chatRoomRepository.deleteRoom(roomId);
                }
            }
        }
    }

    private void broadcastToRoom(String roomId, ChatMessage message, WebSocketSession excludeSession) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findRoomById(roomId);
        if (roomOpt.isEmpty()) {
            logger.warn("존재하지 않는 채팅방: {}", roomId);
            return;
        }
        
        ChatRoom room = roomOpt.get();
        logger.info("방 {} 에 브로드캐스트: {} - 세션 수: {}", roomId, message.getMessage(), room.getSessionCount());
        
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("메시지 직렬화 오류: {}", e.getMessage());
            return;
        }
        
        int sentCount = 0;
        for (WebSocketSession session : room.getSessions()) {
            if (session.isOpen()) {
                // TALK 메시지의 경우에만 전송자 제외, 다른 메시지는 모두에게 전송
                if (message.getType() == MessageType.TALK && session.equals(excludeSession)) {
                    continue;
                }
                
                try {
                    session.sendMessage(new TextMessage(messageJson));
                    sentCount++;
                    logger.info("메시지 전송 성공 - 세션 ID: {}", session.getId());
                } catch (IOException e) {
                    logger.error("메시지 전송 실패 - 세션 ID: {}, 오류: {}", session.getId(), e.getMessage());
                    room.removeSession(session);
                    sessions.remove(session);
                }
            }
        }
        
        logger.info("브로드캐스트 완료 - 전송된 세션 수: {}/{}", sentCount, room.getSessionCount());
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            ChatMessage error = new ChatMessage(MessageType.TALK, "system", "System", errorMessage);
            String errorJson = objectMapper.writeValueAsString(error);
            session.sendMessage(new TextMessage(errorJson));
        } catch (Exception e) {
            logger.error("에러 메시지 전송 실패 - 세션 ID: {}, 오류: {}", session.getId(), e.getMessage());
        }
    }

    public Set<WebSocketSession> getSessions() {
        return sessions;
    }
}