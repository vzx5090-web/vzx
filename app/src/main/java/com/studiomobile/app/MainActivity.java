package com.studiomobile.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int REQUEST_OPEN = 7101;
    private static final int REQUEST_SAVE = 7102;

    private WebView webView;
    private ValueCallback<Uri[]> chooserCallback;
    private String pendingExport = "";
    private String pendingMime = "application/json";
    private String compatPrelude = "";
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService scriptExecutor = Executors.newSingleThreadExecutor();

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
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
