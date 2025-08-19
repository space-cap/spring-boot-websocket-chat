package com.ezlevup.chatsocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateRoomRequest {
    
    @JsonProperty("name")
    private String name;

    public CreateRoomRequest() {}

    public CreateRoomRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CreateRoomRequest{" +
                "name='" + name + '\'' +
                '}';
    }
}