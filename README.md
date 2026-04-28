# Pocket Eye – Smart Expense Tracker with Gamified Insights

Pocket Eye is a modern Android application designed to transform how users manage their finances by combining **expense tracking with gamification**. Instead of simply recording transactions, Pocket Eye motivates users to build better financial habits through rewards, progress tracking, and real-time insights.

Built using Kotlin and modern Android architecture, the app delivers a fast, intuitive, and engaging experience for personal finance management.

---

## 🎯 Core Idea

Most finance apps are passive — they track what you spend.

Pocket Eye is **active** — it encourages better behavior.

By introducing XP, levels, and visual progress systems, users are incentivized to stay consistent and aware of their spending habits.

---

## 🚀 Features

### 💸 Expense Tracking
- Log daily expenses with categories, notes, and timestamps  
- Create and manage custom categories  
- Edit and delete transactions seamlessly  

### 🎯 Savings Goals
- Set financial goals and track progress in real time  
- Visual feedback on how close you are to your targets  

### 📊 Dynamic Dashboard
- Overview of balance, recent transactions, and activity  
- Clean, real-time financial summaries  

### 📈 Spending Insights
- Interactive charts powered by Jetpack Compose  
- Category-based breakdown of expenses  
- Identify trends and high-spending areas  

### 🎮 Gamification System (Key Feature)
- Earn XP for logging transactions  
- Level up based on consistency  
- Encourages disciplined financial tracking  

### 🔐 User Authentication
- Secure login and registration system  
- User-specific financial data isolation  

### 📑 Reports
- Generate detailed financial reports  
- Analyze spending behavior over time  

---

## 🧠 Why Pocket Eye?

Unlike traditional expense trackers, Pocket Eye focuses on **behavior change**, not just data entry.

It helps users:
- Stay consistent with tracking  
- Build awareness of spending habits  
- Stay motivated through rewards and progress  

---

## 🛠 Tech Stack

- **Language**: Kotlin  
- **UI Framework**: Jetpack Compose & XML  
- **Database**: Room (SQLite)  
- **Architecture**: MVVM (Model-View-ViewModel)  
- **Async Processing**: Kotlin Coroutines & Flow  
- **Android Components**: ViewModel, Lifecycle, Navigation  
- **Design System**: Material Design 3  

---

## 🏗 Architecture Overview

Pocket Eye follows the **MVVM architecture pattern**, ensuring separation of concerns and scalability.

- **Model**: Handles data logic (Room Database, Repositories)  
- **View**: UI layer (Compose & XML)  
- **ViewModel**: Manages UI state and business logic  

### Data Flow


UI → ViewModel → Repository → Room Database → ViewModel → UI


---

## 📦 Project Structure


com.ntando.expensetracker
│
├── data/
│ ├── dao/ # Database access objects
│ ├── database/ # Room database configuration
│ ├── entity/ # Data models (User, Expense, Goal)
│ └── repository/ # Data handling abstraction
│
├── viewmodel/ # Business logic layer
│
├── ui/
│ ├── dashboard/ # Main dashboard screen
│ ├── expenses/ # Expense management screens
│ ├── goals/ # Savings goals UI
│ └── reports/ # Reports and analytics
│
├── SplashActivity # App entry animation
├── MainActivity # Authentication entry point
└── navigation/ # Navigation logic


---

## ⚙️ Setup Instructions

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer  
- JDK 17+  
- Android SDK 34+  

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/pocket-eye.git
Open the project in Android Studio
Sync Gradle dependencies
Run the app on an emulator or physical device
🧪 Testing

The application has been tested (https://youtu.be/KAfF7iunvRw) for:

User authentication (valid & invalid inputs)
Expense CRUD operations
Data persistence using Room
UI responsiveness and navigation flow
🔐 Security Considerations
User data is stored locally using Room
Input validation prevents invalid data entries
Authentication ensures data isolation per user
