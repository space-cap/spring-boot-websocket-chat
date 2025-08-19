package com.ezlevup.chatsocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomInfo {
    
    @JsonProperty("roomId")
    private String roomId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("userCount")
    private int userCount;

    public RoomInfo() {}

    public RoomInfo(String roomId, String name, int userCount) {
        this.roomId = roomId;
        this.name = name;
        this.userCount = userCount;
    }

    public static RoomInfo fromChatRoom(ChatRoom chatRoom) {
        return new RoomInfo(
                chatRoom.getRoomId(),
                chatRoom.getName(),
                chatRoom.getSessionCount()
        );
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    @Override
    public String toString() {
        return "RoomInfo{" +
                "roomId='" + roomId + '\'' +
                ", name='" + name + '\'' +
                ", userCount=" + userCount +
                '}';
    }
}