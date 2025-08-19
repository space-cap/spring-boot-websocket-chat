package com.ezlevup.chatsocket.service;

import com.ezlevup.chatsocket.model.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ChatRoomCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatRoomCleanupService.class);
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupEmptyRooms() {
        try {
            int beforeCount = chatRoomRepository.getTotalRoomCount();
            chatRoomRepository.deleteEmptyRooms();
            int afterCount = chatRoomRepository.getTotalRoomCount();
            
            int deletedCount = beforeCount - afterCount;
            if (deletedCount > 0) {
                logger.info("정리된 빈 채팅방 수: {}개", deletedCount);
            }
        } catch (Exception e) {
            logger.error("채팅방 정리 작업 실패: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void logChatRoomStatistics() {
        try {
            int totalRooms = chatRoomRepository.getTotalRoomCount();
            int totalSessions = chatRoomRepository.findAllRooms()
                    .stream()
                    .mapToInt(room -> room.getSessionCount())
                    .sum();
            
            logger.debug("채팅방 통계 - 총 방 수: {}, 총 접속자 수: {}", totalRooms, totalSessions);
        } catch (Exception e) {
            logger.error("채팅방 통계 수집 실패: {}", e.getMessage());
        }
    }
}