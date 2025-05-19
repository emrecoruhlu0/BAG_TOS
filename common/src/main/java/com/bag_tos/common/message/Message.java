package com.bag_tos.common.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.HashMap;


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

    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message{type=").append(type);
        sb.append(", data=");

        if (data != null) {
            sb.append("{");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                sb.append(entry.getKey()).append("=");
                if (entry.getValue() != null) {
                    sb.append(entry.getValue().toString()).append(", ");
                } else {
                    sb.append("null, ");
                }
            }
            if (!data.isEmpty()) {
                sb.setLength(sb.length() - 2); // Son virgül ve boşluğu kaldır
            }
            sb.append("}");
        } else {
            sb.append("null");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}