# Proxy Browser

تطبيق أندرويد يعمل فقط عبر بروكسي مفحوص:
1. يجلب قوائم بروكسيات HTTP وSOCKS5 من proxyscrape.
2. يفحص كل بروكسي عبر طلب خفيف لموقع جوجل ويقيس زمن الاستجابة.
3. يعرض فقط البروكسيات اللي سرعتها أقل من 500ms، مرتبة من الأسرع.
4. عند اختيار بروكسي، يفتح متصفح (WebView) موجّه بالكامل عبر ذاك البروكسي،
   مع شريط عنوان وأزرار رجوع/تقدم/تحديث، وعرض السرعة بالـ ms في الأعلى.

## البنية التقنية

- **Kotlin** كلغة أساسية
- **Jetpack Compose** للواجهات
- **OkHttp + Coroutines** لجلب وفحص البروكسيات بالتوازي
- **androidx.webkit (PROXY_OVERRIDE)** لتوجيه WebView عبر البروكسي
- **minSdk 29** (Android 10+) — مطلوب لضمان دعم كامل لـ PROXY_OVERRIDE
- **GitHub Actions** يبني APK تلقائياً عند كل push لفرع main

## طريقة العمل من Termux

هذا المشروع مصمم بحيث تكتب الكود وتديره من Termux، وتترك GitHub Actions
يقوم بعملية البناء الفعلية (بناء APK كامل على الموبايل عبر Termux غير عملي).

### 1. تثبيت الأدوات داخل Termux

```bash
pkg update && pkg upgrade
pkg install git openssh -y
```

### 2. رفع المشروع لأول مرة

```bash
cd ProxyBrowser
git init
git add .
git commit -m "أول نسخة من تطبيق Proxy Browser"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```

استبدل `USERNAME/REPO` باسم حسابك واسم المستودع اللي تسويه على GitHub.
عند أول push، تحتاج تسوي مصادقة (رمز وصول شخصي Personal Access Token
بدل كلمة المرور، لأن GitHub لا يقبل كلمة مرور الحساب مباشرة).

### 3. البناء التلقائي

بمجرد ما يوصل الكود لفرع `main`، ملف
`.github/workflows/build.yml` يشتغل تلقائياً على سيرفرات GitHub:
- يثبت JDK 17
- يبني APK (نسخة Debug)
- يرفع الـ APK كـ "Artifact" يقدر تنزله من تبويب **Actions** في المستودع

### 4. أي تعديل لاحق

```bash
git add .
git commit -m "وصف التعديل"
git push
```

كل push جديد يشغّل البناء من جديد ويولّد APK محدّث تلقائياً.

## ملاحظات مهمة

- الفحص يتم وقت تشغيل التطبيق على الجهاز نفسه (مو أثناء البناء)، فالتطبيق
  يحتاج اتصال إنترنت فعلي عند فتحه ليجلب ويفحص البروكسيات.
- بعض البروكسيات المجانية من proxyscrape قد تكون غير مستقرة أو تتغير باستمرار؛
  هذا طبيعي لطبيعة القوائم المجانية العامة.
- توجيه WebView عبر بروكسي (PROXY_OVERRIDE) مدعوم بشكل مضمون من Android 10
  فما فوق، وهذا سبب اختيار `minSdk = 29`.
