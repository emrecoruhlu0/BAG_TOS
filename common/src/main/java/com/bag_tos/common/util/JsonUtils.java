package com.bag_tos.common.util;

import com.bag_tos.common.message.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JSON serileştirme ve deserileştirme işlemleri için yardımcı sınıf
 */
public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    /**
     * Bir nesneyi JSON dizesine dönüştürür
     *
     * @param object Dönüştürülecek nesne
     * @return JSON dizesi
     */
    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * JSON dizesini belirtilen türdeki bir nesneye dönüştürür
     *
     * @param json  JSON dizesi
     * @param clazz Dönüştürülecek sınıf
     * @return Dönüştürülen nesne
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * JSON dizesini Message nesnesine dönüştürür
     *
     * @param json JSON dizesi
     * @return Message nesnesi
     */
    public static Message parseMessage(String json) {
        return fromJson(json, Message.class);
    }
}