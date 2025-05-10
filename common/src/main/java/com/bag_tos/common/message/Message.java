package com.bag_tos.common.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.HashMap;

/**
 * İstemci ve sunucu arasında gönderilen tüm mesajların temel sınıfı
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private MessageType type;
    private Map<String, Object> data = new HashMap<>();

    // Boş constructor (Jackson için gerekli)
    public Message() {
    }

    // Constructor
    public Message(MessageType type) {
        this.type = type;
    }

    // Constructor with data
    public Message(MessageType type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    // Getter ve Setter metodları
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    // Yardımcı metodlar
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }

    public Object getDataValue(String key) {
        return this.data.get(key);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}