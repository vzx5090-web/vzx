package com.studiomobile.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int REQUEST_OPEN = 7101;
    private static final int REQUEST_SAVE = 7102;
    private static final String GEMINI_KEY_ALIAS = "studio.mobile.gemini.key.v1";
    private static final String GEMINI_PREFS = "studio_mobile_secure";
    private static final String GEMINI_KEY_VALUE = "encrypted_gemini_key";

    private WebView webView;
    private ValueCallback<Uri[]> chooserCallback;
    private String pendingExport = "";
    private String pendingMime = "application/json";
    private String compatPrelude = "";
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService geminiExecutor = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff111720);
        getWindow().setNavigationBarColor(0xff0d1118);

        try (InputStream in = getAssets().open("roblox_compat.luau");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            compatPrelude = out.toString("UTF-8");
        } catch (Exception error) {
            compatPrelude = "-- compatibility prelude unavailable\n";
        }

        webView = new WebView(this);
        webView.setBackgroundColor(0xff0d1118);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSafeBrowsingEnabled(true);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (chooserCallback != null) chooserCallback.onReceiveValue(null);
                chooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_OPEN);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !(url.startsWith("file:///android_asset/") || url.equals("about:blank"));
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
    }

    public final class AndroidBridge {
        @JavascriptInterface public boolean isLuauAvailable() {
            return NativeLuau.isAvailable();
        }

        @JavascriptInterface public void runLuauAsync(String source, String requestId) {
            final String id = requestId == null ? "" : requestId;
            if (!running.compareAndSet(false, true)) {
                publishResult(id, "BUSY: another script is running.");
                return;
            }
            scriptExecutor.execute(() -> {
                String result;
                try {
                    result = NativeLuau.run(compatPrelude, source == null ? "" : source);
                } catch (Throwable error) {
                    result = "RUNTIME FAILURE: " + error.getClass().getSimpleName() + ": " + error.getMessage();
                } finally {
                    running.set(false);
                }
                publishResult(id, result);
            });
        }

        @JavascriptInterface public boolean hasGeminiApiKey() {
            return !readGeminiApiKey().isEmpty();
        }

        @JavascriptInterface public boolean saveGeminiApiKey(String apiKey) {
            try {
                String key = apiKey == null ? "" : apiKey.trim();
                if (key.isEmpty() || key.length() > 512) return false;
                writeGeminiApiKey(key);
                return true;
            } catch (Exception error) {
                return false;
            }
        }

        @JavascriptInterface public boolean clearGeminiApiKey() {
            try {
                getSharedPreferences(GEMINI_PREFS, MODE_PRIVATE).edit().remove(GEMINI_KEY_VALUE).apply();
                KeyStore store = KeyStore.getInstance("AndroidKeyStore");
                store.load(null);
                if (store.containsAlias(GEMINI_KEY_ALIAS)) store.deleteEntry(GEMINI_KEY_ALIAS);
                return true;
            } catch (Exception error) {
                return false;
            }
        }

        @JavascriptInterface public void askGemini(String prompt, String projectJson, String screenshotBase64, String requestId) {
            final String id = requestId == null ? "" : requestId;
            final String userPrompt = prompt == null ? "" : prompt;
            final String project = projectJson == null ? "" : projectJson;
            final String image = screenshotBase64 == null ? "" : screenshotBase64;
            geminiExecutor.execute(() -> {
                String result;
                try {
                    result = requestGemini(userPrompt, project, image);
                } catch (Exception error) {
                    try {
                        result = new JSONObject().put("ok", false)
                                .put("error", "Gemini request failed: " + error.getMessage()).toString();
                    } catch (Exception ignored) {
                        result = "{\"ok\":false,\"error\":\"Gemini request failed\"}";
                    }
                }
                publishGeminiResult(id, result);
            });
        }

        @JavascriptInterface public void exportFile(String filename, String content, String mimeType) {
            runOnUiThread(() -> {
                pendingExport = content == null ? "" : content;
                pendingMime = mimeType == null ? "application/octet-stream" : mimeType;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(pendingMime);
                intent.putExtra(Intent.EXTRA_TITLE, filename == null ? "project.json" : filename);
                startActivityForResult(intent, REQUEST_SAVE);
            });
        }

        @JavascriptInterface public void notifyUser(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    message == null ? "Studio Mobile" : message, Toast.LENGTH_SHORT).show());
        }
    }

    private SecretKey getGeminiEncryptionKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(GEMINI_KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(GEMINI_KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                GEMINI_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }

    private void writeGeminiApiKey(String apiKey) throws Exception {
        SecretKey secret = getGeminiEncryptionKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
        String packed = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        getSharedPreferences(GEMINI_PREFS, MODE_PRIVATE).edit()
                .putString(GEMINI_KEY_VALUE, packed).apply();
    }

    private String readGeminiApiKey() {
        try {
            String packed = getSharedPreferences(GEMINI_PREFS, MODE_PRIVATE)
                    .getString(GEMINI_KEY_VALUE, "");
            if (packed == null || packed.isEmpty()) return "";
            String[] parts = packed.split("\\.", 2);
            if (parts.length != 2) return "";
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] ciphertext = Base64.decode(parts[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getGeminiEncryptionKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception error) {
            return "";
        }
    }

    private String requestGemini(String prompt, String projectJson, String screenshotBase64) throws Exception {
        String apiKey = readGeminiApiKey();
        if (apiKey.isEmpty()) {
            return new JSONObject().put("ok", false)
                    .put("error", "Gemini API key is missing. Open the Gemini API settings and save your key.")
                    .toString();
        }

        JSONObject inputText = new JSONObject()
                .put("type", "text")
                .put("text", prompt + "\n\nProject snapshot (JSON):\n" + projectJson);
        JSONArray input = new JSONArray().put(inputText);
        if (!screenshotBase64.isEmpty() && screenshotBase64.length() < 3_500_000) {
            input.put(new JSONObject()
                    .put("type", "image")
                    .put("data", screenshotBase64)
                    .put("mime_type", "image/jpeg"));
        }

        JSONObject schema = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("message", new JSONObject().put("type", "string"))
                        .put("luau", new JSONObject().put("type", "string"))
                        .put("apply", new JSONObject().put("type", "boolean")))
                .put("required", new JSONArray().put("message").put("luau").put("apply"));
        JSONObject request = new JSONObject()
                .put("model", "gemini-3.8-flash")
                .put("input", input)
                .put("store", false)
                .put("response_format", new JSONObject()
                        .put("type", "text")
                        .put("mime_type", "application/json")
                        .put("schema", schema));

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(
                    "https://generativelanguage.googleapis.com/v1beta/interactions").openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(120000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            connection.setRequestProperty("Api-Revision", "2026-05-20");
            byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String responseText;
            try (InputStream source = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (source == null) {
                    throw new IllegalStateException("Gemini returned an empty response (HTTP " + status + ")");
                }
                byte[] buffer = new byte[8192];
                int count;
                while ((count = source.read(buffer)) >= 0) {
                    if (output.size() + count > 4 * 1024 * 1024) {
                        throw new IllegalStateException("Gemini response is too large");
                    }
                    output.write(buffer, 0, count);
                }
                responseText = output.toString("UTF-8");
            }

            JSONObject response = new JSONObject(responseText);
            if (status < 200 || status >= 300) {
                JSONObject apiError = response.optJSONObject("error");
                String message = apiError == null ? ("HTTP " + status + ": " + responseText)
                        : apiError.optString("message", "HTTP " + status);
                return new JSONObject().put("ok", false).put("error", message).toString();
            }

            String outputText = response.optString("output_text", "");
            if (outputText.isEmpty()) {
                JSONArray steps = response.optJSONArray("steps");
                if (steps != null) {
                    StringBuilder combined = new StringBuilder();
                    for (int i = 0; i < steps.length(); i++) {
                        JSONObject step = steps.optJSONObject(i);
                        if (step == null || !"model_output".equals(step.optString("type"))) continue;
                        JSONArray content = step.optJSONArray("content");
                        if (content == null) continue;
                        for (int j = 0; j < content.length(); j++) {
                            JSONObject item = content.optJSONObject(j);
                            if (item != null && "text".equals(item.optString("type"))) {
                                combined.append(item.optString("text", ""));
                            }
                        }
                    }
                    outputText = combined.toString();
                }
            }
            if (outputText.trim().isEmpty()) {
                return new JSONObject().put("ok", false)
                        .put("error", "Gemini returned no text. Check model access and API usage limits.")
                        .toString();
            }
            return new JSONObject().put("ok", true).put("text", outputText).toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void publishGeminiResult(String id, String result) {
        final String js = "window.StudioMobile&&window.StudioMobile.onGeminiResult&&window.StudioMobile.onGeminiResult("
                + JSONObject.quote(id) + "," + JSONObject.quote(result == null ? "" : result) + ");";
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
    }

    private void publishResult(String id, String result) {
        final String js = "window.StudioMobile&&window.StudioMobile.onLuauResult&&window.StudioMobile.onLuauResult("
                + JSONObject.quote(id) + "," + JSONObject.quote(result == null ? "" : result) + ");";
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN) {
            Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                    ? new Uri[]{data.getData()} : null;
            if (chooserCallback != null) chooserCallback.onReceiveValue(result);
            chooserCallback = null;
            return;
        }
        if (requestCode == REQUEST_SAVE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new IllegalStateException("Output stream unavailable");
                    out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "Project saved", Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    Toast.makeText(this, "Save failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            pendingExport = "";
            pendingMime = "application/json";
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (chooserCallback != null) chooserCallback.onReceiveValue(null);
        scriptExecutor.shutdownNow();
        geminiExecutor.shutdownNow();
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
