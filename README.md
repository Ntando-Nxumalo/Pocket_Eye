# Pocket Eye - Smart Expense Tracker 👁️💰

Pocket Eye is a premium financial management application designed to help users take control of their spending through intelligent tracking, goal setting, and data-driven insights. It combines modern UI with gamification to make personal finance engaging and simple.

## 🎯 Purpose & Audience
**What the app does:**
Pocket Eye simplifies personal finance by allowing users to log expenses, categorize spending, and set both savings goals and budget limits. It provides visual feedback through dynamic charts and rewards consistency with a leveling system.

**Who it's for:**
The app is designed for students, young professionals, and anyone looking to move away from messy spreadsheets. It's particularly useful for those who want a "gamified" approach to saving money or frequently deal with multiple currencies.

## 📱 Demonstration Video
[**Click Here to View the Demo Video**](ADD_YOUR_YOUTUBE_LINK_HERE)
*Note: Video demonstrates the app running on a physical Android device with voice-over explanation.*

## 🎨 Design Decisions
- **MVVM Architecture**: Chosen to ensure a clean separation between UI logic and data handling, making the app easier to test and maintain.
- **Hybrid UI Strategy**: I used **Jetpack Compose** for complex, state-driven components like the circular charts and budget progress bars because of its declarative power. Traditional **XML Layouts** were kept for standard Activity structures where they provided more stability.
- **Material 3 Design**: Leveraged the latest Material Design components for a modern, accessible, and "premium" feel.
- **Repository Pattern**: Centralizes data access from the Room database, providing a single source of truth for the ViewModels.

## 🛠 GitHub & GitHub Actions
- **Version Control**: GitHub was used for source control, following a feature-branch workflow to keep the `main` branch stable.
- **CI/CD Automation**: I implemented **GitHub Actions** to automate the build and testing process.
    - **Workflow**: `.github/workflows/build.yml`
    - **Functions**: Every push to the `main` branch triggers an automated build (`assembleDebug`) and runs all Unit Tests. This ensures that no "breaking" code is merged without being caught by the CI runner.
- **Transparency**: Commit history reflects the iterative development process, with clear messages for each feature addition.

## 🌟 Custom Features

### 1. 🤖 AI Chat Assistant (PocketEye Bot)
A built-in assistant that uses natural language to answer questions about your balance, progress towards goals, and provides tailored saving tips based on your spending history. It can analyze your data to give you quick summaries without navigating through menus.

### 2. 💱 Integrated Multi-Currency Converter
A real-time tool on the dashboard to quickly convert expenses between **ZAR, USD, EUR, GBP, AUD, and CNY**. This is essential for tracking international spending or planning trips without leaving the app. It uses a custom ViewModel to handle conversion logic instantly as the user types.

## 🏗 Technical Details
- **UI Framework**: Jetpack Compose & XML (Hybrid).
- **Database**: **Room Persistence Library** with Flow support for real-time updates.
- **Navigation**: Custom **Radial Navigation Menu** for a unique and fluid user experience.
- **Concurrency**: **Kotlin Coroutines** and **Flow** for reactive data handling.

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
