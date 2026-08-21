# إعادة بناء مشروع MangaDT بتصميم Kotatsu (Material 3) لجميع المنصات

الهدف هو تحويل مشروع MangaDT الحالي إلى نسخة متكاملة تدعم الأندرويد وسطح المكتب (Desktop) باستخدام واجهة مستخدم مستوحاة من تطبيق Kotatsu الحديث، مع التركيز على القارئ (Reader) وجميع الخصائص الأساسية.

## التغييرات المقترحة

سأقوم ببناء نظام واجهة مستخدم متكيف (Adaptive UI) يتغير تلقائياً بين الأندرويد وسطح المكتب.

---

### [Component] الإعدادات والتبعيات (Dependencies)

#### [MODIFY] [libs.versions.toml](file:///home/abdellah/AndroidStudioProjects/mangaDT/gradle/libs.versions.toml)
إضافة مكتبات ضرورية:
- **Navigation Compose**: للتنقل بين الشاشات في KMP.
- **Coil3**: لتحميل الصور (يدعم KMP).
- **Material Symbols**: للأيقونات الحديثة.

---

### [Component] منطق العمل والنماذج (Shared Core)

#### [NEW] [MangaModels.kt](file:///home/abdellah/AndroidStudioProjects/mangaDT/shared/src/commonMain/kotlin/com/abht/manga_dt/models/MangaModels.kt)
تعريف الكيانات الأساسية:
- `Manga`: (العنوان، الرابط، غلاف، المصدر).
- `Chapter`: (العنوان، الرابط، رقم الفصل).
- `ReaderPage`: (رابط الصورة، رقم الصفحة).

---

### [Component] واجهة المستخدم المتكيفة (Adaptive UI)

#### [NEW] [MainScaffold.kt](file:///home/abdellah/AndroidStudioProjects/mangaDT/shared/src/commonMain/kotlin/com/abht/manga_dt/ui/MainScaffold.kt)
الهيكل الأساسي للتطبيق:
- **Mobile**: استخدام `NavigationBar` (Bottom Bar) كما في Kotatsu.
- **Desktop**: استخدام `NavigationRail` (Sidebar) لتناسب الشاشات الكبيرة.

#### [NEW] [LibraryScreen.kt](file:///home/abdellah/AndroidStudioProjects/mangaDT/shared/src/commonMain/kotlin/com/abht/manga_dt/ui/screens/LibraryScreen.kt)
شبكة عرض المانجا (Manga Grid) مع تصنيفات (Reading, Planned, etc).

#### [NEW] [ReaderScreen.kt](file:///home/abdellah/AndroidStudioProjects/mangaDT/shared/src/commonMain/kotlin/com/abht/manga_dt/ui/screens/ReaderScreen.kt)
قارئ المانجا:
- دعم التمرير الرأسي والأفقي.
- قائمة تحكم علوية وسفلية تظهر عند اللمس.
- معالج ذكي لتحميل الصفحات.

#### [NEW] [BrowseScreen.kt](file:///home/abdellah/AndroidStudioProjects/mangaDT/shared/src/commonMain/kotlin/com/abht/manga_dt/ui/screens/BrowseScreen.kt)
تصفح المصادر والبحث عن مانجا جديدة.

---

### [Component] التوافق مع سطح المكتب (Desktop Specific)

#### [MODIFY] [main.kt](file:///home/abdellah/AndroidStudioProjects/mangaDT/desktopApp/src/main/kotlin/com/abht/manga_dt/main.kt)
إعداد حجم النافذة الافتراضي وتكامل الأيقونات مع سطح المكتب.

---

## خطة التحقق (Verification Plan)

### الاختبارات الآلية
- التحقق من منطق التنقل (Navigation) وحالات الـ ViewModel.

### التحقق اليدوي
1. تشغيل `:androidApp` للتأكد من أن التصميم يطابق Kotatsu على الجوال.
2. تشغيل `:desktopApp` للتأكد من أن الواجهة تتكيف مع حجم الشاشة (Adaptive Layout) وتعمل بسلاسة باستخدام الفأرة ولوحة المفاتيح.
