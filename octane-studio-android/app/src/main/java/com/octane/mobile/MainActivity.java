package com.octane.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(13, 17, 24);
    private static final int PANEL = Color.rgb(24, 30, 40);
    private static final int TEXT = Color.rgb(242, 246, 250);
    private static final int MUTED = Color.rgb(157, 170, 187);
    private static final int ACCENT = Color.rgb(45, 190, 173);

    private LinearLayout root;
    private LinearLayout connectPanel;
    private LinearLayout webPanel;
    private EditText addressInput;
    private WebView webView;
    private String currentHost = "";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9, 13, 19));
        getWindow().setNavigationBarColor(Color.rgb(9, 13, 19));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        CookieManager.getInstance().setAcceptCookie(true);
        buildInterface();
    }

    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(16), dp(18), dp(12));
        setContentView(root);

        TextView brand = new TextView(this);
        brand.setText("OCTANE  /  MOBILE");
        brand.setTextColor(ACCENT);
        brand.setTextSize(12);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setLetterSpacing(0.12f);
        root.addView(brand, matchWrap());

        TextView title = new TextView(this);
        title.setText("تحكّم بالاستديو من هاتفك");
        title.setTextColor(TEXT);
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(12), 0, dp(4));
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("اتصال محلي مباشر بكمبيوترك عبر Wi-Fi");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle, matchWrap());

        connectPanel = new LinearLayout(this);
        connectPanel.setOrientation(LinearLayout.VERTICAL);
        connectPanel.setPadding(dp(15), dp(16), dp(15), dp(16));
        connectPanel.setBackground(roundRect(PANEL, dp(18)));
        root.addView(connectPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView addressLabel = new TextView(this);
        addressLabel.setText("عنوان الكمبيوتر");
        addressLabel.setTextColor(TEXT);
        addressLabel.setTextSize(14);
        addressLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        connectPanel.addView(addressLabel, matchWrap());

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setTextSize(15);
        addressInput.setTextColor(TEXT);
        addressInput.setHintTextColor(MUTED);
        addressInput.setHint("http://192.168.1.15:8765");
        addressInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressInput.setSelectAllOnFocus(false);
        addressInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        addressInput.setBackground(roundRect(Color.rgb(11, 15, 22), dp(11)));
        String saved = getPreferences(MODE_PRIVATE).getString("pc_address", "");
        if (!saved.isEmpty()) addressInput.setText(saved);
        LinearLayout.LayoutParams fieldParams = matchWidthWrap();
        fieldParams.topMargin = dp(9);
        connectPanel.addView(addressInput, fieldParams);

        Button connect = makeButton("اتصال بالكمبيوتر");
        LinearLayout.LayoutParams buttonParams = matchWidthWrap();
        buttonParams.topMargin = dp(13);
        connectPanel.addView(connect, buttonParams);
        connect.setOnClickListener(v -> connectToPc());

        TextView instructions = new TextView(this);
        instructions.setText(
                "1. شغّل Octane Mobile Bridge على Windows.\n"
                + "2. تأكد أن الهاتف والكمبيوتر على شبكة Wi-Fi نفسها.\n"
                + "3. انسخ العنوان الظاهر في نافذة الكمبيوتر أعلاه.\n"
                + "4. بعد الاتصال، أدخل PIN الذي يعرضه الجسر."
        );
        instructions.setTextColor(MUTED);
        instructions.setTextSize(13);
        instructions.setLineSpacing(dp(5), 1f);
        instructions.setPadding(dp(2), dp(16), dp(2), dp(2));
        root.addView(instructions, matchWrap());

        TextView scope = new TextView(this);
        scope.setText("هذا التطبيق واجهة تحكم للهاتف؛ تشغيل Roblox / Octane Studio يبقى على الكمبيوتر.");
        scope.setTextColor(Color.rgb(224, 183, 103));
        scope.setTextSize(12);
        scope.setLineSpacing(dp(3), 1f);
        scope.setPadding(dp(2), dp(18), dp(2), 0);
        root.addView(scope, matchWrap());

        webPanel = new LinearLayout(this);
        webPanel.setOrientation(LinearLayout.VERTICAL);
        webPanel.setVisibility(View.GONE);
        root.addView(webPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button back = makeButton("رجوع إلى شاشة الاتصال");
        LinearLayout.LayoutParams backParams = matchWidthWrap();
        backParams.bottomMargin = dp(8);
        webPanel.addView(back, backParams);
        back.setOnClickListener(v -> showConnectionPanel());

        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " OctaneStudioMobile/1.0");
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                return host == null || !host.equalsIgnoreCase(currentHost);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this,
                            "تعذّر فتح الجسر. افحص العنوان وشبكة Wi-Fi.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        webPanel.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private void connectToPc() {
        String raw = addressInput.getText().toString().trim();
        if (raw.isEmpty()) {
            addressInput.setError("أدخل عنوان الكمبيوتر أولاً");
            return;
        }
        if (!raw.contains("://")) raw = "http://" + raw;
        Uri uri = Uri.parse(raw);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 8765 : uri.getPort();

        if (!"http".equalsIgnoreCase(scheme) || host == null
                || uri.getUserInfo() != null || port < 1 || port > 65535
                || !isLocalHost(host)) {
            addressInput.setError("استخدم عنواناً محلياً مثل http://192.168.1.15:8765");
            return;
        }

        currentHost = host.toLowerCase(Locale.ROOT);
        String target = "http://" + host + ":" + port + "/";
        getPreferences(MODE_PRIVATE).edit().putString("pc_address", target).apply();
        showWebPanel();
        webView.loadUrl(target);
    }

    private boolean isLocalHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (h.endsWith(".local") && h.length() > 6 && !h.contains("..")) return true;
        String[] p = h.split("\\.", -1);
        if (p.length != 4) return false;
        int[] n = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                if (p[i].isEmpty() || p[i].length() > 3) return false;
                n[i] = Integer.parseInt(p[i]);
                if (n[i] < 0 || n[i] > 255) return false;
            }
        } catch (NumberFormatException ex) {
            return false;
        }
        return n[0] == 10
                || (n[0] == 192 && n[1] == 168)
                || (n[0] == 172 && n[1] >= 16 && n[1] <= 31);
    }

    private void showWebPanel() {
        connectPanel.setVisibility(View.GONE);
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView && i > 0) child.setVisibility(View.GONE);
        }
        webPanel.setVisibility(View.VISIBLE);
    }

    private void showConnectionPanel() {
        if (webView != null) webView.stopLoading();
        webPanel.setVisibility(View.GONE);
        connectPanel.setVisibility(View.VISIBLE);
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView) child.setVisibility(View.VISIBLE);
        }
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.rgb(5, 22, 21));
        button.setPadding(dp(12), dp(9), dp(12), dp(9));
        button.setBackground(roundRect(ACCENT, dp(12)));
        return button;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWidthWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webPanel != null && webPanel.getVisibility() == View.VISIBLE) {
            if (webView != null && webView.canGoBack()) webView.goBack();
            else showConnectionPanel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        super.onDestroy();
    }
}
