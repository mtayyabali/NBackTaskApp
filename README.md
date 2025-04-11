
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

```
NBackTaskApp/
├── java/
│   └── com.example.nbacktask/
│       ├── MainActivity.java           # Launch screen and navigation logic
│       ├── TaskActivity.java           # Core N-back task logic
│       ├── RatingsActivity.java        # User ratings after each task
│       ├── AccelerometerService.java   # Sensor data collection
│       └── DataLogger.java             # CSV file handling and storage
├── res/
│   ├── layout/                         # XML UI layouts
│   └── values/                         # Strings, colors, dimensions
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

