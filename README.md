
# 📱 NBackTaskApp

An Android application developed for cognitive load research using the N-back task. The app records user interactions, accelerometer data, and difficulty ratings to assess mental effort during smartphone usage.

---

## 🚀 Getting Started

### 🧰 Prerequisites

- Android Studio
- Android SDK (Minimum API Level: **26 - Android 8.0**)

### ⚙️ Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/mtayyabali/NBackTaskApp.git
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click on **"Open an existing project"**
   - Select the cloned `NBackTaskApp` folder

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click ▶️ **Run** to build and deploy the app

---

## 📂 Project Structure

Thanks for sharing the updated project structure! Based on your screenshot, here's the **modified "Project Structure" section** in proper Markdown format for your README, reflecting the actual Kotlin structure and updated file names/locations:

---

### 📂 Project Structure

```
NBackTaskApp/
├── app/
   └── src/
       └── main/
           ├── java/
           │   └── com.example.nbacktask.ui/
           │       ├── MainActivity.kt         # Launch screen, Core N-back task logic and navigation
           │       └── RatingScreen.kt         # Ratings page after each task
           ├── res/
           │   ├── drawable/                   # App icons and images
           │   ├── layout/                     # XML UI layouts
           │   ├── mipmap-[dpi]/               # Launcher icons (various resolutions)
           │   ├── values/                     # Strings, colors, dimensions
           │   └── xml/                        # Other XML resources
           └── AndroidManifest.xml             # App manifest file

```

---

## 📊 Data Collected

Each task generates **three types of data**, saved locally as `.csv` files:

- **Performance Data**: Accuracy and reaction time  
- **Accelerometer Data**: Raw values recorded continuously  
- **Ratings**: User-reported difficulty on a **1–7 scale**

---

## 🧑‍💻 How to Use

1. Launch the app on your Android device.
2. Complete the N-back task for the presented level.
3. After each task, rate the difficulty on the **Ratings Page**.
4. Repeat until all three levels are completed.
5. Export the `.csv` files from the device for data analysis.

---

