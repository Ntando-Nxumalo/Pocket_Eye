# Pocket Eye - Smart Expense Tracker

Pocket Eye is a modern Android application designed to help users track their daily expenses, set savings goals, and visualize their spending habits. Built with Kotlin and the latest Android development tools, it provides a seamless and intuitive experience for personal finance management.

## 🚀 Features

- **Expense Tracking**: Easily log your daily spending with categories, notes, and custom categories.
- **Savings Goals**: Set and monitor progress towards your financial milestones.
- **Dynamic Dashboard**: View your current balance, XP/Level progress, and recent transactions at a glance.
- **Spending Insights**: Visualize your spending breakdown with interactive charts powered by Jetpack Compose.
- **XP & Gamification**: Earn XP for every transaction logged to stay motivated!
- **User Authentication**: Secure your data with personal accounts (Login/Registration).
- **Reports**: Generate detailed reports to analyze your financial health over time.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Charts & Modern UI) & Android XML
- **Database**: Room Persistence Library (SQLite)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Asynchronous Processing**: Kotlin Coroutines & Flow
- **Components**: ViewModel, Lifecycle, Navigation, Material Design 3

## 📦 Project Structure

```text
com.ntando.expensetracker
├── data/
│   ├── dao/           # Room DAOs for Database access
│   ├── database/      # Room Database Configuration
│   ├── entity/        # Database models (User, Expense, Goal, etc.)
│   └── repository/    # Data source abstraction layer
├── viewmodel/         # Expense and User ViewModels
├── SplashActivity     # Animated entry point
├── MainActivity       # Login/Authentication entry
├── Dashboard          # Main hub with charts and stats
├── AddExpenseActivity # Dedicated page to log expenses
├── SetGoalsActivity   # Manage savings goals
└── ReportsActivity    # Financial analysis views
```

## ⚙️ Setup Instructions

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/yourusername/pocket-eye.git
    ```
2.  **Open in Android Studio**:
    - Launch Android Studio.
    - Select **File > Open** and choose the `PocketEye` folder.
3.  **Sync Gradle**:
    - Click on **Sync Project with Gradle Files** icon in the toolbar.
    - Wait for the build process to finish and download all dependencies.
4.  **Database Configuration**:
    - The app uses **Room Database**. On the first run, it will automatically create an `expense_database` file.
    - Pre-defined categories (*Food, Shopping, Bills, Transport, Other*) are automatically seeded into the database upon initialization.
5.  **Run the App**:
    - Connect your physical Android device via USB (with Developer Options and USB Debugging enabled) or start an Android Emulator.
    - Click the **Run 'app'** button (green play icon) in the toolbar.

## 📱 How to Use

1.  **Splash Screen**: Wait for the Pocket Eye splash animation to finish.
2.  **Registration**: New users should tap **Register** to create an account.
3.  **Login**: Enter your username/email and password to access your dashboard.
4.  **Add Expense**: Tap the **Expense** button in the bottom navigation bar to record a new transaction.
5.  **Set Goals**: Tap the **Goals** button to create a new savings target.
6.  **Charts**: Tap on the **Savings Goals** or **Spending Breakdown** charts on the dashboard for detailed views or updates.

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

---
Built with ❤️ by Ntando
