package com.ezlevup.chatsocket.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ChatRoomRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomRepository.class);
    
    private final ConcurrentHashMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();

    public ChatRoom createChatRoom(String name) {
        String roomId = UUID.randomUUID().toString();
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(name)
                .build();
        
        chatRooms.put(roomId, chatRoom);
        logger.info("채팅방 생성: {} (ID: {})", name, roomId);
        
        return chatRoom;
    }

    public Optional<ChatRoom> findRoomById(String roomId) {
        return Optional.ofNullable(chatRooms.get(roomId));
    }

    public Collection<ChatRoom> findAllRooms() {
        return chatRooms.values();
    }

    public void deleteRoom(String roomId) {
        ChatRoom removedRoom = chatRooms.remove(roomId);
        if (removedRoom != null) {
            logger.info("채팅방 삭제: {} (ID: {})", removedRoom.getName(), roomId);
        }
    }

    public void deleteEmptyRooms() {
        chatRooms.entrySet().removeIf(entry -> {
            ChatRoom room = entry.getValue();
            if (room.isEmpty()) {
                logger.info("빈 채팅방 삭제: {} (ID: {})", room.getName(), room.getRoomId());
                return true;
            }
            return false;
        });
    }

    public boolean existsById(String roomId) {
        return chatRooms.containsKey(roomId);
    }

    public int getTotalRoomCount() {
        return chatRooms.size();
    }

    public ChatRoom getOrCreateRoom(String roomId, String name) {
        return chatRooms.computeIfAbsent(roomId, id -> {
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomId(id)
                    .name(name != null ? name : "Room " + id.substring(0, 8))
                    .build();
            logger.info("채팅방 생성 (기존 ID 사용): {} (ID: {})", chatRoom.getName(), id);
            return chatRoom;
        });
    }

    public void clear() {
        int count = chatRooms.size();
        chatRooms.clear();
        logger.info("모든 채팅방 삭제: {}개", count);
    }
}