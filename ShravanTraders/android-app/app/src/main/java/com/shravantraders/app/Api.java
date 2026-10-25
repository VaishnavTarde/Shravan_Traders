package com.shravantraders.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class Api {

    // Primary USB reverse port forwarding URL
    private static final String PRIMARY_API_URL = "http://127.0.0.1:5000";
    // Fallback Android Emulator host URL
    private static final String FALLBACK_API_URL = "http://10.0.2.2:5000";

    private Api() {
        // Prevent instantiation
    }

    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE);
        return prefs.getString("custom_api_url", PRIMARY_API_URL);
    }

    public static void setBaseUrl(Context context, String url) {
        context.getSharedPreferences("session", Context.MODE_PRIVATE)
                .edit()
                .putString("custom_api_url", url)
                .apply();
    }

    public static String token(Context context) {
        return context
                .getSharedPreferences("session", Context.MODE_PRIVATE)
                .getString("token", "");
    }

    public static String role(Context context) {
        return context
                .getSharedPreferences("session", Context.MODE_PRIVATE)
                .getString("role", "customer");
    }

    public static String userName(Context context) {
        return context
                .getSharedPreferences("session", Context.MODE_PRIVATE)
                .getString("name", "User");
    }

    public static void saveSession(
            Context context,
            String token,
            String role,
            String name
    ) {
        SharedPreferences.Editor editor = context
                .getSharedPreferences("session", Context.MODE_PRIVATE)
                .edit();

        editor.putString("token", token);
        editor.putString("role", role);
        editor.putString("name", name);
        editor.apply();
    }

    public static void clearSession(Context context) {
        context.getSharedPreferences("session", Context.MODE_PRIVATE)
                .edit()
                .remove("token")
                .remove("role")
                .remove("name")
                .apply();
    }

    public static JSONObject request(
            Context context,
            String method,
            String path,
            JSONObject body
    ) throws Exception {

        String baseUrl = getBaseUrl(context);
        try {
            return executeHttpRequest(context, baseUrl + path, method, body);
        } catch (IOException e) {
            // If primary localhost URL fails and no custom URL was set, try fallback emulator URL
            if (PRIMARY_API_URL.equals(baseUrl)) {
                try {
                    return executeHttpRequest(context, FALLBACK_API_URL + path, method, body);
                } catch (Exception ignored) {}
            }
            throw e;
        }
    }

    private static JSONObject executeHttpRequest(
            Context context,
            String fullUrl,
            String method,
            JSONObject body
    ) throws Exception {

        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");

        String savedToken = token(context);
        if (!savedToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + savedToken);
        }

        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }

        int responseCode = connection.getResponseCode();
        InputStream inputStream;

        if (responseCode >= 200 && responseCode < 400) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (inputStream != null) {
            try (InputStream stream = inputStream) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
        }

        String responseBody = output.toString("UTF-8");
        connection.disconnect();

        JSONObject result;
        if (responseBody.trim().isEmpty()) {
            result = new JSONObject();
        } else {
            try {
                result = new JSONObject(responseBody);
            } catch (Exception e) {
                throw new IOException("Server error (" + responseCode + "): " + responseBody);
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException(result.optString("message", "Request failed (" + responseCode + ")"));
        }

        return result;
    }
}
