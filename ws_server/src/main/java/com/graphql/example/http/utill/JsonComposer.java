package com.graphql.example.http.utill;

import java.util.Map;

public final class JsonComposer {

    public static String compose(Object object) {
        if (object instanceof Map) {
            final Map<Object, Object> map = (Map<Object, Object>) object;
            final StringBuilder sb = new StringBuilder("{");
            boolean includeComma = false;
            for (Map.Entry entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    if (includeComma) {
                        sb.append(',');
                    }

                    sb.append('"').append(entry.getKey()).append("\":").append(compose(entry.getValue()));
                }
                includeComma = true;
            }
            return sb.append('}').toString();
        }
        else if (object instanceof Iterable) {
            final Iterable iterable = (Iterable) object;
            final StringBuilder sb = new StringBuilder("[");
            boolean includeComma = false;
            for (Object value : iterable) {
                if (includeComma) {
                    sb.append(',');
                }

                sb.append(compose(value));
                includeComma = true;
            }
            return sb.append(']').toString();
        }
        else if (object instanceof Boolean) {
            return object.toString();
        }
        else if (object instanceof Number) {
            return object.toString();
        }
        else if (object instanceof String) {
            return "\"" + object + "\"";
        }
        else if (object == null) {
            return "null";
        }

        throw new IllegalArgumentException("object of type " + object.getClass().getName() + " cannot be converted");
    }
}
