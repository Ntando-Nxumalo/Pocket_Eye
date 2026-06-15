# Pocket Eye - Smart Expense Tracker 👁️💰

Pocket Eye is a premium financial management application designed to help users take control of their spending through intelligent tracking, goal setting, and data-driven insights. It combines modern UI with gamification to make personal finance engaging and simple.

## 📱 Demonstration Video
[**Click Here to View the Demo Video**](ADD_YOUR_YOUTUBE_LINK_HERE)
*Note: Video demonstrates the app running on a physical Android device with voice-over explanation.*

## 📸 Screenshots
<p align="center">
  <img src="https://via.placeholder.com/200x400?text=Dashboard" width="200" alt="Dashboard">
  <img src="https://via.placeholder.com/200x400?text=Reports" width="200" alt="Reports">
  <img src="https://via.placeholder.com/200x400?text=Achievements" width="200" alt="Achievements">
</p>

## 🚀 GitHub Actions CI/CD
This project uses **GitHub Actions** for Automated CI/CD.
- **Workflow**: `.github/workflows/build.yml`
- **Functions**: Automatically builds the APK (`assembleDebug`) and runs all Unit Tests (`test`) on every push to the `main` branch.
- **Badge**: ![Android Build](https://github.com/YOUR_USERNAME/PocketEye/actions/workflows/build.yml/badge.svg)

## 🌟 Custom Features

### 🤖 AI Chat Assistant (PocketEye Bot)
A built-in assistant that uses natural language to answer questions about your balance, progress towards goals, and provides tailored saving tips based on your spending history. It can analyze your data to give you quick summaries without navigating through menus.

### 💱 Integrated Multi-Currency Converter
A real-time tool on the dashboard to quickly convert expenses between **ZAR, USD, EUR, GBP, AUD, and CNY**. This is essential for tracking international spending or planning trips without leaving the app.

### 🎮 Gamified Experience
Finance doesn't have to be boring. Pocket Eye includes an RPG-style leveling system:
- **XP System**: Earn 20 XP for every expense logged.
- **Leveling**: Reach 100 XP (5 expenses) to level up!
- **Celebrations**: Special dialogs and badges when you hit milestones or level up.
- **Badges**: Unlock achievements like "First Step", "Week Warrior", and "Budget Boss".

### 📊 Dynamic Reporting & Budgeting
- **Interactive Charts**: Powered by MPAndroidChart and Jetpack Compose Canvas, providing visual breakdowns of spending.
- **Limit Lines**: Budget goals are visualized with limit lines in reports to show how close you are to your limits.
- **Monthly Budgeting**: Set min/max targets for specific categories or your overall monthly spend.

## 🏗 Technical Details
- **Architecture**: MVVM (Model-View-ViewModel) with a robust Repository pattern for clean separation of concerns.
- **UI Framework**: A hybrid approach using **Jetpack Compose** for modern, high-performance UI components and **XML Layouts** for structured activity views.
- **Database**: **Room Persistence Library** with Flow support for real-time data updates.
- **Navigation**: Custom **Radial Navigation Menu** for a unique and fluid user experience.
- **Concurrency**: **Kotlin Coroutines** and **Flow** for reactive data handling and smooth background operations.

## 🔧 Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/PocketEye.git
   ```
2. Open the project in **Android Studio** (Ladybug 2024.2.1 or newer recommended).
3. Ensure you have the **Android SDK 35** installed.
4. Sync the project with Gradle files.
5. Connect a physical Android device or launch an emulator.
6. Build and run the `app` module.

## 📦 Key Dependencies
- `androidx.room`: Local data storage.
- `androidx.compose`: Modern UI toolkit.
- `com.github.PhilJay:MPAndroidChart`: Advanced charting.
- `androidx.lifecycle`: ViewModel and Lifecycle management.
- `kotlinx.coroutines`: Asynchronous programming.
