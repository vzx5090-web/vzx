package com.apexai.mobile;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    private static long lastRequestAt = 0L;
    static String chat(Context context, String system, String user, int maxTokens) throws Exception {
        waitForProviderWindow(context);
        String base = AppSettings.base(context).replaceAll("/+$", "");
        if (!base.startsWith("https://")) throw new Exception("عنوان API يجب أن يبدأ بـ HTTPS لأمان المفتاح.");
        String endpoint = base.endsWith("/chat/completions") ? base : base + "/chat/completions";
        String model = AppSettings.model(context);
        String token = SecureStore.readApiKey(context);

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", maxTokens);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", system));
        messages.put(new JSONObject().put("role", "user").put("content", user));
        payload.put("messages", messages);

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(endpoint).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(45000);
            c.setReadTimeout(240000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("Accept", "application/json");
            if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
            byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }

            int status = c.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
            String body = readAll(stream);
            if (status < 200 || status >= 300) {
                String hint = status == 401 || status == 403 ? "تحقق من مفتاح API وصلاحياته."
                        : status == 429 ? "تم تجاوز حد الطلبات؛ انتظر قليلاً أو استخدم مفتاح مزودك."
                        : status == 404 ? "عنوان API أو اسم النموذج غير صحيح."
                        : "راجع عنوان API والنموذج وإعدادات المزود.";
                throw new Exception("HTTP " + status + ": " + hint + "\\n" + body.substring(0, Math.min(700, body.length())));
            }
            JSONObject response = new JSONObject(body);
            JSONArray choices = response.getJSONArray("choices");
            String content = choices.getJSONObject(0).getJSONObject("message").optString("content", "");
            if (content.trim().isEmpty()) throw new Exception("النموذج أعاد إجابة فارغة.");
            return content.trim();
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static void waitForProviderWindow(Context context) throws InterruptedException {
        boolean anonymous = SecureStore.readApiKey(context).isEmpty();
        long minimumGap = anonymous ? 31000L : 1200L;
        synchronized (ApiClient.class) {
            long wait = lastRequestAt + minimumGap - System.currentTimeMillis();
            if (wait > 0) Thread.sleep(wait);
            lastRequestAt = System.currentTimeMillis();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            return output.toString("UTF-8");
        }
    }
    private ApiClient() {}
}
