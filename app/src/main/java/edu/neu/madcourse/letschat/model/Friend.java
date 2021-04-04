package edu.neu.madcourse.letschat.model;

public class Friend {
    private String idChatRoom;

    public Friend(String idChatRoom) {
        this.idChatRoom = idChatRoom;
    }

    public Friend() {
    }

    public String getIdChatRoom() {
        return idChatRoom;
    }

    public void setIdChatRoom(String idChatRoom) {
        this.idChatRoom = idChatRoom;
    }
}
