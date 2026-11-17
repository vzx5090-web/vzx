package com.apexai.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AgentWorker extends Worker {
    private static final String API = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
    // OVHcloud virtual-model DSL: automatically resolves a current code-chat model.
    private static final String MODEL = "code_chat@latest";
    private static final long RATE_GAP_MS = 35_000L;
    private static final int MAX_RETRIES = 3;

    private final Context app;
    private JSONObject state;

    public AgentWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        app = context.getApplicationContext();
    }

    @NonNull @Override
    public Result doWork() {
        String id = getInputData().getString("id");
        String title = getInputData().getString("title");
        String description = getInputData().getString("description");
        int rounds = Math.max(1, Math.min(4, getInputData().getInt("rounds", 2)));
        if (id == null || title == null || description == null) return Result.failure();

        state = TaskStore.load(app, id);
        if (state == null) state = TaskStore.create(id, title, description, rounds);

        try {
            setForegroundAsync(foregroundInfo("APEX AI يعمل على مشروعك", "تحليل المتطلبات وإنشاء اللعبة")).get();
            put("status", "running");
            put("stage", "فهم الفكرة والتخطيط");
            put("progress", 4);
            save();

            String project = "عنوان المشروع: " + title + "\nالمنصة: Roblox / Luau\nالمتطلبات:\n" + description;
            String system = "أنت APEX AI، مهندس ألعاب أول ومتخصص Roblox Luau. اكتب بالعربية الواضحة، وأسماء الملفات والكود بالإنجليزية. كن دقيقاً وصريحاً ولا تدّع اختبار شيء لم يتم تشغيله. استخدم Roblox APIs المعروفة فقط. اجعل الخادم مسؤولاً عن التحقق من المال والضرر والمكافآت، ولا تثق بمدخلات العميل. صمم حلولاً قابلة للصيانة ومناسبة للهواتف.";

            String plan = ask(system, "ضع مخططاً هندسياً كاملاً للمشروع. أعطني: حلقة اللعب الأساسية، تقسيم الأنظمة، اعتمادياتها، شجرة ملفات Roblox، مراحل التنفيذ، معايير قبول قابلة للاختبار، والمخاطر. لا تكتب كل الكود بعد.\n\n" + project);
            put("plan", plan);
            put("stage", "بناء الملفات والأنظمة الأساسية");
            put("progress", 26);
            save();

            waitRateWindow();
            String code = ask(system, "اعتماداً على الخطة، أنشئ أول تنفيذ مترابط. أخرج شجرة الملفات ثم محتوى الملفات الأساسية مع المسار لكل ملف. اكتب Luau قابلاً للمراجعة. تحقق من كل طلب عميل على الخادم، وأضف إعدادات واختبارات/خطوات تحقق. لا تضع placeholders فارغة، ولا تدّع تشغيل الكود. إن كان الوصف ضخماً فابنِ نواة مترابطة وحدد بوضوح ما لم يكتمل.\n\nالخطة:\n" + plan + "\n\nالمتطلبات:\n" + project);
            put("implementation", code);
            put("stage", "المراجعة الأمنية والوظيفية");
            put("progress", 50);
            save();

            waitRateWindow();
            String review = ask(system, "راجع التنفيذ بصرامة كمراجع مستقل. اكتشف أخطاء Luau وRoblox API، الثغرات، الاعتماديات الناقصة، تعارض الأنظمة، مشاكل الأداء على Android، والنواقص مقابل المتطلبات. أعط قائمة إصلاحات مرتبة وتقييماً من 10. لا تدّع تشغيل الكود.\n\nالخطة:\n" + plan + "\n\nالتنفيذ:\n" + code);
            put("review", review);
            put("stage", "جولة تحسين ذاتية 1");
            put("progress", 64);
            save();

            for (int i = 2; i <= rounds; i++) {
                waitRateWindow();
                String revised = ask(system, "هذه مراجعة نقدية للتنفيذ السابق. أعد كتابة الأجزاء المتأثرة وقدّم نسخة محسّنة متماسكة، لا مجرد قائمة نصائح. احتفظ بالمسارات واذكر الملفات التي تغيرت. عالج الأولويات العالية أولاً. لا تدّع التشغيل.\n\nالخطة:\n" + plan + "\n\nالتنفيذ الحالي:\n" + code + "\n\nالمراجعة:\n" + review);
                code = revised;
                put("implementation", code);
                put("stage", "التحقق من جولة التحسين " + i);
                put("progress", 64 + (i - 1) * 12);
                save();

                waitRateWindow();
                review = ask(system, "افحص النسخة المحسنة بصورة مستقلة، وابحث عن أخطاء جديدة أو متبقية. أعط تقريراً قصيراً مرتباً، وحدد إن كانت النسخة جاهزة للمراجعة البشرية. لا تدّع تشغيلها.\n\nالنسخة المحسنة:\n" + code);
                put("review", review);
                put("stage", "اكتملت مراجعة جولة " + i);
                put("progress", Math.min(94, 72 + (i - 1) * 10));
                save();
            }

            String summary = "اكتملت دورة APEX AI.\n\nالمشروع: " + title
                    + "\nجولات التحسين: " + rounds
                    + "\nالنموذج: " + MODEL
                    + "\n\nالنتائج: راجع تبويبات الخطة والتنفيذ والمراجعة.\n"
                    + "مهم: هذه مخرجات مولّدة ومراجعة نصياً. لم يتم تشغيلها داخل Roblox Studio أو نشر لعبة تلقائياً.";
            put("final_output", summary);
            put("status", "completed");
            put("stage", "اكتملت دورة التطوير والمراجعة");
            put("progress", 100);
            save();
            return Result.success(new Data.Builder().putString("task_id", id).build());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            put("status", "paused");
            put("stage", "توقفت المهمة؛ يمكن إعادة المحاولة");
            put("error", "توقفت المهمة أثناء انتظار دور الخدمة.");
            save();
            return Result.retry();
        } catch (Exception e) {
            put("status", "failed");
            put("stage", "تعذّر إكمال المهمة");
            put("error", e.getMessage() == null ? e.toString() : e.getMessage());
            save();
            return Result.failure();
        }
    }

    private ForegroundInfo foregroundInfo(String title, String body) {
        String channelId = "apex_ai_work";
        NotificationManager manager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(channelId, "APEX AI tasks", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Long-running AI game development tasks");
            manager.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(app, channelId)
                : new Notification.Builder(app);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title)
                .setContentText(body)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        return new ForegroundInfo(1001, notification);
    }

    private void waitRateWindow() throws InterruptedException {
        // The anonymous provider tier is limited to 2 requests/minute/model/IP.
        Thread.sleep(RATE_GAP_MS);
    }

    private String ask(String system, String user) throws Exception {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpURLConnection c = null;
            try {
                JSONObject payload = new JSONObject();
                payload.put("model", MODEL);
                payload.put("temperature", 0.2);
                payload.put("max_tokens", 3600);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", system));
                messages.put(new JSONObject().put("role", "user").put("content", user));
                payload.put("messages", messages);

                c = (HttpURLConnection) new URL(API).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(45_000);
                c.setReadTimeout(240_000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("Accept", "application/json");
                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = c.getOutputStream()) { out.write(bytes); }

                int status = c.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                String body = readAll(stream);
                if (status == 429 || status == 503) {
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(60_000L);
                        continue;
                    }
                }
                if (status < 200 || status >= 300) {
                    throw new Exception("خدمة الذكاء الاصطناعي ردّت HTTP " + status + ": " + body.substring(0, Math.min(900, body.length())));
                }
                JSONObject response = new JSONObject(body);
                JSONArray choices = response.getJSONArray("choices");
                String content = choices.getJSONObject(0).getJSONObject("message").optString("content", "");
                if (content.trim().isEmpty()) throw new Exception("النموذج أعاد استجابة فارغة.");
                return content.trim();
            } catch (JSONException e) {
                throw new Exception("تعذر تحليل رد النموذج: " + e.getMessage(), e);
            } catch (java.net.SocketTimeoutException e) {
                if (attempt == MAX_RETRIES) throw new Exception("انتهت مهلة انتظار النموذج. أعد المحاولة عند اتصال مستقر.", e);
                Thread.sleep(20_000L);
            } finally {
                if (c != null) c.disconnect();
            }
        }
        throw new Exception("تعذر الوصول إلى نموذج الذكاء الاصطناعي بعد عدة محاولات.");
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

    private void put(String key, Object value) {
        try { state.put(key, value); } catch (JSONException ignored) {}
    }

    private void save() {
        TaskStore.save(app, state);
        setProgressAsync(new Data.Builder()
                .putInt("progress", state.optInt("progress"))
                .putString("stage", state.optString("stage"))
                .build());
    }
}
