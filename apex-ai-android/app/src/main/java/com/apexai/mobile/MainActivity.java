package com.apexai.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONObject;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 12, 23);
    private static final int PANEL = Color.rgb(17, 26, 44);
    private static final int PANEL2 = Color.rgb(22, 34, 56);
    private static final int TEXT = Color.rgb(241, 246, 255);
    private static final int MUTED = Color.rgb(151, 168, 194);
    private static final int CYAN = Color.rgb(110, 231, 224);
    private static final int BLUE = Color.rgb(126, 148, 255);
    private static final int GREEN = Color.rgb(98, 214, 166);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText titleInput, descriptionInput;
    private TextView roundsLabel, activeTitle, activeStage, activePercent, resultText;
    private ProgressBar progressBar;
    private LinearLayout historyBox;
    private String selectedId = null;
    private String activeTab = "plan";
    private int rounds = 2;
    private Runnable ticker;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        buildUi();
        renderHistory();
        ticker = new Runnable() {
            @Override public void run() {
                refreshSelected();
                renderHistory();
                handler.postDelayed(this, 1800);
            }
        };
        handler.post(ticker);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout brandRow = row();
        TextView mark = text("A", 25, Color.rgb(8,16,30), true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(CYAN, 16));
        brandRow.addView(mark, lp(46,46));
        LinearLayout brandText = column();
        TextView brand = text("APEX AI", 18, TEXT, true);
        brand.setLetterSpacing(0.12f);
        brandText.addView(brand);
        brandText.addView(text("مختبر تطوير الألعاب · ANDROID", 10, MUTED, false));
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, -2, 1);
        brandParams.setMargins(dp(10), 0, 0, 0);
        brandRow.addView(brandText, brandParams);
        brandRow.addView(chip("AI CODE ENGINE", CYAN));
        root.addView(brandRow);

        TextView headline = text("حوّل فكرتك إلى\nمشروع لعبة حقيقي", 30, TEXT, true);
        headline.setPadding(0, dp(28), 0, dp(7));
        root.addView(headline);
        TextView intro = text("APEX يخطط، يكتب Luau، يراجع الأخطاء ويعيد تحسين الحل تلقائياً. لا تحتاج إلى إعداد نموذج أو مفتاح API.", 14, MUTED, false);
        intro.setLineSpacing(dp(4), 1f);
        root.addView(intro);

        LinearLayout heroTags = row();
        heroTags.addView(chip("ROBLOX + LUAU", CYAN));
        heroTags.addView(space(7));
        heroTags.addView(chip("AUTO REVIEW", BLUE));
        heroTags.addView(space(7));
        heroTags.addView(chip("NO API KEY", GREEN));
        LinearLayout.LayoutParams tagsLp = new LinearLayout.LayoutParams(-1, -2);
        tagsLp.topMargin = dp(14);
        root.addView(heroTags, tagsLp);

        LinearLayout createCard = card();
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.topMargin = dp(22);
        root.addView(createCard, cardLp);
        createCard.addView(text("مهمة تطوير جديدة", 20, TEXT, true));
        createCard.addView(text("اكتب وصف اللعبة بالتفصيل. APEX سيتولى تقسيمها إلى مراحل.", 12, MUTED, false));
        titleInput = edit("اسم اللعبة أو المشروع", false);
        descriptionInput = edit("اشرح اللعبة والأنظمة والخريطة والشخصيات وطريقة اللعب...", true);
        createCard.addView(label("اسم المشروع"));
        createCard.addView(titleInput);
        createCard.addView(label("تفاصيل اللعبة"));
        createCard.addView(descriptionInput);

        TextView roundTitle = label("جهد التطوير (جولات التحسين)");
        createCard.addView(roundTitle);
        SeekBar seek = new SeekBar(this);
        seek.setMax(3);
        seek.setProgress(1);
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        roundsLabel = text("جولتان من التحسين", 12, CYAN, true);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                rounds = p + 1;
                roundsLabel.setText(rounds + (rounds == 1 ? " جولة تحسين" : " جولات تحسين"));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        createCard.addView(seek);
        createCard.addView(roundsLabel);

        Button start = button("ابدأ التطوير الذاتي  ٩", CYAN, BG);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(-1, dp(52));
        startLp.topMargin = dp(18);
        createCard.addView(start, startLp);
        start.setOnClickListener(v -> startTask());

        LinearLayout activeCard = card();
        activeCard.setBackground(round(PANEL, 20));
        LinearLayout.LayoutParams activeLp = new LinearLayout.LayoutParams(-1, -2);
        activeLp.topMargin = dp(18);
        root.addView(activeCard, activeLp);
        activeCard.addView(text("حالة المشروع", 12, CYAN, true));
        activeTitle = text("لم تبدأ أي مهمة بعد", 18, TEXT, true);
        activeTitle.setPadding(0, dp(5), 0, dp(4));
        activeCard.addView(activeTitle);
        activeStage = text("أنشئ مهمة لتبدأ", 12, MUTED, false);
        activeCard.addView(activeStage);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL2));
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(-1, dp(8));
        progLp.topMargin = dp(12);
        progLp.bottomMargin = dp(5);
        activeCard.addView(progressBar, progLp);
        activePercent = text("0%", 11, MUTED, false);
        activeCard.addView(activePercent);

        LinearLayout tabs = row();
        String[] tabNames = {"الخطة", "التنفيذ", "المراجعة"};
        String[] tabKeys = {"plan", "implementation", "review"};
        for (int i=0;i<tabNames.length;i++) {
            final String key = tabKeys[i];
            Button tab = button(tabNames[i], key.equals(activeTab) ? CYAN : PANEL2, key.equals(activeTab) ? BG : TEXT);
            LinearLayout.LayoutParams t = new LinearLayout.LayoutParams(0, dp(42), 1);
            t.setMargins(dp(3), dp(13), dp(3), dp(8));
            tabs.addView(tab,t);
            tab.setOnClickListener(v -> { activeTab=key; refreshSelected(); });
        }
        activeCard.addView(tabs);
        resultText = text("ستظهر هنا نتائج كل مرحلة.\n\nاختر جهد التطوير ثم ابدأ.", 12, TEXT, false);
        resultText.setTextIsSelectable(true);
        resultText.setGravity(Gravity.TOP | Gravity.START);
        resultText.setPadding(dp(12), dp(12), dp(12), dp(12));
        resultText.setBackground(round(Color.rgb(7, 14, 27), 12));
        resultText.setMinHeight(dp(190));
        activeCard.addView(resultText);
        Button copy = button("نسخ النص المعروض", PANEL2, TEXT);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(-1, dp(44));
        copyLp.topMargin = dp(8);
        activeCard.addView(copy, copyLp);
        copy.setOnClickListener(v -> {
            if (resultText.getText().length()==0) return;
            ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("APEX AI",resultText.getText()));
            Toast.makeText(this,"تم النسخ",Toast.LENGTH_SHORT).show();
        });

        LinearLayout historyCard = card();
        LinearLayout.LayoutParams histLp = new LinearLayout.LayoutParams(-1,-2);
        histLp.topMargin=dp(18);
        root.addView(historyCard,histLp);
        historyCard.addView(text("سجل المشاريع",18,TEXT,true));
        historyBox = column();
        historyCard.addView(historyBox);

        LinearLayout info = card();
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1,-2);
        infoLp.topMargin=dp(18);
        root.addView(info,infoLp);
        info.addView(text("كيف يعمل الذكاء الاصطناعي؟",14,CYAN,true));
        info.addView(text("يتصل APEX تلقائياً بنموذج برمجة سحابي مفتوح الأوزان عبر OVHcloud. لا يحتاج تسجيل دخول أو مفتاح API في هذا الإصدار. الطلبات ترسل وصف مشروعك إلى مزود الخدمة لمعالجتها. لا ترسل كلمات مرور أو بيانات خاصة.",11,MUTED,false));
        info.addView(text("حد الخدمة المجهولة: طلبان في الدقيقة لكل نموذج وعنوان IP؛ لذلك يضيف التطبيق فواصل انتظار تلقائية. جودة التوفر والسرعة تعتمد على الخدمة المجانية.",11,MUTED,false));
        info.addView(text("هذه النسخة تولّد وتراجع الكود لكنها لا تشغّله داخل Roblox ولا تنشر اللعبة تلقائياً.",11,MUTED,false));
        TextView footer=text("APEX AI · VERSION 0.1.0",10,MUTED,false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,dp(24),0,dp(3));
        root.addView(footer);
    }

    private void startTask() {
        String title=titleInput.getText().toString().trim();
        String desc=descriptionInput.getText().toString().trim();
        if(title.length()<3 || desc.length()<12) {
            Toast.makeText(this,"اكتب اسم المشروع ووصفاً أوضح.",Toast.LENGTH_LONG).show();
            return;
        }
        hideKeyboard();
        String id=UUID.randomUUID().toString();
        JSONObject state=TaskStore.create(id,title,desc,rounds);
        TaskStore.save(this,state);
        Data input=new Data.Builder().putString("id",id).putString("title",title)
                .putString("description",desc).putInt("rounds",rounds).build();
        Constraints constraints=new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(AgentWorker.class)
                .setInputData(input).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniqueWork("apex-"+id, ExistingWorkPolicy.KEEP, request);
        selectedId=id;
        titleInput.setText("");
        descriptionInput.setText("");
        activeTab="plan";
        renderHistory();
        refreshSelected();
        Toast.makeText(this,"بدأ APEX تنفيذ المهمة. سيظهر إشعار أثناء العمل.",Toast.LENGTH_LONG).show();
    }

    private void refreshSelected() {
        if(selectedId==null) {
            List<JSONObject> all=TaskStore.all(this);
            if(!all.isEmpty()) selectedId=all.get(0).optString("id");
        }
        if(selectedId==null) return;
        JSONObject t=TaskStore.load(this,selectedId);
        if(t==null) return;
        activeTitle.setText(t.optString("title","مشروع APEX"));
        activeStage.setText(t.optString("stage","—") + " · " + statusLabel(t.optString("status")));
        int p=t.optInt("progress",0);
        progressBar.setProgress(p);
        activePercent.setText(p+"%");
        String value=t.optString(activeTab,"");
        if(value.isEmpty()) {
            if("failed".equals(t.optString("status"))) value="حدث خطأ:\n"+t.optString("error");
            else value="بانتظار مخرجات هذه المرحلة...";
        }
        resultText.setText(value);
    }

    private void renderHistory() {
        if(historyBox==null) return;
        historyBox.removeAllViews();
        List<JSONObject> all=TaskStore.all(this);
        if(all.isEmpty()) {
            historyBox.addView(text("لا توجد مشاريع محفوظة بعد.",12,MUTED,false));
            return;
        }
        for(JSONObject t:all) {
            String id=t.optString("id");
            Button item=button(t.optString("title","مشروع") + "  ·  " + t.optInt("progress",0)+"%", 
                    id.equals(selectedId)?PANEL2:Color.TRANSPARENT, id.equals(selectedId)?CYAN:TEXT);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));
            lp.topMargin=dp(5);
            historyBox.addView(item,lp);
            item.setOnClickListener(v->{selectedId=id;refreshSelected();renderHistory();});
        }
    }

    private String statusLabel(String s) {
        switch(s) {
            case "queued": return "في الانتظار";
            case "running": return "يعمل الآن";
            case "completed": return "مكتملة";
            case "failed": return "فشلت";
            case "paused": return "متوقفة";
            default: return s;
        }
    }

    private void hideKeyboard() {
        View v=getCurrentFocus();
        if(v!=null) {
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(),0);
            v.clearFocus();
        }
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(round(PANEL,18));return l;}
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);v.setLineSpacing(dp(3),1f);return v;}
    private TextView label(String s){TextView v=text(s,12,MUTED,true);v.setPadding(0,dp(13),0,dp(6));return v;}
    private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setTextColor(TEXT);e.setHintTextColor(Color.rgb(111,129,157));e.setHint(hint);e.setTextSize(13);e.setPadding(dp(13),dp(12),dp(13),dp(12));e.setBackground(round(Color.rgb(8,16,31),12));e.setGravity(Gravity.TOP|Gravity.RIGHT);if(multi){e.setMinLines(5);e.setMaxLines(12);e.setInputType( android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}else{e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}return e;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(fg);b.setBackground(round(bg,12));b.setPadding(dp(10),0,dp(10),0);return b;}
    private TextView chip(String s,int color){TextView t=text(s,9,color,true);t.setPadding(dp(9),dp(7),dp(9),dp(7));t.setBackground(round(Color.rgb(25,39,60),20));return t;}
    private View space(int w){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return v;}
    private android.graphics.drawable.GradientDrawable round(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.argb(28,180,205,240));return d;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(dp(w),dp(h));}

    @Override protected void onDestroy(){if(ticker!=null)handler.removeCallbacks(ticker);super.onDestroy();}
}
