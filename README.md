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
text com.ntando.expensetracker ├── data/ │   ├── dao/           # Room DAOs for Database access │   ├── database/      # Room Database Configuration │   ├── entity/        # Database models (User, Expense, Goal, etc.) │   └── repository/    # Data source abstraction layer ├── viewmodel/         # Expense and User ViewModels ├── SplashActivity     # Animated entry point ├── MainActivity       # Login/Authentication entry ├── Dashboard          # Main hub with charts and stats ├── AddExpenseActivity # Dedicated page to log expenses ├── SetGoalsActivity   # Manage savings goals └── ReportsActivity    # Financial analysis views



## ⚙️ Setup Instructions

### Prerequisites
- **Android Studio Ladybug (2024.2.1)** or newer.
- **JDK 17** or higher.
- **Android SDK 34** (API Level 34) or higher.

### Step-by-Step Setup
1.  **Clone the Repository**:
    
