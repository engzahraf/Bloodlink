package com.bloodlink.protocol;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delimiter-safe key-value payload container for BloodLink protocol packets.
 * Keys and values are URL-encoded during serialization to prevent injection of
 * delimiters (pipes, ampersands, equals, newlines, colons).
 */
public class ProtocolPayload {

    private final Map<String, String> data = new LinkedHashMap<>();

    public ProtocolPayload() {
    }

    public ProtocolPayload(Map<String, String> initial) {
        if (initial != null) {
            data.putAll(initial);
        }
    }

    public static ProtocolPayload empty() {
        return new ProtocolPayload();
    }

    public static ProtocolPayload of(String key, String value) {
        ProtocolPayload payload = new ProtocolPayload();
        payload.set(key, value);
        return payload;
    }

    public ProtocolPayload set(String key, String value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
        return this;
    }

    public ProtocolPayload set(String key, int value) {
        return set(key, Integer.toString(value));
    }

    public ProtocolPayload set(String key, long value) {
        return set(key, Long.toString(value));
    }

    public ProtocolPayload set(String key, boolean value) {
        return set(key, Boolean.toString(value));
    }

    public String get(String key) {
        return data.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String val = get(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Serializes a list of items into an encoded sub-string stored at the given key.
     */
    public ProtocolPayload setList(String key, List<String> items) {
        if (items == null || items.isEmpty()) {
            return set(key, "");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append("~");
            }
            sb.append(URLEncoder.encode(items.get(i), StandardCharsets.UTF_8));
        }
        return set(key, sb.toString());
    }

    /**
     * Retrieves and decodes a list of items stored by setList.
     */
    public List<String> getList(String key) {
        String raw = get(key);
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        String[] tokens = raw.split("~");
        for (String token : tokens) {
            result.add(URLDecoder.decode(token, StandardCharsets.UTF_8));
        }
        return result;
    }

    /**
     * Serializes the key-value pairs into a query-like URL-encoded string: key=val&key2=val2
     */
    public String serialize() {
        if (data.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            String encKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encVal = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            sb.append(encKey).append("=").append(encVal);
        }
        return sb.toString();
    }

    /**
     * Parses a serialized string into a ProtocolPayload instance safely.
     */
    public static ProtocolPayload parse(String serialized) {
        ProtocolPayload payload = new ProtocolPayload();
        if (serialized == null || serialized.trim().isEmpty()) {
            return payload;
        }
        String[] pairs = serialized.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String rawKey = pair.substring(0, idx);
                String rawVal = pair.substring(idx + 1);
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                String val = URLDecoder.decode(rawVal, StandardCharsets.UTF_8);
                payload.set(key, val);
            } else if (idx == -1) {
                String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                payload.set(key, "");
            }
        }
        return payload;
    }

    @Override
    public String toString() {
        return serialize();
    }
}
