<div align="center">

# 📚 Needed Books

**A High-Performance, Lightweight Native Android PDF Reader & Book Manager**

<!-- GitHub Shields / Badges Card -->
<p>
  <a href="https://github.com/sr7mods"><img src="https://img.shields.io/badge/Language-Kotlin%2070%25%20%7C%20XML%2030%25-007ACC?style=for-the-badge&logo=java&logoColor=white" alt="Languages"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%20Native-3DDC84?style=for-the-badge&logo=android&logoColor=black" alt="Android Platform"></a>
  <a href="https://m3.material.io/"><img src="https://img.shields.io/badge/UI-Glassmorphism%20%7C%20Dark-00B0FF?style=for-the-badge&logo=material-design&logoColor=white" alt="UI Style"></a>
  <a href="https://firebase.google.com/"><img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase Backend"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-FF1744?style=for-the-badge&logo=gnu&logoColor=white" alt="License GPL 3.0"></a>
  <a href="https://t.me/sr7mods"><img src="https://img.shields.io/badge/Dev-SR7%20Mods-00E676?style=for-the-badge&logo=github&logoColor=black" alt="Developer Handle"></a>
</p>

</div>

---

## 🌟 Key Features

* **🎨 Glassy & Modern UI**: Pure XML drawables dwara design kiya gaya Dark Mode UI aur Glassmorphic Notice Dialog.
* **⚡ High-Performance Engine**: Memory-managed native `ListView` rendering, jisse badi PDF files bina kisi lag ke smoothly scroll hoti hain.
* **📥 Dynamic Download Overlay**: Real-time download progress bar ke saath downloading speed (KB/s ya MB/s) aur file size tracker.
* **🌙 Dark / Light Mode Toggle**: PDF reader view ke andar single-tap layout color inverter.
* **🗂️ Interactive Side Drawer**: Social links, Developer Portfolio, Telegram channel aur Support options.
* **⚠️ Custom Notice Dialog**: App opening notice with "Don't Show Again" preference state check.

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
Agra aap Termux ya Terminal se build karna chahte hain:
```bash
# Clean previous build caches
./gradlew clean

# Assemble Release APK
./gradlew assembleRelease

```
Generated APK location:
app/build/outputs/apk/release/app-release.apk
## 🔒 License & Copyright
Ye project **GNU General Public License v3.0 (GPL-3.0)** ke under protected hai.
 * **Strict Requirement**: Koi bhi is code ko commercial project me bina source code public kiye ya original copyright credit (**SR7 MODS**) hataye reuse/copy-paste nahi kar sakta.
## 👨‍💻 Developer Support
<p align="left">
<a href="https://t.me/sr7mods"><img src="https://img.shields.io/badge/Telegram-@sr7mods-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram"></a>
<a href="https://github.com/sr7mods"><img src="https://img.shields.io/badge/GitHub-SR7--Mods-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub"></a>
</p>
```

