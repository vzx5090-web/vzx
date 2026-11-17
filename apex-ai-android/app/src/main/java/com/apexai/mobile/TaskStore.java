package com.apexai.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TaskStore {
    private static final String PREFS = "apex_ai_tasks";
    private static final String IDS = "task_ids";
    private static final Object LOCK = new Object();

    static void save(Context context, JSONObject task) {
        synchronized (LOCK) {
            SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String id = task.optString("id");
            if (id.isEmpty()) return;
            JSONArray ids;
            try { ids = new JSONArray(p.getString(IDS, "[]")); }
            catch (JSONException e) { ids = new JSONArray(); }
            List<String> all = new ArrayList<>();
            for (int i = 0; i < ids.length(); i++) {
                String value = ids.optString(i);
                if (!value.isEmpty() && !value.equals(id)) all.add(value);
            }
            all.add(0, id);
            SharedPreferences.Editor edit = p.edit().putString("task_" + id, task.toString());
            JSONArray newIds = new JSONArray();
            for (int i = 0; i < Math.min(25, all.size()); i++) {
                newIds.put(all.get(i));
            }
            edit.putString(IDS, newIds.toString());
            for (int i = 25; i < all.size(); i++) edit.remove("task_" + all.get(i));
            edit.apply();
        }
    }

    static JSONObject load(Context context, String id) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("task_" + id, null);
        if (raw == null) return null;
        try { return new JSONObject(raw); }
        catch (JSONException e) { return null; }
    }

    static List<JSONObject> all(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<JSONObject> result = new ArrayList<>();
        try {
            JSONArray ids = new JSONArray(p.getString(IDS, "[]"));
            for (int i = 0; i < ids.length(); i++) {
                JSONObject item = load(context, ids.optString(i));
                if (item != null) result.add(item);
            }
        } catch (JSONException ignored) {}
        return result;
    }

    static JSONObject create(String id, String title, String description, int rounds) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("title", title);
            o.put("description", description);
            o.put("rounds", rounds);
            o.put("status", "queued");
            o.put("stage", "في قائمة الانتظار");
            o.put("progress", 0);
            o.put("plan", "");
            o.put("implementation", "");
            o.put("review", "");
            o.put("created_at", System.currentTimeMillis());
        } catch (JSONException ignored) {}
        return o;
    }

    private TaskStore() {}
}
