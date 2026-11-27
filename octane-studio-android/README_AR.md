# Octane Studio Mobile — Android APK

هذا تطبيق Android مستقل (Java / Android SDK) للوصول إلى واجهة Octane Studio Mobile Bridge على كمبيوتر Windows من الهاتف.

## الوظيفة الفعلية
- يثبت كتطبيق APK مستقل على Android.
- يحتوي شاشة اتصال أصلية لإدخال عنوان الكمبيوتر على شبكة Wi-Fi.
- يفتح واجهة التحكم داخل WebView بملء مساحة التطبيق.
- يستفيد من بث الشاشة وأزرار اللمس ولوحة المفاتيح ولصق Luau التي يقدمها جسر الكمبيوتر.
- لا يحتاج إلى تسجيل دخول خارجي أو خدمة سحابية.

## المتطلبات
1. شغّل جسر الكمبيوتر الموجود في حزمة OctaneStudioMobileBridge على Windows.
2. اجعل الهاتف والكمبيوتر على شبكة Wi-Fi واحدة.
3. أدخل في التطبيق العنوان الذي يعرضه الجسر، مثل: http://192.168.1.15:8765
4. أدخل PIN على صفحة الجسر التي تظهر داخل التطبيق.

## الحدود
هذا ليس إصدار Roblox Studio محلياً على Android. Octane/Roblox Studio يستمر بالعمل على الكمبيوتر، والتطبيق هو واجهة تحكم وبث محلية. لا يغيّر هذا المشروع ملفات OctanePlayerLauncher.exe أو ملفات Roblox.

## البناء من المصدر
يتطلب JDK 17 وAndroid SDK Platform 35 وBuild Tools 35 وGradle 8.9:
```bash
gradle :app:assembleDebug
```
الملف الناتج:
`app/build/outputs/apk/debug/app-debug.apk`

## الأمان
يسمح التطبيق فقط بعناوين IPv4 الخاصة بالشبكة المحلية أو أسماء أجهزة تنتهي بـ .local. لا تستخدم Port Forwarding ولا تشارك PIN.
