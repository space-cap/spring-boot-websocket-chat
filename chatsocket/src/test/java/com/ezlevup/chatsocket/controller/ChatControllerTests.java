package com.ezlevup.chatsocket.controller;

import com.ezlevup.chatsocket.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ChatRoom testRoom1;
    private ChatRoom testRoom2;

    @BeforeEach
    void setUp() {
        testRoom1 = ChatRoom.builder()
                .roomId("room1")
                .name("Test Room 1")
                .build();
        
        testRoom2 = ChatRoom.builder()
                .roomId("room2")
                .name("Test Room 2")
                .build();
    }

    @Test
    void testGetChatRooms() throws Exception {
        when(chatRoomRepository.findAllRooms()).thenReturn(Arrays.asList(testRoom1, testRoom2));

        mockMvc.perform(get("/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms.length()").value(2))
                .andExpect(jsonPath("$.rooms[0].roomId").value("room1"))
                .andExpect(jsonPath("$.rooms[0].name").value("Test Room 1"))
                .andExpect(jsonPath("$.rooms[0].userCount").value(0));
    }

    @Test
    void testCreateChatRoom() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("New Room");
        
        when(chatRoomRepository.createChatRoom("New Room")).thenReturn(testRoom1);

        mockMvc.perform(post("/chat/room")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.roomId").value("room1"))
                .andExpect(jsonPath("$.name").value("Test Room 1"));
    }

    @Test
    void testCreateChatRoomWithEmptyName() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("");

        mockMvc.perform(post("/chat/room")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetChatRoom() throws Exception {
        when(chatRoomRepository.findRoomById("room1")).thenReturn(Optional.of(testRoom1));

        mockMvc.perform(get("/chat/room/room1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.roomId").value("room1"))
                .andExpect(jsonPath("$.name").value("Test Room 1"));
    }

    @Test
    void testGetNonExistentChatRoom() throws Exception {
        when(chatRoomRepository.findRoomById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/chat/room/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteChatRoom() throws Exception {
        when(chatRoomRepository.findRoomById("room1")).thenReturn(Optional.of(testRoom1));

        mockMvc.perform(delete("/chat/room/room1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteNonExistentChatRoom() throws Exception {
        when(chatRoomRepository.findRoomById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/chat/room/nonexistent"))
                .andExpect(status().isNotFound());
    }
}