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
        sessions.add(session);
        logger.info("웹소켓 연결: {}", session.getId());
        logger.info("현재 연결된 세션 수: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("받은 메시지: {}", payload);
        
        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
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
        
        broadcastToRoom(roomId, chatMessage, session);
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
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("메시지 직렬화 오류: {}", e.getMessage());
            return;
        }
        
        room.getSessions().parallelStream()
                .filter(session -> session.isOpen() && !session.equals(excludeSession))
                .forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                    } catch (IOException e) {
                        logger.error("메시지 전송 실패 - 세션 ID: {}, 오류: {}", session.getId(), e.getMessage());
                        room.removeSession(session);
                        sessions.remove(session);
                    }
                });
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