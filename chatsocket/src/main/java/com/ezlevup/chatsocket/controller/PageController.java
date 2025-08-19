package com.ezlevup.chatsocket.controller;

import com.ezlevup.chatsocket.model.ChatRoom;
import com.ezlevup.chatsocket.model.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
public class PageController {
    
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("rooms", chatRoomRepository.findAllRooms());
            model.addAttribute("totalRooms", chatRoomRepository.getTotalRoomCount());
            
            logger.info("메인 페이지 접근 - 총 채팅방 수: {}", chatRoomRepository.getTotalRoomCount());
            
            return "index";
        } catch (Exception e) {
            logger.error("메인 페이지 로드 실패: {}", e.getMessage());
            model.addAttribute("error", "페이지를 불러오는데 실패했습니다.");
            return "error";
        }
    }

    @GetMapping("/room/{roomId}")
    public ModelAndView chatRoom(@PathVariable String roomId) {
        try {
            Optional<ChatRoom> roomOpt = chatRoomRepository.findRoomById(roomId);
            
            if (roomOpt.isEmpty()) {
                logger.warn("존재하지 않는 채팅방 페이지 접근: {}", roomId);
                ModelAndView mav = new ModelAndView("error");
                mav.addObject("error", "존재하지 않는 채팅방입니다.");
                mav.addObject("errorCode", "404");
                return mav;
            }
            
            ChatRoom room = roomOpt.get();
            
            ModelAndView mav = new ModelAndView("chatroom");
            mav.addObject("room", room);
            mav.addObject("roomId", roomId);
            mav.addObject("roomName", room.getName());
            mav.addObject("userCount", room.getSessionCount());
            
            logger.info("채팅방 페이지 접근: {} (사용자 수: {})", room.getName(), room.getSessionCount());
            
            return mav;
        } catch (Exception e) {
            logger.error("채팅방 페이지 로드 실패: {}", e.getMessage());
            ModelAndView mav = new ModelAndView("error");
            mav.addObject("error", "채팅방을 불러오는데 실패했습니다.");
            mav.addObject("errorCode", "500");
            return mav;
        }
    }

    @GetMapping("/create")
    public String createRoomPage() {
        logger.info("채팅방 생성 페이지 접근");
        return "create-room";
    }

    @GetMapping("/error")
    public String errorPage(Model model) {
        model.addAttribute("error", "오류가 발생했습니다.");
        return "error";
    }
    
    @GetMapping("/test")
    public String testPage() {
        logger.info("WebSocket 테스트 페이지 접근");
        return "test";
    }
}