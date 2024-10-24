package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String senderUid;
    private String senderPfpUrl;
    private String senderDisplayName;
    private String messageText;
    private String messageType;
    private String analysisId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ChatMessage() {}

    public ChatMessage(String senderUid, String senderPfpUrl, String senderDisplayName, String messageText, String messageType, String analysisId, Timestamp createdAt, Timestamp updatedAt) {
        this.senderUid = senderUid;
        this.senderPfpUrl = senderPfpUrl;
        this.senderDisplayName = senderDisplayName;
        this.messageText = messageText;
        this.messageType = messageType;
        this.analysisId = analysisId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public String getSenderPfpUrl() {
        return senderPfpUrl;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public void setSenderPfpUrl(String senderPfpUrl) {
        this.senderPfpUrl = senderPfpUrl;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "senderUid='" + senderUid + '\'' +
                ", senderPfpUrl='" + senderPfpUrl + '\'' +
                ", senderDisplayName='" + senderDisplayName + '\'' +
                ", messageText='" + messageText + '\'' +
                ", messageType='" + messageType + '\'' +
                ", analysisId='" + analysisId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
