package com.blackjack.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils {
    private static final Gson gson = new GsonBuilder().create();

    /**
     * Converteste obiectul in sir JSON
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Parseaza sirul JSON in JsonObject
     */
    public static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * Creaza un raspuns de succes simplu
     */
    public static String successResponse(String type, String message, String data) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("success", true);
        response.addProperty("message", message);
        if (data != null) {
            response.add("data", JsonParser.parseString(data));
        }
        return gson.toJson(response);
    }

    /**
     * Creaza un raspuns de eroare simplu
     */
    public static String errorResponse(String message, String code) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "ERROR");
        response.addProperty("success", false);
        response.addProperty("message", message);
        if (code != null) {
            response.addProperty("code", code);
        }
        return gson.toJson(response);
    }

    /**
     * Creaza un mesaj cu tip si date
     */
    public static String createMessage(String type, String data) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        if (data != null) {
            message.add("data", JsonParser.parseString(data));
        }
        return gson.toJson(message);
    }
}
