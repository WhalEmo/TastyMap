package com.beem.TastyMap.rag.qdrant;

import io.qdrant.client.grpc.JsonWithInt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class QdrantPayloadConverter {

    public Map<String, Object> toPlainMap(Map<String, JsonWithInt.Value> payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> plainMap = new HashMap<>(payload.size());

        for (Map.Entry<String, JsonWithInt.Value> entry : payload.entrySet()) {
            plainMap.put(entry.getKey(), unpackValue(entry.getValue()));
        }
        return plainMap;
    }


    private Object unpackValue(JsonWithInt.Value value) {
        if (value == null) {
            return null;
        }

        return switch (value.getKindCase()){
            case NULL_VALUE -> null;
            case DOUBLE_VALUE -> value.getDoubleValue();
            case INTEGER_VALUE -> value.getIntegerValue();
            case STRING_VALUE -> value.getStringValue();
            case BOOL_VALUE -> value.getBoolValue();
            case STRUCT_VALUE -> toPlainMap(value.getStructValue().getFieldsMap());
            case LIST_VALUE -> value.getListValue().getValuesList()
                    .stream()
                    .map(this::unpackValue)
                    .toList();
            case KIND_NOT_SET -> null;
        };
    }

}
