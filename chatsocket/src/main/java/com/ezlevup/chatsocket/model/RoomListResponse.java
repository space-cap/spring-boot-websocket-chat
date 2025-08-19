package com.ezlevup.chatsocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RoomListResponse {
    
    @JsonProperty("rooms")
    private List<RoomInfo> rooms;

    public RoomListResponse() {}

    public RoomListResponse(List<RoomInfo> rooms) {
        this.rooms = rooms;
    }

    public List<RoomInfo> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomInfo> rooms) {
        this.rooms = rooms;
    }

    @Override
    public String toString() {
        return "RoomListResponse{" +
                "rooms=" + rooms +
                '}';
    }
}