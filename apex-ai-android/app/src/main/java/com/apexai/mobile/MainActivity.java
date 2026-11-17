package com.apexai.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONObject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(8,12,23), PANEL=Color.rgb(17,26,44), PANEL2=Color.rgb(22,34,56);
    private static final int TEXT=Color.rgb(241,246,255), MUTED=Color.rgb(151,168,194), CYAN=Color.rgb(110,231,224);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();

    private EditText titleInput,descriptionInput,taskPromptInput,baseInput,modelInput,keyInput,instructionsInput,researchInput,learningInput;
    private TextView roundsLabel,activeTitle,activeStage,activePercent,resultText,historyCount,memoryCount;
    private ProgressBar progressBar;
    private LinearLayout historyBox;
    private String selectedId=null,activeTab="plan";
    private int rounds=2;
    private Runnable ticker;

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
        try{buildUi();renderHistory();refreshSelected();}
        catch(Throwable e){showFatal("تعذر فتح الواجهة: "+e.getClass().getSimpleName()+" · "+e.getMessage());return;}
        ticker=()->{try{refreshSelected();renderHistory();}catch(Exception ignored){}handler.postDelayed(ticker,2000);};
        handler.post(ticker);
    }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=column();root.setPadding(dp(17),dp(14),dp(17),dp(28));root.setBackgroundColor(BG);scroll.addView(root);setContentView(scroll);

        LinearLayout brandRow=row();
        TextView mark=text("A",25,Color.rgb(8,16,30),true);mark.setGravity(Gravity.CENTER);mark.setBackground(round(CYAN,15));brandRow.addView(mark,lp(46,46));
        LinearLayout bt=column();bt.addView(text("APEX AI",18,TEXT,true));bt.addView(text("مختبر تطوير الألعاب · V0.2",10,MUTED,false));
        LinearLayout.LayoutParams btp=new LinearLayout.LayoutParams(0,-2,1);btp.setMargins(dp(10),0,0,0);brandRow.addView(bt,btp);brandRow.addView(chip("MEMORY + RESEARCH",CYAN));root.addView(brandRow);

        TextView headline=text("عقل تطوير ألعاب\\nيتعلم من كل مشروع",29,TEXT,true);headline.setPadding(0,dp(25),0,dp(8));root.addView(headline);
        root.addView(text("يبحث، يجمع المراجع، يكتب Luau، يحفظ المعرفة، يراجع عمله ثم يحسّنه وفق تعليماتك الدائمة.",13,MUTED,false));
        LinearLayout tags=row();tags.addView(chip("AUTO MEMORY",CYAN));tags.addView(space(6));tags.addView(chip("WEB RESEARCH",0xff91a7ff));tags.addView(space(6));tags.addView(chip("API CUSTOM",0xff62d6a6));
        LinearLayout.LayoutParams taglp=new LinearLayout.LayoutParams(-1,-2);taglp.topMargin=dp(12);root.addView(tags,taglp);

        LinearLayout settings=card();marginTop(root,settings,dp(20));
        settings.addView(text("إعدادات العقل ومزود API",18,TEXT,true));
        settings.addView(text("لا يلزمك إعداد أي شيء الآن. الإعداد الافتراضي يعمل دون مفتاح، ويمكنك لاحقاً إدخال مفتاحك وموديلك الخاص.",11,MUTED,false));
        baseInput=edit("رابط API متوافق مع OpenAI",false);baseInput.setText(AppSettings.base(this));
        modelInput=edit("اسم النموذج مثل code_chat@latest أو gpt-4o-mini",false);modelInput.setText(AppSettings.model(this));
        keyInput=edit("مفتاح API (يُحفظ مشفراً على الجهاز)",false);keyInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        instructionsInput=edit("تعليمات دائمة: أسلوب البرمجة، بنية الملفات، قواعد المشروع، ما يجب تجنبه...",true);
        instructionsInput.setText(AppSettings.instructions(this));
        settings.addView(label("عنوان API"));settings.addView(baseInput);
        settings.addView(label("النموذج"));settings.addView(modelInput);
        settings.addView(label("مفتاح API اختياري"));settings.addView(keyInput);
        settings.addView(label("تعليمات ثابتة يلتزم بها العقل في كل مهمة"));settings.addView(instructionsInput);
        LinearLayout settingsButtons=row();
        Button saveSettings=button("حفظ الإعدادات",CYAN,BG);Button testApi=button("اختبار الاتصال",PANEL2,TEXT);
        settingsButtons.addView(saveSettings,new LinearLayout.LayoutParams(0,dp(46),1));
        LinearLayout.LayoutParams testLp=new LinearLayout.LayoutParams(0,dp(46),1);testLp.setMargins(dp(7),0,0,0);settingsButtons.addView(testApi,testLp);
        LinearLayout.LayoutParams sbp=new LinearLayout.LayoutParams(-1,-2);sbp.topMargin=dp(13);settings.addView(settingsButtons,sbp);
        saveSettings.setOnClickListener(v->saveSettings());
        testApi.setOnClickListener(v->{saveSettings();testConnection();});

        LinearLayout task=card();marginTop(root,task,dp(16));
        task.addView(text("مهمة تطوير جديدة",20,TEXT,true));
        task.addView(text("أخبره بما تريد صنعه. سيبحث أولاً، ثم ينفذ خطة تطوير، ويراجع ويحسّن.",11,MUTED,false));
        titleInput=edit("اسم اللعبة أو المشروع",false);
        descriptionInput=edit("صف اللعبة كاملة: أسلوب اللعب، الأنظمة، الخرائط، الشخصيات، الأسلحة، الاقتصاد، الواجهات...",true);
        taskPromptInput=edit("برومبت إضافي لهذه المهمة (اختياري)",true);
        task.addView(label("اسم المشروع"));task.addView(titleInput);
        task.addView(label("وصف المشروع"));task.addView(descriptionInput);
        task.addView(label("تعليمات خاصة بهذه المهمة"));task.addView(taskPromptInput);
        task.addView(label("عدد جولات النقد والتحسين"));
        SeekBar seek=new SeekBar(this);seek.setMax(3);seek.setProgress(1);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        roundsLabel=text("2 جولات تحسين",12,CYAN,true);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){rounds=p+1;roundsLabel.setText(rounds+" جولات تحسين");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        task.addView(seek);task.addView(roundsLabel);
        Button start=button("ابدأ البحث والتطوير الذاتي",CYAN,BG);LinearLayout.LayoutParams startlp=new LinearLayout.LayoutParams(-1,dp(52));startlp.topMargin=dp(15);task.addView(start,startlp);
        start.setOnClickListener(v->startTask());

        LinearLayout research=card();marginTop(root,research,dp(16));
        research.addView(text("باحث المعرفة",17,TEXT,true));
        research.addView(text("ابحث عن أي معلومة واحفظها تلقائياً في ذاكرة APEX لاستخدامها في المشاريع القادمة.",11,MUTED,false));
        researchInput=edit("اكتب موضوع البحث أو سؤالاً تقنياً...",false);research.addView(researchInput);
        Button search=button("ابحث واحفظ النتائج",PANEL2,CYAN);LinearLayout.LayoutParams searchlp=new LinearLayout.LayoutParams(-1,dp(46));searchlp.topMargin=dp(9);research.addView(search,searchlp);
        search.setOnClickListener(v->searchAndSave());

        LinearLayout learn=card();marginTop(root,learn,dp(16));
        learn.addView(text("علّمه معلومة أو قاعدة جديدة",17,TEXT,true));
        learn.addView(text("أي معلومة تحفظها هنا تصبح ضمن ذاكرته الدائمة ويسترجعها عند الحاجة.",11,MUTED,false));
        learningInput=edit("مثلاً: في هذا المشروع نستخدم ModuleScripts لكل نظام...",true);learn.addView(learningInput);
        Button learnBtn=button("احفظ المعلومة في الذاكرة",PANEL2,CYAN);LinearLayout.LayoutParams llp=new LinearLayout.LayoutParams(-1,dp(44));llp.topMargin=dp(8);learn.addView(learnBtn,llp);
        learnBtn.setOnClickListener(v->saveLesson());

        LinearLayout active=card();marginTop(root,active,dp(16));
        active.addView(text("المهمة الحالية",12,CYAN,true));
        activeTitle=text("لا توجد مهمة محددة",18,TEXT,true);active.addView(activeTitle);
        activeStage=text("تظهر حالة العمل هنا",11,MUTED,false);active.addView(activeStage);
        progressBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progressBar.setMax(100);progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL2));
        LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-1,dp(8));plp.topMargin=dp(10);active.addView(progressBar,plp);
        activePercent=text("0%",11,MUTED,false);active.addView(activePercent);
        LinearLayout tabs=row();
        addTab(tabs,"الخطة","plan");addTab(tabs,"البحث","research");addTab(tabs,"التنفيذ","implementation");addTab(tabs,"المراجعة","review");
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2);tlp.topMargin=dp(9);active.addView(tabs,tlp);
        resultText=text("ستظهر نتائج المرحلة المختارة هنا.",12,TEXT,false);resultText.setTextIsSelectable(true);resultText.setGravity(Gravity.TOP|Gravity.START);resultText.setPadding(dp(12),dp(12),dp(12),dp(12));resultText.setBackground(round(0xff070e1b,12));resultText.setMinHeight(dp(180));active.addView(resultText);
        Button copy=button("نسخ النتيجة",PANEL2,TEXT);LinearLayout.LayoutParams copylp=new LinearLayout.LayoutParams(-1,dp(42));copylp.topMargin=dp(7);active.addView(copy,copylp);
        copy.setOnClickListener(v->{ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cb.setPrimaryClip(ClipData.newPlainText("APEX",resultText.getText()));Toast.makeText(this,"تم النسخ",Toast.LENGTH_SHORT).show();});

        LinearLayout history=card();marginTop(root,history,dp(16));
        history.addView(text("ذاكرة المشاريع وسجل المهام",17,TEXT,true));
        memoryCount=text("الذاكرة: 0 معلومة محفوظة",11,CYAN,false);history.addView(memoryCount);
        historyBox=column();history.addView(historyBox);

        LinearLayout note=card();marginTop(root,note,dp(16));
        note.addView(text("ماذا يعني التعلّم هنا؟",14,CYAN,true));
        note.addView(text("يحفظ APEX تلقائياً البرومبتات والخطط ونتائج البحث والأكواد والمراجعات والدروس في SQLite على جهازك، ويسترجعها لاحقاً عبر ذاكرة سياقية (RAG). كما يمكنك وضع تعليمات دائمة وإضافة معلومات يدوياً. هذا يحسّن سياق العمل، لكنه لا يغيّر أوزان النموذج تلقائياً. التدريب الحقيقي Fine-tuning يحتاج مجموعة بيانات منتقاة وبيئة تدريب GPU منفصلة؛ يمكن تجهيز بياناته من الخبرات المحفوظة لاحقاً.",11,MUTED,false));
        TextView footer=text("APEX AI · ANDROID · 0.2.0",10,MUTED,false);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(22),0,0);root.addView(footer);
    }

    private void addTab(LinearLayout tabs,String title,String key){
        Button b=button(title,key.equals(activeTab)?CYAN:PANEL2,key.equals(activeTab)?BG:TEXT);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(40),1);p.setMargins(dp(2),0,dp(2),0);tabs.addView(b,p);
        b.setOnClickListener(v->{activeTab=key;refreshSelected();});
    }

    private void saveSettings(){
        try{
            AppSettings.save(this,baseInput.getText().toString(),modelInput.getText().toString(),instructionsInput.getText().toString(),"duckduckgo");
            if(keyInput.getText().length()>0){
                SecureStore.saveApiKey(this,keyInput.getText().toString());
                keyInput.setText("");
            }
            Toast.makeText(this,"حُفظت الإعدادات. مفتاح API محفوظ بتشفير الجهاز.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"تعذر حفظ الإعدادات: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void testConnection(){
        Toast.makeText(this,"جارٍ اختبار النموذج...",Toast.LENGTH_SHORT).show();
        io.execute(()->{
            try{
                String answer=ApiClient.chat(this,"أجب بكلمة واحدة فقط: جاهز","اختبار اتصال APEX AI",80);
                runOnUiThread(()->Toast.makeText(this,"الاتصال ناجح: "+answer,Toast.LENGTH_LONG).show());
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"فشل الاتصال: "+e.getMessage(),Toast.LENGTH_LONG).show());}
        });
    }

    private void startTask(){
        String title=titleInput.getText().toString().trim(),description=descriptionInput.getText().toString().trim();
        String taskPrompt=taskPromptInput.getText().toString().trim();
        if(title.length()<3||description.length()<12){Toast.makeText(this,"اكتب اسم المشروع ووصفاً أطول.",Toast.LENGTH_LONG).show();return;}
        hideKeyboard();
        try{
            saveSettingsQuietly();
            String id=UUID.randomUUID().toString();
            JSONObject state=TaskStore.create(id,title,description,rounds);
            state.put("task_prompt",taskPrompt);
            TaskStore.save(this,state);
            // Only the small task id goes into WorkManager Data. Long prompts live in local persistent storage.
            Data input=new Data.Builder().putString("id",id).build();
            Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
            OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(AgentWorker.class)
                    .setInputData(input)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("apex-"+id,ExistingWorkPolicy.KEEP,request);
            selectedId=id;activeTab="plan";titleInput.setText("");descriptionInput.setText("");taskPromptInput.setText("");
            renderHistory();refreshSelected();
            Toast.makeText(this,"بدأ APEX المهمة. ستحفظ المراحل تلقائياً ويمكنك الخروج.",Toast.LENGTH_LONG).show();
        }catch(Throwable e){
            Toast.makeText(this,"لم تبدأ المهمة: "+e.getClass().getSimpleName()+" · "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void saveSettingsQuietly(){
        try{
            AppSettings.save(this,baseInput.getText().toString(),modelInput.getText().toString(),instructionsInput.getText().toString(),"duckduckgo");
            if(keyInput.getText().length()>0){SecureStore.saveApiKey(this,keyInput.getText().toString());keyInput.setText("");}
        }catch(Exception ignored){}
    }

    private void searchAndSave(){
        String query=researchInput.getText().toString().trim();
        if(query.length()<3){Toast.makeText(this,"اكتب موضوعاً للبحث.",Toast.LENGTH_SHORT).show();return;}
        Toast.makeText(this,"جارٍ البحث وحفظ المصادر...",Toast.LENGTH_SHORT).show();
        io.execute(()->{
            MemoryStore db=new MemoryStore(this);
            try{
                List<WebResearcher.ResultItem> items=WebResearcher.search(query,8);
                if(items.isEmpty())throw new Exception("لم يعثر محرك البحث على نتائج قابلة للاستخراج.");
                for(WebResearcher.ResultItem item:items)db.remember("global","web_research",item.title,item.snippet,item.url);
                String output=WebResearcher.format(items);
                runOnUiThread(()->{
                    selectedId=null;activeTab="research";activeTitle.setText("بحث: "+query);activeStage.setText("حُفظت "+items.size()+" مصادر في الذاكرة العامة");resultText.setText(output);updateMemoryCount();
                    Toast.makeText(this,"تم حفظ "+items.size()+" نتائج ومصادرها.",Toast.LENGTH_LONG).show();
                });
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"تعذر البحث: "+e.getMessage(),Toast.LENGTH_LONG).show());}
            finally{db.close();}
        });
    }

    private void saveLesson(){
        String note=learningInput.getText().toString().trim();
        if(note.length()<3){Toast.makeText(this,"اكتب المعلومة أولاً.",Toast.LENGTH_SHORT).show();return;}
        MemoryStore db=new MemoryStore(this);
        db.remember("global","user_lesson","تعليمات ومعلومة من المستخدم",note,"");
        db.close();
        learningInput.setText("");
        updateMemoryCount();
        Toast.makeText(this,"تم حفظ المعلومة وستدخل في سياق المهام القادمة.",Toast.LENGTH_LONG).show();
    }

    private void refreshSelected(){
        if(selectedId==null){
            List<JSONObject> all=TaskStore.all(this);
            if(!all.isEmpty())selectedId=all.get(0).optString("id");
        }
        if(selectedId==null)return;
        JSONObject t=TaskStore.load(this,selectedId);if(t==null)return;
        activeTitle.setText(t.optString("title","مشروع APEX"));
        activeStage.setText(t.optString("stage","—")+" · "+statusLabel(t.optString("status")));
        int p=t.optInt("progress",0);progressBar.setProgress(p);activePercent.setText(p+"%");
        String value=t.optString(activeTab,"");
        if(value.isEmpty()){
            if("failed".equals(t.optString("status")))value="الخطأ:\\n"+t.optString("error");
            else value="بانتظار مخرجات هذه المرحلة...";
        }
        resultText.setText(value);
        updateMemoryCount();
    }

    private void renderHistory(){
        if(historyBox==null)return;
        historyBox.removeAllViews();
        List<JSONObject> all=TaskStore.all(this);
        if(all.isEmpty())historyBox.addView(text("لا توجد مهام محفوظة بعد.",11,MUTED,false));
        for(JSONObject t:all){
            String id=t.optString("id");
            Button b=button(t.optString("title","مشروع")+" · "+t.optInt("progress",0)+"% · "+statusLabel(t.optString("status")),
                    id.equals(selectedId)?PANEL2:Color.TRANSPARENT,id.equals(selectedId)?CYAN:TEXT);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));p.topMargin=dp(4);historyBox.addView(b,p);
            b.setOnClickListener(v->{selectedId=id;activeTab="plan";refreshSelected();renderHistory();});
        }
        updateMemoryCount();
    }

    private void updateMemoryCount(){
        if(memoryCount==null)return;
        MemoryStore db=new MemoryStore(this);int n=db.count();db.close();
        memoryCount.setText("الذاكرة الدائمة: "+n+" معلومة / بحث / خطة / مراجعة");
    }

    private String statusLabel(String s){switch(s){case "queued":return "في الانتظار";case "running":return "يعمل الآن";case "completed":return "مكتملة";case "failed":return "تعذر إكمالها";case "paused":return "متوقفة";default:return s;}}
    private void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}
    private void showFatal(String message){new android.app.AlertDialog.Builder(this).setTitle("APEX AI").setMessage(message).setPositiveButton("إغلاق",(d,w)->finish()).show();}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(15),dp(15),dp(15),dp(15));l.setBackground(round(PANEL,18));return l;}
    private void marginTop(LinearLayout parent,View child,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=top;parent.addView(child,p);}
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);v.setLineSpacing(dp(3),1f);return v;}
    private TextView label(String s){TextView t=text(s,12,MUTED,true);t.setPadding(0,dp(12),0,dp(5));return t;}
    private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setTextColor(TEXT);e.setHintTextColor(0xff71819d);e.setHint(hint);e.setTextSize(13);e.setPadding(dp(12),dp(11),dp(12),dp(11));e.setBackground(round(0xff08101f,12));e.setGravity(Gravity.TOP|Gravity.RIGHT);if(multi){e.setMinLines(4);e.setMaxLines(10);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}else{e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);}return e;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setAllCaps(false);b.setTextColor(fg);b.setBackground(round(bg,12));b.setPadding(dp(7),0,dp(7),0);return b;}
    private TextView chip(String s,int c){TextView t=text(s,9,c,true);t.setPadding(dp(8),dp(6),dp(8),dp(6));t.setBackground(round(0xff19273c,20));return t;}
    private View space(int w){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return v;}
    private android.graphics.drawable.GradientDrawable round(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.argb(28,180,205,240));return d;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(dp(w),dp(h));}
    @Override protected void onDestroy(){if(ticker!=null)handler.removeCallbacks(ticker);io.shutdownNow();super.onDestroy();}
}
