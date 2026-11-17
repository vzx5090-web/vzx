package com.apexai.mobile;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class MemoryStore extends SQLiteOpenHelper {
    private static final String DB = "apex_memory.db";
    private static final int VERSION = 1;

    MemoryStore(Context c) { super(c, DB, null, VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE memories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_id TEXT NOT NULL," +
                "kind TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "source_url TEXT," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX memories_project_idx ON memories(project_id,id DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    synchronized long remember(String projectId, String kind, String title, String content, String url) {
        if (content == null || content.trim().isEmpty()) return -1;
        ContentValues v = new ContentValues();
        v.put("project_id", projectId == null ? "global" : projectId);
        v.put("kind", kind == null ? "note" : kind);
        v.put("title", title == null ? "معلومة محفوظة" : title);
        v.put("content", content);
        v.put("source_url", url == null ? "" : url);
        v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("memories", null, v);
    }

    synchronized String recall(String projectId, int limit, String query) {
        java.util.Set<String> terms = new java.util.HashSet<>();
        if (query != null) {
            for (String token : query.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
                if (token.length() >= 3) terms.add(token);
            }
        }
        class Row {
            String project, kind, title, content, url;
            long created;
            int score;
        }
        java.util.List<Row> rows = new java.util.ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT project_id,kind,title,content,source_url,created_at FROM memories ORDER BY id DESC LIMIT 400", null);
        try {
            while (c.moveToNext()) {
                Row row = new Row();
                row.project=c.getString(0); row.kind=c.getString(1); row.title=c.getString(2);
                row.content=c.getString(3); row.url=c.getString(4); row.created=c.getLong(5);
                if (row.project.equals(projectId) || row.project.equals("global")) row.score += 4;
                String hay=(row.title+" "+row.content).toLowerCase();
                for (String term : terms) if (hay.contains(term)) row.score += 2;
                if (row.score > 0) rows.add(row);
            }
        } finally { c.close(); }
        java.util.Collections.sort(rows, (a,b) -> {
            int score = Integer.compare(b.score, a.score);
            return score != 0 ? score : Long.compare(b.created, a.created);
        });
        StringBuilder out = new StringBuilder();
        int count=0;
        for (Row row : rows) {
            if (count++ >= Math.max(1, Math.min(18, limit))) break;
            out.append("\n[ذاكرة / ").append(row.kind).append("] ").append(row.title)
                    .append("\n").append(row.content).append("\n");
            if (row.url != null && !row.url.isEmpty()) out.append("المصدر: ").append(row.url).append("\n");
        }
        return out.toString();
    }

    synchronized List<JSONObject> recent(int limit) {
        List<JSONObject> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT project_id,kind,title,content,source_url,created_at FROM memories ORDER BY id DESC LIMIT ?",
                new String[]{String.valueOf(Math.max(1, Math.min(100, limit)))});
        try {
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("project_id", c.getString(0));
                o.put("kind", c.getString(1));
                o.put("title", c.getString(2));
                o.put("content", c.getString(3));
                o.put("source_url", c.getString(4));
                o.put("created_at", c.getLong(5));
                result.add(o);
            }
        } catch (Exception ignored) {
        } finally { c.close(); }
        return result;
    }

    synchronized int count() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM memories", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; }
        finally { c.close(); }
    }
}
