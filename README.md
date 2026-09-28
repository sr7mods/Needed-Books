<div align="center">

# 📚 Needed Books

**A Modern, High-Performance Android PDF Reader & Book Manager**

<!-- GitHub Shields / Badges Card -->
<p>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%20Native-3DDC84?style=for-the-badge&logo=android&logoColor=black" alt="Android Platform"></a>
  <a href="https://m3.material.io/"><img src="https://img.shields.io/badge/Design-Glassmorphic%20%7C%20Dark-00B0FF?style=for-the-badge&logo=material-design&logoColor=white" alt="UI Style"></a>
  <a href="https://firebase.google.com/"><img src="https://img.shields.io/badge/Backend-Firebase%20REST-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase Backend"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-FF1744?style=for-the-badge&logo=gnu&logoColor=white" alt="License GPL 3.0"></a>
  <a href="https://t.me/sr7mods"><img src="https://img.shields.io/badge/Dev-SR7%20Mods-00E676?style=for-the-badge&logo=github&logoColor=black" alt="Developer Handle"></a>
</p>

</div>

---

## 🌟 Key Features

* **🚀 Upgraded Native PDF Engine**: Powered by `AndroidPdfViewer` with fluid vertical continuous scrolling, double-tap multi-level zooming, and anti-aliasing.
* **🔖 Smart Bookmark Manager**: Save any book page instantly with single-tap bookmarking, browse saved bookmarks in a glass dialog, and jump directly to any bookmark.
* **⏩ Quick Jump to Page**: Jump to any target page seamlessly via direct number input or interactive slider.
* **🌙 Color Inversion Dark Mode**: Hardware-accelerated color matrix inverter specifically tuned for reading white documents comfortably at night without eye strain.
* **📊 Comprehensive Download Overlay**: Real-time progress tracking with percentage, live transfer speeds, downloaded size, total file size (`Downloaded / Actual Size`), and remaining size display.
* **🎨 Glassmorphic & Modern UI**: Sleek Material 3 dark-themed interface built entirely with declarative Jetpack Compose.
* **🗂️ Smooth Scrolling Navigation Drawer**: Developer portfolio, community links (Telegram, WhatsApp, Facebook), language selector (English / Bangla), cache cleaner, and instructions.
* **🧹 Lightweight & Optimized**: Unused legacy dependencies and deprecated architectures stripped out, reducing APK footprint and memory overhead.

---

## 📊 Tech Stack Breakdown

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Language** | **Kotlin (100%)** | Modern, idiomatic Kotlin coroutines, flows, and type safety |
| **UI Framework** | **Jetpack Compose (M3)** | Declarative UI, glassmorphism styling, fluid animations, custom modals |
| **PDF Rendering** | **AndroidPdfViewer (Pdfium)** | Native C++ Pdfium-backed vector decoding, double-tap zoom & pinch gestures |
| **Networking** | **HttpURLConnection & Coroutines** | Resilient background file streaming with chunked byte-size calculations |
| **Backend & Sync** | **Firebase REST** | Dynamic catalog fetch with local caching and offline cache management |

---

## 📁 Project Structure

```text
NeededBooks/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/needed/books/
│   │       │   ├── MainActivity.kt          # Application entry point & theme initialization
│   │       │   ├── NativePdfActivity.kt     # Dedicated PDF reader activity container
│   │       │   ├── BookModel.kt             # Data models & Firebase JSON parser
│   │       │   ├── LocaleHelper.kt          # Dynamic language switching (EN / BN)
│   │       │   ├── data/
│   │       │   │   └── BookRepository.kt    # Firebase fetch, caching, and stream downloader
│   │       │   ├── pdf/
│   │       │   │   └── PdfViewerScreen.kt   # PDFView compose integration, bookmarks, night mode & jump
│   │       │   ├── security/
│   │       │   │   └── SecurityVault.kt     # Secure app configuration
│   │       │   └── ui/
│   │       │       ├── MainScreen.kt        # Book grid/list, sidebar drawer, download overlay & dialogs
│   │       │       └── theme/               # Color palette, Material3 theme & typography
│   │       ├── res/
│   │       │   ├── values/                  # Strings, colors, and base themes
│   │       │   ├── values-bn/               # Full Bengali localization resources
│   │       │   └── drawable/                # Optimized vector icons and visual assets
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE
└── README.md
