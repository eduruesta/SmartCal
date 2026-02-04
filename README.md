# SmartCalAI - Smart Calendar Assistant

SmartCalAI is an intelligent calendar assistant mobile application built with Kotlin Multiplatform (KMP) and Jetpack Compose. The app integrates with external calendar services to help users manage their schedules through natural language interactions powered by AI.

## 🎯 Features

- 🗓️ **Smart Calendar Management**: Create, read, update, and delete calendar events using natural language
- 🤖 **AI-Powered Assistant**: Powered by GPT-4 for intelligent conversation and calendar operations
- 📱 **Cross-Platform**: Built with Kotlin Multiplatform for Android and iOS
- 🎨 **Modern UI**: Clean Material Design 3 interface with Jetpack Compose
- 🌐 **External Calendar Integration**: Connects to external calendar services via HTTP API
- ⏰ **Date & Time Processing**: Advanced natural language time parsing and formatting

## Project Configuration

- **Package**: `com.smartcal.app`
- **Platforms**: Android, iOS
- **UI Framework**: Jetpack Compose Multiplatform 1.8.2
- **Architecture**: Clean Architecture with AI Agent integration
- **Calendar Integration**: HTTP API with external calendar service

## 🚀 Key Dependencies

- ✅ **Kotlin 2.1.21** - Latest Kotlin version
- ✅ **Compose Multiplatform 1.8.2** - Modern UI framework
- ✅ **Koin 4.0** - Dependency Injection
- ✅ **Ktor 3.1** - HTTP Client  
- ✅ **Navigation Compose 2.7** - Navigation
- ✅ **Kotlinx Serialization** - JSON serialization
- ✅ **Koog AI 0.4** - AI Agent framework
- ✅ **Coil 3.1** - Image loading
- ✅ **Kotlinx DateTime** - Date/time utilities

## 🛠️ Commands

```bash
# Build project
./gradlew build

# Run Android app
./gradlew :composeApp:installDebug

# Run iOS Simulator
./gradlew :composeApp:iosSimulatorArm64Run

# Run tests
./gradlew test

# Clean project
./gradlew clean
```

## 📁 Project Structure

```
src/
├── commonMain/kotlin/com.smartcal.app/
│   ├── data/
│   │   ├── agent/          # AI agents
│   │   ├── repository/     # Data repositories
│   │   ├── remote/         # API clients
│   │   └── settings/       # App settings
│   ├── domain/
│   │   ├── entity/         # Business entities
│   │   ├── repository/     # Repository interfaces
│   │   └── usecase/        # Business use cases
│   ├── presentation/
│   │   ├── components/     # Reusable UI components
│   │   ├── navigation/     # Navigation setup
│   │   ├── screens/        # App screens
│   │   ├── theme/          # App theming
│   │   └── viewmodel/      # ViewModels
│   ├── di/                 # Dependency injection
│   └── utils/              # Utility functions
├── androidMain/kotlin/com.smartcal.app/
│   ├── data/settings/      # Android settings
│   └── di/                 # Android DI modules
└── iosMain/kotlin/com.smartcal.app/
    ├── data/settings/      # iOS settings
    └── di/                 # iOS DI modules
```

## 🏗️ Architecture

This project follows **Clean Architecture** principles:

1. **Presentation Layer**: ViewModels, Screens, Components
2. **Domain Layer**: Entities, Use Cases, Repository Interfaces
3. **Data Layer**: Repository Implementations, Data Sources

## 🔧 Development Guidelines

1. **Shared Code**: Place business logic in `commonMain`
2. **Platform Code**: Use `expect/actual` for platform-specific implementations
3. **Dependency Injection**: Use Koin modules for managing dependencies
4. **State Management**: Use StateFlow for reactive state management
5. **Network**: Use Ktor client for HTTP requests
6. **Navigation**: Use Navigation Compose for screen navigation

## 🚀 Getting Started

1. Open the project in Android Studio or IntelliJ IDEA
2. Sync Gradle dependencies
3. Run the app on Android or iOS
4. Start building your features!

## 📅 Setup & Configuration

### Prerequisites

1. **External Calendar Service**: You need a running calendar service that exposes an HTTP API
2. **OpenAI API Key**: Required for the AI assistant functionality

### Setup Instructions

#### 1. Configure API Keys

Add your OpenAI API key to the repository configuration or environment variables.

#### 2. Configure Calendar Service URL

Update the calendar service URL in `CalendarChatTool` (`DateTimeUtils.kt`):

```kotlin
val response = client.post("http://your-calendar-service:8080/chat") {
    // Your calendar service endpoint
}
```

#### 3. Build and Run the App

```bash
# Build project
./gradlew build

# Run Android app
./gradlew :composeApp:installDebug

# Run iOS Simulator
./gradlew :composeApp:iosSimulatorArm64Run
```

## 🎯 Usage

1. Launch the app on your device
2. Wait for the AI assistant to initialize
3. Start chatting with the assistant using natural language:
   - "¿Qué eventos tengo hoy?"
   - "Crea una reunión mañana a las 3pm"
   - "Elimina mi cita del viernes"
   - "¿Tengo tiempo libre el jueves por la mañana?"

### ✅ Available Calendar Operations
The AI assistant can help you with:
- **📅 List Events**: View upcoming events and schedules
- **➕ Create Events**: Schedule new meetings and appointments
- **✏️ Update Events**: Modify existing calendar entries
- **🗑️ Delete Events**: Remove unwanted appointments
- **🔍 Search Events**: Find specific events by description or time
- **⏰ Time Processing**: Understand natural language time expressions

### 🔮 Future Enhancements
- **📝 Enhanced Event Creation**: Rich event details and recurring events
- **🔍 Advanced Search**: Enhanced search and filtering capabilities
- **⚡ Real-time Sync**: Live calendar updates and notifications
- **📊 Calendar Analytics**: Insights about your schedule patterns

## 🏗️ Architecture Overview

The app follows a clean architecture pattern with:

```
composeApp/src/commonMain/kotlin/com/calendar/agent/project/
├── ai/
│   ├── agent/          # AI agent configuration and setup
│   └── tools/          # Calendar and datetime processing tools
├── repository/         # Data management and API interactions
├── ui/                 # Compose UI components and screens
├── viewmodel/          # UI state management
└── models/             # Data models and entities
```

### Key Components

- **`CalendarAgent`**: AI agent that orchestrates calendar operations using GPT-4
- **`CalendarChatTool`**: HTTP client for external calendar service communication
- **`DateTime Tools`**: Natural language time processing utilities
- **`CalendarScreen`**: Main chat interface with the AI assistant built in Compose

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with [Koog AI Agents Framework](https://koog.ai)
- Powered by OpenAI GPT-4
- UI components from Material Design 3

---

**SmartCalAI - Smart Calendar Assistant** 🚀  
Package: `com.smartcal.app`  
Platform: Kotlin Multiplatform Compose

Manage your calendar with AI! 📅✨
