package com.ezlevup.chatsocket.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatModelTests {

    private ObjectMapper objectMapper;
    private ChatRoomRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        repository = new ChatRoomRepository();
    }

    @Test
    void testChatMessageJsonSerialization() throws Exception {
        ChatMessage message = new ChatMessage(MessageType.TALK, "room1", "user1", "Hello World");
        
        String json = objectMapper.writeValueAsString(message);
        assertNotNull(json);
        assertTrue(json.contains("TALK"));
        assertTrue(json.contains("room1"));
        assertTrue(json.contains("user1"));
        assertTrue(json.contains("Hello World"));
        
        ChatMessage deserializedMessage = objectMapper.readValue(json, ChatMessage.class);
        assertEquals(MessageType.TALK, deserializedMessage.getType());
        assertEquals("room1", deserializedMessage.getRoomId());
        assertEquals("user1", deserializedMessage.getSender());
        assertEquals("Hello World", deserializedMessage.getMessage());
    }

    @Test
    void testChatRoomBuilderPattern() {
        ChatRoom room = ChatRoom.builder()
                .roomId("test-room-1")
                .name("Test Room")
                .build();
        
        assertNotNull(room);
        assertEquals("test-room-1", room.getRoomId());
        assertEquals("Test Room", room.getName());
        assertTrue(room.isEmpty());
        assertEquals(0, room.getSessionCount());
    }

    @Test
    void testChatRoomRepository() {
        assertEquals(0, repository.getTotalRoomCount());
        
        ChatRoom room1 = repository.createChatRoom("Room 1");
        assertNotNull(room1);
        assertEquals(1, repository.getTotalRoomCount());
        
        assertTrue(repository.findRoomById(room1.getRoomId()).isPresent());
        assertTrue(repository.existsById(room1.getRoomId()));
        
        ChatRoom room2 = repository.getOrCreateRoom("custom-id", "Custom Room");
        assertEquals("custom-id", room2.getRoomId());
        assertEquals("Custom Room", room2.getName());
        assertEquals(2, repository.getTotalRoomCount());
        
        repository.deleteRoom(room1.getRoomId());
        assertEquals(1, repository.getTotalRoomCount());
        assertFalse(repository.existsById(room1.getRoomId()));
        
        repository.clear();
        assertEquals(0, repository.getTotalRoomCount());
    }

    @Test
    void testMessageTypeEnum() {
        assertEquals(3, MessageType.values().length);
        assertEquals(MessageType.ENTER, MessageType.valueOf("ENTER"));
        assertEquals(MessageType.TALK, MessageType.valueOf("TALK"));
        assertEquals(MessageType.QUIT, MessageType.valueOf("QUIT"));
    }
}