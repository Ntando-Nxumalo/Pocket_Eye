# Pocket Eye - Smart Expense Tracker

## 📱 Demonstration Video
[**Click Here to View the Demo Video**](ADD_YOUR_YOUTUBE_LINK_HERE)
*Note: Video demonstrates the app running on a physical Android device with voice-over explanation.*

## 📸 Screenshots
<p align="center">
  <img src="https://via.placeholder.com/200x400?text=Dashboard" width="200">
  <img src="https://via.placeholder.com/200x400?text=Reports" width="200">
  <img src="https://via.placeholder.com/200x400?text=Achievements" width="200">
</p>

## 🚀 GitHub Actions CI/CD
This project uses **GitHub Actions** for Automated CI/CD.
- **Workflow**: `.github/workflows/build.yml`
- **Functions**: Automatically builds the APK (`assembleDebug`) and runs all Unit Tests (`test`) on every push to the `main` branch.
- **Badge**: ![Android Build](https://github.com/YOUR_USERNAME/PocketEye/actions/workflows/build.yml/badge.svg)

## 🌟 Custom Features (Part 3)

### Feature 1: AI Chat Assistant (PocketEye Bot)
A built-in assistant that uses natural language to answer questions about your balance, progress towards goals, and provides tailored saving tips based on your spending history.

### Feature 2: Integrated Multi-Currency Converter
A real-time tool on the dashboard to quickly convert expenses between ZAR, USD, EUR, GBP, AUD, and CNY. This allows users to track international spending accurately without leaving the app.

## 🛠 Project Overview
Pocket Eye is a premium financial management application designed to help users take control of their spending through intelligent tracking, goal setting, and data-driven insights.

### Key Features
- **Smart Expense Capture**: Log expenses with metadata and photo receipts.
- **Budgeting & Goal Setting**: Define min/max spending limits per category.
- **Dynamic Reporting**: Interactive Bar Charts with Limit Lines for goals.
- **Gamification**: RPG-style Leveling (XP) and Achievement badges.

## 🏗 Technical Details
- **Architecture**: MVVM with Repository pattern.
- **Database**: Room Persistence Library.
- **UI**: Jetpack Compose (Modern UI) + XML.
- **Charts**: MPAndroidChart.

## 🔧 Installation
1. Clone the repository: `git clone https://github.com/YOUR_USERNAME/PocketEye.git`
2. Open in Android Studio (Ladybug or newer).
3. Connect a physical Android device with USB Debugging enabled.
4. Run the app using the 'Run' button.
