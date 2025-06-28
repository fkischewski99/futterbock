# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Running the Application
```bash
# Desktop application (recommended)
./gradlew :composeApp:desktopRun -PmainClass=MainKt

# Alternative shorthand  
./gradlew run
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :composeApp:testDebugUnitTest
```

### Building
```bash
# Build all platforms
./gradlew build

# Build desktop distribution
./gradlew :composeApp:packageDistributionForCurrentOS
```

## Project Architecture

This is a **Kotlin Multiplatform Compose application** for meal planning and shopping list management for scout camps. The app targets Android, Desktop (JVM), and iOS platforms.

### Core Architecture Patterns

**Clean Architecture with Dependency Injection (Koin)**
- Repository pattern with Firebase backend
- MVVM with Compose UI
- Reactive programming using Kotlin Flow
- Platform-specific implementations using expect/actual

### Module Organization

**DI Modules** (`composeApp/src/commonMain/kotlin/modules/`)
- `DataModules.kt` - Repository bindings (EventRepository → FireBaseRepository)
- `ServiceModules.kt` - Business logic services (calculations, PDF, auth, updates)
- `ViewModelModules.kt` - All ViewModels as singletons with dependency injection

**Data Layer**
- `EventRepository` interface - 88 methods covering all data operations
- `FireBaseRepository` - Firebase implementation with batch operations and error handling
- Models in `model/` directory represent domain entities

**Service Layer**
- Domain services for business logic (shopping list calculation, material calculation)
- Platform abstractions (PDF generation, file picking, update checking)
- Authentication service with Firebase backend

**View Layer**
- `SharedEventViewModel` - Central event management with action pattern
- Specialized ViewModels for different screens (overview, shopping lists, participants)
- Compose UI with Material 3 design system

### Key Patterns to Follow

**ViewModel Actions**
```kotlin
// Use sealed classes for type-safe actions
sealed interface EventAction {
    data class LoadEvent(val eventId: String) : EventAction
    // ...
}

// Central action handler in ViewModel
fun onAction(action: EventAction) {
    when (action) {
        is EventAction.LoadEvent -> handleLoadEvent(action.eventId)
        // ...
    }
}
```

**State Management**
```kotlin
// Use ResultState wrapper for consistent loading/error states
sealed interface ResultState<out T> {
    data object Loading : ResultState<Nothing>
    data class Success<T>(val data: T) : ResultState<T>
    data class Error(val exception: Throwable) : ResultState<Nothing>
}
```

**Repository Pattern**
- Always program against interfaces (`EventRepository`, not `FireBaseRepository`)
- Use suspend functions for async operations
- Return Flow for reactive data streams
- Implement proper error handling with try-catch

### Platform-Specific Code

**File Structure**
- `commonMain/` - Shared business logic and UI
- `androidMain/` - Android-specific implementations
- `desktopMain/` - Desktop (JVM) implementations  
- `iosMain/` - iOS-specific implementations

**Expect/Actual Pattern**
```kotlin
// commonMain
expect class PlatformService {
    fun performPlatformAction(): String
}

// Platform-specific implementations in respective source sets
actual class PlatformService {
    actual fun performPlatformAction(): String = "Platform-specific result"
}
```

### Database and Backend

**Firebase Firestore** - Main backend with collections for:
- Events, Participants, Meals, Recipes, Ingredients, Materials
- Real-time synchronization via Flow-based reactive streams
- Batch operations for performance optimization

**Authentication** - Firebase Auth with email/password

### Important Business Logic

**Participant Age Weighting**
- Babies (<4): 0.4x factor
- Children (<10): 0.7x factor  
- Teens (11-14): 1.0x factor
- Young adults (15-23): 1.2x factor
- Adults (24+): 1.0x factor

**CSV Import** - Supports participant import with columns: Vorname, Nachname, Ernährungsweise, Geburtsjahr

### Configuration

**Environment Variables/local.properties**
- `FIREBASE_PROJECT_ID`
- `FIREBASE_APPLICATION_ID` 
- `FIREBASE_API_KEY`

**Version Management** - Uses `versions.properties` file for version numbers across all platforms