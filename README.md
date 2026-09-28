<div align="center">

# 📚 Needed Books

**A High-Performance, Lightweight Native Android PDF Reader & Book Manager**

<!-- GitHub Shields / Badges Card -->
<p>
  <a href="https://github.com/sr7mods"><img src="https://img.shields.io/badge/Language-Kotlin%2070%25%20%7C%20XML%2030%25-007ACC?style=for-the-badge&logo=java&logoColor=white" alt="Languages"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%20Native-3DDC84?style=for-the-badge&logo=android&logoColor=black" alt="Android Platform"></a>
  <a href="https://m3.material.io/"><img src="https://img.shields.io/badge/UI-Glassmorphism%20%7C%20Dark%20%7C%20Green-00B0FF?style=for-the-badge&logo=material-design&logoColor=white" alt="UI Style"></a>
  <a href="https://firebase.google.com/"><img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase Backend"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-FF1744?style=for-the-badge&logo=gnu&logoColor=white" alt="License GPL 3.0"></a>
  <a href="https://t.me/sr7mods"><img src="https://img.shields.io/badge/Dev-SR7%20Mods-00E676?style=for-the-badge&logo=github&logoColor=black" alt="Developer Handle"></a>
</p>

</div>

---

## 🌟 Key Features

* **🎨 Glassy & Modern UI**: Dark Mode UI and Glassmorphic Notice Dialog designed purely with XML drawables.
* **⚡ High-Performance Engine**: Memory-managed native `ListView` rendering that allows large PDF files to scroll smoothly without any lag.
* **📥 Dynamic Download Overlay**: File size tracker with real-time download progress bar and speed display (KB/s or MB/s).
* **🌙 Dark / Light Mode Toggle**: Single-tap layout color inverter inside the PDF reader view.
* **🗂️ Interactive Side Drawer**: Social links, Developer Portfolio, Telegram channel, and Support options.
* **⚠️ Custom Notice Dialog**: App opening notice featuring a "Don't Show Again" preference state check.

---

## 📊 Language & Tech Stack Breakdown

| Technology | Usage Ratio | Role |
| :--- | :--- | :--- |
| **Java** | `60%` | Application logic, PDF rendering, file downloading & Firebase management |
| **XML** | `40%` | Custom UI components, Glassmorphism layouts, vector icons & drawables |
| **Kotlin / Jetpack Compose** | `0%` | Not used (Pure Native Android Architecture) |

---

## 📁 Project Structure

```text
NeededBooks/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/needed/books/
│   │       │   ├── MainActivity.java
│   │       │   └── NativePdfActivity.java
│   │       ├── res/
│   │       │   ├── drawable/
│   │       │   │   ├── btn_glass_primary.xml
│   │       │   │   ├── btn_glass_secondary.xml
│   │       │   │   ├── custom_progress_bar.xml
│   │       │   │   ├── drawer_glass_bg.xml
│   │       │   │   └── glass_dialog_bg.xml
│   │       │   └── layout/
│   │       │       ├── activity_main.xml
│   │       │       ├── activity_native_pdf.xml
│   │       │       ├── book_item.xml
│   │       │       └── dialog_glass_notice.xml
│   │       └── AndroidManifest.xml
├── LICENSE
└── README.md

```
## ⚡ How to Build (Termux / CLI)
If you want to build using Termux or Terminal:
```bash
# Clean previous build caches
./gradlew clean

# Assemble Release APK
./gradlew assembleRelease

```
Generated APK location:
app/build/outputs/apk/release/app-release.apk
## 🔒 License & Copyright
This project is protected under the **GNU General Public License v3.0 (GPL-3.0)**.
 * **Strict Requirement**: Anyone reusing or copying code from this project cannot use it in commercial projects without making the source code public or removing the original copyright credit (**SR7 MODS**).
## 👨‍💻 Developer Support
<p align="left">
<a href="https://t.me/sr7mods"><img src="https://img.shields.io/badge/Telegram-@sr7mods-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram"></a>
<a href="https://github.com/sr7mods"><img src="https://img.shields.io/badge/GitHub-SR7--Mods-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub"></a>
</p>
```
