package com.apexai.mobile;

import android.content.Context;
import android.content.SharedPreferences;

final class AppSettings {
    static final String DEFAULT_BASE = "https://generativelanguage.googleapis.com/v1beta/openai";
    static final String DEFAULT_MODEL = "gemini-3.8-flash";
    private static final String PREFS = "apex_settings";

    static String get(Context c, String key, String fallback) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, fallback);
    }
    static void save(Context c, String base, String model, String instructions, String searchProvider) throws Exception {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("base_url", clean(base, DEFAULT_BASE))
                .putString("model", clean(model, DEFAULT_MODEL))
                .putString("instructions", instructions == null ? "" : instructions.trim())
                .putString("search_provider", clean(searchProvider, "duckduckgo"))
                .apply();
    }
    static String base(Context c) {
        String value = get(c, "base_url", DEFAULT_BASE);
        if ("https://oai.endpoints.kepler.ai.cloud.ovh.net/v1".equals(value)) return DEFAULT_BASE;
        return value;
    }
    static String model(Context c) {
        String value = get(c, "model", DEFAULT_MODEL);
        if ("code_chat@latest".equals(value)) return DEFAULT_MODEL;
        return value;
    }
    static String instructions(Context c) { return get(c, "instructions", ""); }
    static String searchProvider(Context c) { return get(c, "search_provider", "duckduckgo"); }
    private static String clean(String s, String fallback) {
        if (s == null || s.trim().isEmpty()) return fallback;
        return s.trim().replaceAll("/+$", "");
    }
    private AppSettings() {}
}
