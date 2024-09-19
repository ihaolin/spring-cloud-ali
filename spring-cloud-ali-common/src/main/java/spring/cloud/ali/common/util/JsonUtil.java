package spring.cloud.ali.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

public class JsonUtil {

    // Create a single ObjectMapper instance to use across the application
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Configure ObjectMapper (optional)
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Serialize an object to JSON string.
     * 
     * @param obj The object to serialize.
     * @return JSON string representation of the object.
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a JsonNode object to a formatted JSON string.
     *
     * @param jsonNode The JsonNode to convert.
     * @return The JSON string representation of the JsonNode.
     */
    public static String toJson(JsonNode jsonNode)  {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Deserialize a JSON string to an object of the specified type.
     * 
     * @param json The JSON string to deserialize.
     * @param valueType The class of the type to deserialize to.
     * @param <T> The type of the object.
     * @return The deserialized object.
     */
    public static <T> T toObject(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserialize a JSON string to a list of objects of the specified type.
     *
     * @param json The JSON string to deserialize.
     * @param typeReference The type reference for the list of objects.
     * @param <T> The type of the objects in the list.
     * @return The deserialized list of objects.
     */
    public static <T> List<T> toList(String json, TypeReference<List<T>> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserialize a JSON string to an object of the specified type with a generic type parameter.
     * 
     * @param json The JSON string to deserialize.
     * @param typeReference The type reference for the object.
     * @param <T> The type of the object.
     * @return The deserialized object.
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a JSON string to a JsonNode object.
     * 
     * @param json The JSON string to convert.
     * @return The JsonNode representation of the JSON string.
     */
    public static JsonNode toJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if a JSON string is valid.
     * 
     * @param json The JSON string to check.
     * @return True if the JSON string is valid, otherwise false.
     */
    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
