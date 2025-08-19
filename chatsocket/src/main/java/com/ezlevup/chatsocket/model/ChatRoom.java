package com.ezlevup.chatsocket.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
    
    private String roomId;
    private String name;
    private Set<WebSocketSession> sessions;

    private ChatRoom(Builder builder) {
        this.roomId = builder.roomId;
        this.name = builder.name;
        this.sessions = Collections.synchronizedSet(ConcurrentHashMap.newKeySet());
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public Set<WebSocketSession> getSessions() {
        return Collections.unmodifiableSet(sessions);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return Objects.equals(roomId, chatRoom.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "roomId='" + roomId + '\'' +
                ", name='" + name + '\'' +
                ", sessionCount=" + sessions.size() +
                '}';
    }

    public static class Builder {
        private String roomId;
        private String name;

        public Builder roomId(String roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public ChatRoom build() {
            Objects.requireNonNull(roomId, "Room ID cannot be null");
            Objects.requireNonNull(name, "Room name cannot be null");
            return new ChatRoom(this);
        }
    }
}