# 📖 Manga DT (مانـغـا DT)

A modern, fast, and feature-rich **Manga & Comics Reader** built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting Android and Desktop (Linux/Windows/macOS). Powered by the open-source Kotatsu parser engine.

---

## ✨ Features

- 🌐 **Extensive Manga Sources**: Access dozens of online manga, webtoon, and comic sources with search, filters, and latest updates.
- 🎨 **Adaptive Material 3 Design**: Expressive dynamic color palette, AMOLED pure black mode, and automatic **Follow System Dark/Light Theme**.
- 👤 **Personalized User Profile**: Showcase your reading persona, avatar, reader rank, streak badges, favorites, and "Plan to Read" queue.
- 📊 **Deep Analytics & Reading Statistics**:
  - Detailed breakdown of total reading time, completed chapters, and pages turned.
  - Reading pace calculation (seconds/page, minutes/chapter).
  - Peak reading time habits (Morning, Afternoon, or Night reader).
  - Interactive 7-Day activity bar chart & streak calendar.
  - Per-manga searchable & sortable deep statistics.
- 🔄 **Local Network Sync (Zero-Cloud)**:
  - Synchronize library, history, reading progress, and stats directly between PC and phone over the local Wi-Fi / LAN without internet or third-party servers.
  - Binary socket protocol with automatic LAN peer discovery.
- 📦 **Kotatsu-Style Backup & Restore**:
  - Export complete data into JSON backup files.
  - Native SAF File Picker on Android & Desktop file dialog.
  - Compatible with Kotatsu JSON format.
  - **Smart Merge Mode** (non-destructive union) & **Full Overwrite Mode**.
- 🌍 **Bilingual Localization**: Seamless Arabic and English support with automatic **System Language Detection**.
- 🛡️ **100% Offline & Private**: All history, library, and stats are stored strictly locally on your device under the GPL license.

---

## 🛠️ Tech Stack

- **Kotlin Multiplatform (KMP)** & **Compose Multiplatform**
- **Material 3 Adaptive UI**
- **Coil 3** for image loading & disk caching
- **Kotatsu Parsers Engine**
- **Coroutines & Flow**

---

## 🚀 Building and Running

### Android
```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

### Desktop (Linux / macOS / Windows)
```bash
./gradlew :desktopApp:run
```

---

## 📄 License
This project is open-source under the [GNU General Public License v3.0 (GPL-3.0)](https://www.gnu.org/licenses/gpl-3.0.html).