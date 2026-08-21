package com.abht.manga_dt.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "Follow System", "تلقائي (حسب لغة الجهاز)"),
    ARABIC("ar", "Arabic", "العربية"),
    ENGLISH("en", "English", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

expect fun isSystemLanguageArabic(): Boolean

object Strings {
    val current: AppStrings
        @Composable
        get() = when (AppSettings.appLanguage) {
            AppLanguage.ARABIC -> ArabicStrings
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.SYSTEM -> if (isSystemLanguageArabic()) ArabicStrings else EnglishStrings
        }
}

interface AppStrings {
    val appName: String
    val home: String
    val library: String
    val updates: String
    val browse: String
    val settings: String
    val favorites: String
    val history: String
    val popular: String
    val latest: String
    val searchManga: String
    val searchLocal: String
    val searchSources: String
    val onlineMangaMode: String
    val localLibraryMode: String
    val sourcesMode: String
    val sources: String
    val genre: String
    val language: String
    val all: String
    val continueReading: String
    val startReading: String
    val resumeReading: String
    val inLibrary: String
    val addToLibrary: String
    val chapters: String
    val noMangaFound: String
    val retry: String
    val statusOngoing: String
    val statusCompleted: String
    val statusOnHold: String
    val statusDropped: String
    val appearance: String
    val colorScheme: String
    val theme: String
    val themeDark: String
    val themeLight: String
    val themeSystem: String
    val themeSystemDesc: String
    val blackAmoled: String
    val blackAmoledDesc: String
    val selectThemeTitle: String
    val darkTheme: String
    val nsfwContent: String
    val nsfwContentDesc: String
    val appLanguage: String
    val selectLanguageTitle: String
    val clearCache: String
    val clearCacheSuccess: String
    val expandSidebar: String
    val collapseSidebar: String
    val selectSourceTitle: String
    val selectGenreTitle: String
    val close: String
    val clear: String
    val ok: String
    val cancel: String
    val notificationsTitle: String
    val noNotifications: String
    val notificationsSubtitle: String
    val userDefaultName: String
    val userSubtitle: String
    val typeToSearchLocal: String
    val typeToSearchOnline: String
    val loadingManga: String
    val defaultBrowseLayout: String
    val layoutList: String
    val layoutComfortable: String
    val layoutCompact: String
    val selectLayoutTitle: String
    val defaultReadingMode: String
    val readingModeWebtoon: String
    val readingModeVerticalPaged: String
    val readingModeLTR: String
    val readingModeRTL: String
    val selectReadingModeTitle: String
    val readerSettings: String
    val readerMode: String
    val readerBackground: String
    val readerScale: String
    val readerScaleFitWidth: String
    val readerScaleFitHeight: String
    val readerScaleFitScreen: String
    val readerScaleOriginal: String
    val readerBgBlack: String
    val readerBgDarkGray: String
    val readerBgWhite: String
    val previousChapter: String
    val nextChapter: String
    val chapterList: String
    val searchChapters: String
    val endOfChapter: String
    val finishedChapter: String
    val goToNextChapter: String
    val goToPreviousChapter: String
    val noMoreChapters: String
    val goToPage: String
    val pageIndicator: String
    val pageIndicatorDesc: String
    val pageFailedToLoad: String
    val tapToRetry: String
    val keepScreenOn: String
    val keepScreenOnDesc: String
    val cropBorders: String
    val cropBordersDesc: String
    val pinnedSourcesTitle: String
    val noPinnedSources: String
    val clearAllPinned: String
    val versionText: String
    val autoConfigured: String
    val storageUsageDesc: String
    val captchaRequired: String
    val solveCaptcha: String
    val captchaSolved: String
    val captchaHelpText: String
    val iHaveSolvedIt: String

    // Profile & Statistics strings
    val profile: String
    val statistics: String
    val profileAndStats: String
    val totalReadingTime: String
    val chaptersRead: String
    val pagesRead: String
    val mangaRead: String
    val readingStreak: String
    val streakDays: String
    val bestStreak: String
    val dailyAverage: String
    val averageTimePerChapter: String
    val rank: String
    val rankNovice: String
    val rankReader: String
    val rankEnthusiast: String
    val rankOtaku: String
    val rankLegend: String
    val topGenres: String
    val weeklyActivity: String
    val mangaStatisticsTitle: String
    val timeSpent: String
    val lastRead: String
    val completedChapters: String
    val editProfile: String
    val username: String
    val bio: String
    val save: String
    val privacyLocalNotice: String
    val noStatsYet: String
    val clearStats: String
    val clearStatsConfirm: String

    // Profile Experience strings
    val overviewAndActivity: String
    val detailedAnalytics: String
    val currentlyReading: String
    val myFavorites: String
    val planToRead: String
    val tasteDna: String
    val readingVibe: String
    val noFavoritesYet: String
    val noPlanToReadYet: String
    val noCurrentReading: String

    // Local Network Sync strings
    val localSync: String
    val localSyncDesc: String
    val myDevice: String
    val deviceName: String
    val serverStatus: String
    val serverRunning: String
    val serverStopped: String
    val localIp: String
    val scanDevices: String
    val scanning: String
    val noDevicesFound: String
    val discoveredDevices: String
    val syncNow: String
    val manualConnect: String
    val enterTargetIp: String
    val connectAndSync: String
    val syncOptions: String
    val syncLibrary: String
    val syncHistory: String
    val syncStats: String
    val syncSettings: String
    val syncSuccess: String
    val syncFailed: String
    val syncing: String
    val lastSync: String

    // Backup & Restore strings
    val createBackup: String
    val createBackupDesc: String
    val restoreBackup: String
    val restoreBackupDesc: String
    val backupCreatedSuccess: String
    val backupRestoredSuccess: String
    val backupSavedAt: String
    val restoreModeTitle: String
    val restoreModeMerge: String
    val restoreModeMergeDesc: String
    val restoreModeOverwrite: String
    val restoreModeOverwriteDesc: String
    val backupPreviewTitle: String
    val backupDate: String
    val backupLibraryItems: String
    val backupHistoryEntries: String
    val backupStatsEntries: String
    val localBackupsList: String
    val noLocalBackupsFound: String
    val selectFileFromDevice: String
    val selectFileFromDeviceDesc: String
    val pasteJsonBackup: String
    val pasteJsonPlaceholder: String
    val invalidBackupFormat: String
    val deleteBackupConfirm: String

    // Deep Analytics Strings
    val deepAnalyticsTitle: String
    val deepAnalyticsSubtitle: String
    val readingPace: String
    val secondsPerPage: String
    val minutesPerChapter: String
    val longestSession: String
    val peakReadingTime: String
    val morningReader: String
    val afternoonReader: String
    val nightReader: String
    val allMangaDetailed: String
    val sortByTime: String
    val sortByChapters: String
    val sortByPages: String
    val sortByLastRead: String
    val openDeepAnalytics: String

    fun searchingInSource(source: String): String
    fun noResultsFor(query: String): String
    fun libraryResultsCount(count: Int): String
    fun historyResultsCount(count: Int): String
    fun chapterFormat(title: String): String
    fun pageFormat(page: Int): String
    fun categoryFormat(cat: String): String
    fun languageFormat(lang: String): String
}

object ArabicStrings : AppStrings {
    override val appName = "مانـغـا DT"
    override val home = "الرئيسية"
    override val library = "المكتبة"
    override val updates = "التحديثات"
    override val browse = "المصادر والتصفح"
    override val settings = "الإعدادات"
    override val favorites = "المفضلة"
    override val history = "سجل القراءة"
    override val popular = "الأكثر شعبية"
    override val latest = "أحدث الفصول"
    override val searchManga = "البحث عن مانجا أونلاين..."
    override val searchLocal = "البحث في المكتبة وسجل القراءة..."
    override val searchSources = "البحث عن مصدر مانجا..."
    override val onlineMangaMode = "مانجا أونلاين"
    override val localLibraryMode = "المكتبة والسجل"
    override val sourcesMode = "المصادر"
    override val sources = "المصدر"
    override val genre = "التصنيف"
    override val language = "اللغة"
    override val all = "الكل"
    override val continueReading = "متابعة القراءة"
    override val startReading = "بدء القراءة"
    override val resumeReading = "استئناف"
    override val inLibrary = "في المكتبة"
    override val addToLibrary = "إضافة للمكتبة"
    override val chapters = "الفصول"
    override val noMangaFound = "لا توجد مانجا متوفرة في هذا القسم"
    override val retry = "إعادة المحاولة"
    override val statusOngoing = "مستمرة"
    override val statusCompleted = "مكتملة"
    override val statusOnHold = "متوقفة"
    override val statusDropped = "ملغية"
    override val appearance = "المظهر والتصميم"
    override val colorScheme = "نظام الألوان"
    override val theme = "السمة"
    override val themeDark = "داكن"
    override val themeLight = "فاتح"
    override val themeSystem = "اتباع سمة النظام"
    override val themeSystemDesc = "التبديل بين الفاتح والداكن تلقائياً وفق إعدادات جهازك"
    override val blackAmoled = "الأسود الداكن"
    override val blackAmoledDesc = "يوفر استهلاك الطاقة لشاشات AMOLED"
    override val selectThemeTitle = "اختر السمة"
    override val darkTheme = "الوضع الداكن"
    override val nsfwContent = "عرض محتوى 18+ (NSFW)"
    override val nsfwContentDesc = "السماح بعرض والبحث في المصادر الخاصة بالبالغين"
    override val appLanguage = "لغة التطبيق"
    override val selectLanguageTitle = "اختر لغة التطبيق"
    override val clearCache = "مسح الذاكرة المؤقتة"
    override val clearCacheSuccess = "تم مسح الذاكرة المؤقتة بنجاح ✓"
    override val expandSidebar = "توسيع القائمة"
    override val collapseSidebar = "طي القائمة"
    override val selectSourceTitle = "اختر المصدر المعروض"
    override val selectGenreTitle = "اختر التصنيف"
    override val close = "إغلاق"
    override val clear = "مسح"
    override val ok = "حسناً"
    override val cancel = "إلغاء"
    override val notificationsTitle = "الإشعارات والتحديثات"
    override val noNotifications = "لا توجد إشعارات جديدة في الوقت الحالي."
    override val notificationsSubtitle = "ستظهر هنا تحديثات الفصول الجديدة لمانجا مكتبتك فور صدورها."
    override val userDefaultName = "مستخدم Manga DT"
    override val userSubtitle = "reader@mangadt.app"
    override val typeToSearchLocal = "اكتب للبحث داخل المكتبة وسجل القراءة"
    override val typeToSearchOnline = "اكتب اسم المانجا للبحث أونلاين"
    override val loadingManga = "جاري تحميل أحدث فصول وقوائم المانجا..."
    override val defaultBrowseLayout = "نمط عرض المصادر"
    override val layoutList = "عرض القائمة"
    override val layoutComfortable = "شبكة مريحة"
    override val layoutCompact = "شبكة مدمجة"
    override val selectLayoutTitle = "اختر نمط العرض"
    override val defaultReadingMode = "نمط القراءة الافتراضي"
    override val readingModeWebtoon = "ويب تون (طولي مستمر)"
    override val readingModeVerticalPaged = "صفحات عمودية"
    override val readingModeLTR = "من اليسار إلى اليمين"
    override val readingModeRTL = "من اليمين إلى اليسار"
    override val selectReadingModeTitle = "اختر نمط القراءة"
    override val readerSettings = "إعدادات القارئ"
    override val readerMode = "نمط القراءة"
    override val readerBackground = "لون الخلفية"
    override val readerScale = "ملاءمة وحجم الصور"
    override val readerScaleFitWidth = "ملاءمة العرض"
    override val readerScaleFitHeight = "ملاءمة الارتفاع"
    override val readerScaleFitScreen = "ملاءمة الشاشة"
    override val readerScaleOriginal = "الحجم الأصلي"
    override val readerBgBlack = "أسود AMOLED"
    override val readerBgDarkGray = "رمادي داكن"
    override val readerBgWhite = "أبيض"
    override val previousChapter = "الفصل السابق"
    override val nextChapter = "الفصل التالي"
    override val chapterList = "قائمة الفصول"
    override val searchChapters = "البحث في الفصول..."
    override val endOfChapter = "نهاية الفصل"
    override val finishedChapter = "أتممت قراءة هذا الفصل بنجاح"
    override val goToNextChapter = "الانتقال إلى الفصل التالي"
    override val goToPreviousChapter = "العودة إلى الفصل السابق"
    override val noMoreChapters = "وصلت إلى أحدث فصل متوفر"
    override val goToPage = "الانتقال إلى صفحة"
    override val pageIndicator = "مؤشر رقم الصفحة"
    override val pageIndicatorDesc = "إظهار فقاعة رقم الصفحة والوقت أثناء القراءة"
    override val pageFailedToLoad = "فشل تحميل هذه الصفحة"
    override val tapToRetry = "اضغط لإعادة المحاولة"
    override val keepScreenOn = "إبقاء الشاشة قيد التشغيل"
    override val keepScreenOnDesc = "منع إيقاف تشغيل الشاشة تلقائياً أثناء القراءة"
    override val cropBorders = "قص الحواف والهوامش البيضاء"
    override val cropBordersDesc = "إزالة الهوامش الفارغة المحيطة بالصفحات تلقائياً"
    override val pinnedSourcesTitle = "المصادر المثبتة"
    override val noPinnedSources = "لا توجد مصادر مثبتة حالياً"
    override val clearAllPinned = "إلغاء تثبيت الكل"
    override val versionText = "الإصدار 1.0.0 • محرك كوتاتسو"
    override val autoConfigured = "يتم ضبط هذه الإعدادات تلقائياً."
    override val storageUsageDesc = "الذاكرة المؤقتة لصور الأغلفة والفصول"
    override val captchaRequired = "يتطلب هذا المصدر إكمال التحقق البشري (Captcha / Cloudflare) للمتابعة"
    override val solveCaptcha = "حل التحقق في المتصفح المدمج (WebView)"
    override val captchaSolved = "تم التحقق بنجاح ✓"
    override val captchaHelpText = "يرجى حل اختبار التحقق البشري الظاهر في الصفحة ثم الضغط على \"تم اجتياز التحقق\""
    override val iHaveSolvedIt = "تم اجتياز التحقق"

    // Profile & Statistics (Arabic)
    override val profile = "الملف الشخصي"
    override val statistics = "الإحصائيات"
    override val profileAndStats = "الملف الشخصي والنشاط"
    override val totalReadingTime = "وقت القراءة"
    override val chaptersRead = "الفصول المقروءة"
    override val pagesRead = "الصفحات"
    override val mangaRead = "الأعمال"
    override val readingStreak = "سلسلة القراءة"
    override val streakDays = "أيام متتالية"
    override val bestStreak = "أطول سلسلة قراءة"
    override val dailyAverage = "المعدل اليومي"
    override val averageTimePerChapter = "متوسط وقت الفصل"
    override val rank = "الرتبة القرائية"
    override val rankNovice = "مبتدئ"
    override val rankReader = "قارئ نشط"
    override val rankEnthusiast = "عاشق للمانغا"
    override val rankOtaku = "خبير أوتاكو"
    override val rankLegend = "أسطورة المانغا"
    override val topGenres = "أكثر التصنيفات قراءة"
    override val weeklyActivity = "نشاط القراءة الأسبوعي"
    override val mangaStatisticsTitle = "إحصائيات كل مانغا بالتفصيل"
    override val timeSpent = "الوقت المستغرق"
    override val lastRead = "آخر قراءة"
    override val completedChapters = "الفصول المكتملة"
    override val editProfile = "تعديل الملف الشخصي"
    override val username = "اسم المستخدم"
    override val bio = "النبذة التعريفية"
    override val save = "حفظ التغييرات"
    override val privacyLocalNotice = "بياناتك ونشاطك مخزن محلياً بالكامل 100% على جهازك بترخيص مفتوح المصدر (GPL) دون أي اتصال بخوادم خارجية."
    override val noStatsYet = "لا توجد إحصائيات مسجلة بعد. ابدأ بقراءة أي مانغا ليتم تجميع نشاطك تلقائياً!"
    override val clearStats = "إعادة ضبط الإحصائيات"
    override val clearStatsConfirm = "هل أنت متأكد من رغبتك في حذف جميع إحصائيات القراءة المخزنة محلياً؟"

    // Profile Experience (Arabic)
    override val overviewAndActivity = "نظرة عامة وشخصيتي"
    override val detailedAnalytics = "التحليلات المفصلة"
    override val currentlyReading = "أقرأ حالياً"
    override val myFavorites = "أعمالي المفضلة"
    override val planToRead = "أنوي قراءتها"
    override val tasteDna = "البصمة القرائية والتصنيفات"
    override val readingVibe = "نمط ومزاج القراءة"
    override val noFavoritesYet = "لم تضف أعمالاً إلى المفضلة بعد. اضغط على أيقونة القلب في أي مانغا لإبرازها هنا!"
    override val noPlanToReadYet = "لا توجد مانجا في قائمة الانتظار. أضف أعمالاً تخطط لقراءتها لاحقاً!"
    override val noCurrentReading = "لا توجد مانجا قيد القراءة حالياً. استكشف المكتبة وابدأ مغامرتك القادمة!"

    // Local Network Sync (Arabic)
    override val localSync = "المزامنة عبر الشبكة المحلية"
    override val localSyncDesc = "مزامنة المكتبة وسجل القراءة والإحصائيات مباشرة بين الهاتف والحاسوب على نفس شبكة Wi-Fi دون إنترنت أو خوادم خارجية"
    override val myDevice = "جهازي الحالي"
    override val deviceName = "اسم الجهاز"
    override val serverStatus = "خادم المزامنة المحلي"
    override val serverRunning = "الخادم نشط وجاهز للمزامنة 🟢"
    override val serverStopped = "الخادم متوقف 🔴"
    override val localIp = "عنوان IP المحلي"
    override val scanDevices = "البحث عن أجهزة على نفس الشبكة"
    override val scanning = "جاري البحث عن أجهزة..."
    override val noDevicesFound = "لم يتم العثور على أجهزة أخرى. تأكد من تشغيل خادم المزامنة على الجهاز الآخر واتصال كلا الجهازين بنفس شبكة Wi-Fi أو نقطة الاتصال."
    override val discoveredDevices = "الأجهزة المكتشفة على الشبكة"
    override val syncNow = "مزامنة الآن"
    override val manualConnect = "الاتصال المباشر بعنوان IP"
    override val enterTargetIp = "أدخل عنوان IP للجهاز الآخر (مثال: 192.168.1.50)"
    override val connectAndSync = "اتصال ومزامنة فورية"
    override val syncOptions = "البيانات المراد مزامنتها"
    override val syncLibrary = "المكتبة، التصنيفات، والمفضلة"
    override val syncHistory = "سجل القراءة، الفصول، ورقم الصفحة"
    override val syncStats = "إحصائيات القراءة وسلاسل الأيام"
    override val syncSettings = "المصادر المثبتة وتفضيلات القراءة"
    override val syncSuccess = "تمت المزامنة بنجاح ✓"
    override val syncFailed = "فشلت المزامنة. تأكد من صحة عنوان IP وتشغيل خادم المزامنة."
    override val syncing = "جاري تبادل ومزامنة البيانات..."
    override val lastSync = "آخر مزامنة"

    // Backup & Restore (Arabic)
    override val createBackup = "إنشاء نسخة احتياطية"
    override val createBackupDesc = "تصدير المكتبة وسجل القراءة والإحصائيات والإعدادات إلى ملف محلي"
    override val restoreBackup = "استعادة نسخة احتياطية"
    override val restoreBackupDesc = "استيراد البيانات من ملف نسخة احتياطية سابقة أو عبر نص JSON"
    override val backupCreatedSuccess = "تم إنشاء النسخة الاحتياطية بنجاح ✓"
    override val backupRestoredSuccess = "تمت استعادة النسخة الاحتياطية بنجاح ✓"
    override val backupSavedAt = "تم الحفظ في المسار:"
    override val restoreModeTitle = "اختر أسلوب الاستعادة"
    override val restoreModeMerge = "⚡ دمج ذكي مع البيانات الحالية (مستحسن)"
    override val restoreModeMergeDesc = "إضافة المانجا والفصول وساعات القراءة دون مسح ما قرأته محلياً"
    override val restoreModeOverwrite = "🔄 استبدال كامل (تصفير واستعادة مطابقة)"
    override val restoreModeOverwriteDesc = "استبدال البيانات الحالية بالكامل بمحتويات النسخة الاحتياطية"
    override val backupPreviewTitle = "معاينة النسخة الاحتياطية"
    override val backupDate = "تاريخ النسخة"
    override val backupLibraryItems = "عناصر المكتبة"
    override val backupHistoryEntries = "سجل القراءة"
    override val backupStatsEntries = "إحصائيات القراءة"
    override val localBackupsList = "النسخ الاحتياطية المحفوظة محلياً"
    override val noLocalBackupsFound = "لا توجد نسخ احتياطية محفوظة حالياً."
    override val selectFileFromDevice = "اختيار ملف من ذاكرة الجهاز"
    override val selectFileFromDeviceDesc = "تصفح الذاكرة واختيار ملف النسخة الاحتياطية (JSON) مباشرة"
    override val pasteJsonBackup = "استعادة عبر لصق نص JSON"
    override val pasteJsonPlaceholder = "الصق محتوى النسخة الاحتياطية بتنسيق JSON هنا..."
    override val invalidBackupFormat = "صيغة النسخة الاحتياطية غير صالحة. يرجى التأكد من صحة الملف."
    override val deleteBackupConfirm = "هل أنت متأكد من رغبتك في حذف هذا الملف نهائياً؟"

    // Deep Analytics (Arabic)
    override val deepAnalyticsTitle = "التحليلات والإحصائيات الدقيقة"
    override val deepAnalyticsSubtitle = "تحليل مفصل لسلوك وقراءة المانجا، السرعة، ومخطط الأوقات"
    override val readingPace = "سرعة ومعدل القراءة"
    override val secondsPerPage = "ثانية / صفحة"
    override val minutesPerChapter = "دقيقة / فصل"
    override val longestSession = "أطول جلسة قراءة"
    override val peakReadingTime = "فترة القراءة المفضلة"
    override val morningReader = "قارئ صباحي ☀️"
    override val afternoonReader = "قارئ نهاري 🌤️"
    override val nightReader = "قارئ ليلي 🌙"
    override val allMangaDetailed = "إحصائيات كل عمل بالتفصيل"
    override val sortByTime = "حسب الوقت المستغرق"
    override val sortByChapters = "حسب عدد الفصول"
    override val sortByPages = "حسب عدد الصفحات"
    override val sortByLastRead = "حسب أحدث قراءة"
    override val openDeepAnalytics = "عرض الإحصائيات والتحليلات الدقيقة ⭢"

    override fun searchingInSource(source: String) = "جاري البحث في $source..."
    override fun noResultsFor(query: String) = "لا توجد نتائج مطابقة لـ \"$query\""
    override fun libraryResultsCount(count: Int) = "المكتبة ($count)"
    override fun historyResultsCount(count: Int) = "سجل القراءة ($count)"
    override fun chapterFormat(title: String) = "فصل $title"
    override fun pageFormat(page: Int) = "صفحة $page"
    override fun categoryFormat(cat: String) = "التصنيف: $cat"
    override fun languageFormat(lang: String) = "اللغة: $lang"
}

object EnglishStrings : AppStrings {
    override val appName = "Manga DT"
    override val home = "Home"
    override val library = "Library"
    override val updates = "Updates"
    override val browse = "Browse"
    override val settings = "Settings"
    override val favorites = "Favorites"
    override val history = "History"
    override val popular = "Popular"
    override val latest = "Latest"
    override val searchManga = "Search manga online..."
    override val searchLocal = "Search library & history..."
    override val searchSources = "Search sources..."
    override val onlineMangaMode = "Online Manga"
    override val localLibraryMode = "Library & History"
    override val sourcesMode = "Sources"
    override val sources = "Source"
    override val genre = "Genre"
    override val language = "Language"
    override val all = "All"
    override val continueReading = "Continue Reading"
    override val startReading = "Start Reading"
    override val resumeReading = "Resume"
    override val inLibrary = "In Library"
    override val addToLibrary = "Add to Library"
    override val chapters = "Chapters"
    override val noMangaFound = "No manga available in this section"
    override val retry = "Retry"
    override val statusOngoing = "Ongoing"
    override val statusCompleted = "Completed"
    override val statusOnHold = "On Hold"
    override val statusDropped = "Dropped"
    override val appearance = "Appearance"
    override val colorScheme = "Color scheme"
    override val theme = "Theme"
    override val themeDark = "Dark"
    override val themeLight = "Light"
    override val themeSystem = "Follow System Theme"
    override val themeSystemDesc = "Switch between light and dark based on your device settings"
    override val blackAmoled = "Black"
    override val blackAmoledDesc = "Uses less power on AMOLED screens"
    override val selectThemeTitle = "Select Theme"
    override val darkTheme = "Dark Theme"
    override val nsfwContent = "Show 18+ (NSFW) Content"
    override val nsfwContentDesc = "Allow viewing and searching adult sources"
    override val appLanguage = "App Language"
    override val selectLanguageTitle = "Select App Language"
    override val clearCache = "Clear Cache"
    override val clearCacheSuccess = "Cache Cleared Successfully ✓"
    override val expandSidebar = "Expand Sidebar"
    override val collapseSidebar = "Collapse Sidebar"
    override val selectSourceTitle = "Select Displayed Source"
    override val selectGenreTitle = "Select Genre"
    override val close = "Close"
    override val clear = "Clear"
    override val ok = "OK"
    override val cancel = "Cancel"
    override val notificationsTitle = "Notifications & Updates"
    override val noNotifications = "No new notifications at this time."
    override val notificationsSubtitle = "New chapter updates for your library manga will appear here."
    override val userDefaultName = "Manga DT User"
    override val userSubtitle = "reader@mangadt.app"
    override val typeToSearchLocal = "Type to search inside library & history"
    override val typeToSearchOnline = "Type manga name to search online"
    override val loadingManga = "Loading latest manga and chapters..."
    override val defaultBrowseLayout = "Default Browse Layout"
    override val layoutList = "List View"
    override val layoutComfortable = "Comfortable Grid"
    override val layoutCompact = "Compact Grid"
    override val selectLayoutTitle = "Select Layout Mode"
    override val defaultReadingMode = "Default Reading Mode"
    override val readingModeWebtoon = "Webtoon"
    override val readingModeVerticalPaged = "Vertical Paged"
    override val readingModeLTR = "Left to Right"
    override val readingModeRTL = "Right to Left"
    override val selectReadingModeTitle = "Select Reading Mode"
    override val readerSettings = "Reader Settings"
    override val readerMode = "Reading Mode"
    override val readerBackground = "Background Color"
    override val readerScale = "Image Scaling"
    override val readerScaleFitWidth = "Fit Width"
    override val readerScaleFitHeight = "Fit Height"
    override val readerScaleFitScreen = "Fit Screen"
    override val readerScaleOriginal = "Original Size"
    override val readerBgBlack = "AMOLED Black"
    override val readerBgDarkGray = "Dark Gray"
    override val readerBgWhite = "White"
    override val previousChapter = "Previous Chapter"
    override val nextChapter = "Next Chapter"
    override val chapterList = "Chapter List"
    override val searchChapters = "Search chapters..."
    override val endOfChapter = "End of Chapter"
    override val finishedChapter = "You have completed this chapter"
    override val goToNextChapter = "Go to Next Chapter"
    override val goToPreviousChapter = "Go to Previous Chapter"
    override val noMoreChapters = "You are on the latest chapter"
    override val goToPage = "Go to Page"
    override val pageIndicator = "Page Number Indicator"
    override val pageIndicatorDesc = "Show floating page bubble and clock while reading"
    override val pageFailedToLoad = "Page failed to load"
    override val tapToRetry = "Tap to retry"
    override val keepScreenOn = "Keep Screen On"
    override val keepScreenOnDesc = "Prevent device screen from sleeping while reading"
    override val cropBorders = "Crop Image Borders"
    override val cropBordersDesc = "Automatically trim empty white borders around pages"
    override val pinnedSourcesTitle = "Pinned Sources"
    override val noPinnedSources = "No sources pinned yet"
    override val clearAllPinned = "Clear All Pinned"
    override val versionText = "Version 1.0.0 • Kotatsu Multiplatform"
    override val autoConfigured = "These settings are configured automatically."
    override val storageUsageDesc = "Covers and chapters disk cache"
    override val captchaRequired = "This source requires human verification (Captcha / Cloudflare) to proceed"
    override val solveCaptcha = "Solve in Embedded WebView"
    override val captchaSolved = "Verification Solved Successfully ✓"
    override val captchaHelpText = "Please solve the human verification test on the page, then click \"I have solved it\""
    override val iHaveSolvedIt = "I have solved it"

    // Profile & Statistics (English)
    override val profile = "Profile"
    override val statistics = "Statistics"
    override val profileAndStats = "Profile & Activity"
    override val totalReadingTime = "Reading Time"
    override val chaptersRead = "Chapters Read"
    override val pagesRead = "Pages"
    override val mangaRead = "Titles"
    override val readingStreak = "Reading Streak"
    override val streakDays = "Days Streak"
    override val bestStreak = "Best Streak"
    override val dailyAverage = "Daily Average"
    override val averageTimePerChapter = "Avg Time / Chapter"
    override val rank = "Reader Rank"
    override val rankNovice = "Novice"
    override val rankReader = "Active Reader"
    override val rankEnthusiast = "Manga Enthusiast"
    override val rankOtaku = "Otaku Master"
    override val rankLegend = "Manga Legend"
    override val topGenres = "Top Genres"
    override val weeklyActivity = "Weekly Activity"
    override val mangaStatisticsTitle = "Per-Manga Statistics"
    override val timeSpent = "Time Spent"
    override val lastRead = "Last Read"
    override val completedChapters = "Completed Chapters"
    override val editProfile = "Edit Profile"
    override val username = "Username"
    override val bio = "Bio"
    override val save = "Save Changes"
    override val privacyLocalNotice = "100% Offline & Local. Your activity and stats are kept strictly on your device under GPL open-source license with zero external servers."
    override val noStatsYet = "No reading data recorded yet. Start reading any manga to automatically track your activity!"
    override val clearStats = "Reset Statistics"
    override val clearStatsConfirm = "Are you sure you want to delete all locally stored reading statistics?"

    // Profile Experience (English)
    override val overviewAndActivity = "Overview & Taste"
    override val detailedAnalytics = "Deep Analytics"
    override val currentlyReading = "Currently Reading"
    override val myFavorites = "My Favorites"
    override val planToRead = "Plan to Read"
    override val tasteDna = "Taste DNA & Genres"
    override val readingVibe = "Reading Vibe"
    override val noFavoritesYet = "No favorites added yet. Tap the heart on any manga to showcase it here!"
    override val noPlanToReadYet = "No manga in your reading queue. Add titles you plan to read later!"
    override val noCurrentReading = "No manga currently being read. Discover titles and start your next adventure!"

    // Local Network Sync (English)
    override val localSync = "Local Network Sync"
    override val localSyncDesc = "Sync library, history and stats directly between phone and PC on the same Wi-Fi with zero external servers"
    override val myDevice = "This Device"
    override val deviceName = "Device Name"
    override val serverStatus = "Local Sync Server"
    override val serverRunning = "Server Running & Ready 🟢"
    override val serverStopped = "Server Stopped 🔴"
    override val localIp = "Local IP Address"
    override val scanDevices = "Scan for Nearby Devices"
    override val scanning = "Scanning for devices..."
    override val noDevicesFound = "No other devices found. Ensure sync server is running on the other device and both are on the same Wi-Fi or hotspot."
    override val discoveredDevices = "Discovered Devices on LAN"
    override val syncNow = "Sync Now"
    override val manualConnect = "Direct IP Connection"
    override val enterTargetIp = "Enter target device IP (e.g. 192.168.1.50)"
    override val connectAndSync = "Connect & Sync Now"
    override val syncOptions = "Items to Synchronize"
    override val syncLibrary = "Library, Categories & Favorites"
    override val syncHistory = "Reading History & Chapter Progress"
    override val syncStats = "Reading Stats & Streaks"
    override val syncSettings = "Pinned Sources & Preferences"
    override val syncSuccess = "Sync Completed Successfully ✓"
    override val syncFailed = "Sync failed. Check target IP and ensure sync server is enabled."
    override val syncing = "Transferring and merging data..."
    override val lastSync = "Last Synced"

    // Backup & Restore (English)
    override val createBackup = "Create Backup"
    override val createBackupDesc = "Export library, reading history, statistics, and settings to a local file"
    override val restoreBackup = "Restore Backup"
    override val restoreBackupDesc = "Import data from a previous backup file or via JSON text"
    override val backupCreatedSuccess = "Backup Created Successfully ✓"
    override val backupRestoredSuccess = "Backup Restored Successfully ✓"
    override val backupSavedAt = "Saved at:"
    override val restoreModeTitle = "Select Restore Mode"
    override val restoreModeMerge = "⚡ Smart Merge with Current Data (Recommended)"
    override val restoreModeMergeDesc = "Add new manga, chapters, and stats without deleting local progress"
    override val restoreModeOverwrite = "🔄 Full Overwrite (Replace All)"
    override val restoreModeOverwriteDesc = "Completely replace current local data with the backup contents"
    override val backupPreviewTitle = "Backup Preview"
    override val backupDate = "Backup Date"
    override val backupLibraryItems = "Library Items"
    override val backupHistoryEntries = "Reading History"
    override val backupStatsEntries = "Reading Statistics"
    override val localBackupsList = "Locally Saved Backups"
    override val noLocalBackupsFound = "No backup files saved yet."
    override val selectFileFromDevice = "Select Backup File from Device"
    override val selectFileFromDeviceDesc = "Browse device storage and select a JSON backup file directly"
    override val pasteJsonBackup = "Restore via Paste JSON Text"
    override val pasteJsonPlaceholder = "Paste backup JSON content here..."
    override val invalidBackupFormat = "Invalid backup format. Please verify the file content."
    override val deleteBackupConfirm = "Are you sure you want to delete this backup file?"

    // Deep Analytics (English)
    override val deepAnalyticsTitle = "Deep Analytics"
    override val deepAnalyticsSubtitle = "Comprehensive breakdown of reading speed, completed chapters, and hours"
    override val readingPace = "Reading Pace"
    override val secondsPerPage = "sec / page"
    override val minutesPerChapter = "min / chapter"
    override val longestSession = "Longest Reading Session"
    override val peakReadingTime = "Peak Reading Time"
    override val morningReader = "Morning Reader ☀️"
    override val afternoonReader = "Afternoon Reader 🌤️"
    override val nightReader = "Night Owl 🌙"
    override val allMangaDetailed = "All Manga Detailed Stats"
    override val sortByTime = "By Time Spent"
    override val sortByChapters = "By Chapters"
    override val sortByPages = "By Pages"
    override val sortByLastRead = "By Last Read"
    override val openDeepAnalytics = "View Deep Analytics ⭢"

    override fun searchingInSource(source: String) = "Searching in $source..."
    override fun noResultsFor(query: String) = "No results found for \"$query\""
    override fun libraryResultsCount(count: Int) = "Library ($count)"
    override fun historyResultsCount(count: Int) = "History ($count)"
    override fun chapterFormat(title: String) = "Ch. $title"
    override fun pageFormat(page: Int) = "Page $page"
    override fun categoryFormat(cat: String) = "Category: $cat"
    override fun languageFormat(lang: String) = "Language: $lang"
}
