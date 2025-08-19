package com.ezlevup.chatsocket.controller;

import com.ezlevup.chatsocket.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @GetMapping("/rooms")
    public ResponseEntity<RoomListResponse> getChatRooms() {
        try {
            List<RoomInfo> rooms = chatRoomRepository.findAllRooms()
                    .stream()
                    .map(RoomInfo::fromChatRoom)
                    .collect(Collectors.toList());
            
            RoomListResponse response = new RoomListResponse(rooms);
            logger.info("채팅방 목록 조회: {}개", rooms.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("채팅방 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/room")
    public ResponseEntity<RoomInfo> createChatRoom(@RequestBody CreateRoomRequest request) {
        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                logger.warn("채팅방 생성 실패: 방 이름이 비어있음");
                return ResponseEntity.badRequest().build();
            }
            
            ChatRoom newRoom = chatRoomRepository.createChatRoom(request.getName().trim());
            RoomInfo roomInfo = RoomInfo.fromChatRoom(newRoom);
            
            logger.info("새 채팅방 생성: {} (ID: {})", newRoom.getName(), newRoom.getRoomId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(roomInfo);
        } catch (Exception e) {
            logger.error("채팅방 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<RoomInfo> getChatRoom(@PathVariable String roomId) {
        try {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findRoomById(roomId);
            
            if (roomOpt.isEmpty()) {
                logger.warn("존재하지 않는 채팅방 조회: {}", roomId);
                return ResponseEntity.notFound().build();
            }
            
            ChatRoom room = roomOpt.get();
            RoomInfo roomInfo = RoomInfo.fromChatRoom(room);
            
            logger.info("채팅방 조회: {} (사용자 수: {})", room.getName(), room.getSessionCount());
            
            return ResponseEntity.ok(roomInfo);
        } catch (Exception e) {
            logger.error("채팅방 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable String roomId) {
        try {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findRoomById(roomId);
            
            if (roomOpt.isEmpty()) {
                logger.warn("존재하지 않는 채팅방 삭제 시도: {}", roomId);
                return ResponseEntity.notFound().build();
            }
            
            chatRoomRepository.deleteRoom(roomId);
            logger.info("채팅방 삭제: {}", roomId);
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("채팅방 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}