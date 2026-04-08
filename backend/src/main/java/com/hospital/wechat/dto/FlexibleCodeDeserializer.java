package com.hospital.wechat.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 登录验证码字段兼容 JSON 字符串与数字，避免 wx.request 序列化差异导致取不到值。
 */
public class FlexibleCodeDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == null) {
            return null;
        }
        switch (t) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return p.getNumberValue().toString();
            case VALUE_STRING:
                return p.getText();
            case VALUE_NULL:
                return null;
            default:
                return p.getValueAsString();
        }
    }
}
