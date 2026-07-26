# Contributing

## Development Setup

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android SDK 37
- Device/emulator with API 28+ for testing

### Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device/emulator

### Build Commands

Build debug APK:
```bash
./gradlew assembleDebug
```

Build release APK:
```bash
./gradlew assembleRelease
```

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

Run lint checks:
```bash
./gradlew lint
```

Clean build:
```bash
./gradlew clean
```

## Project Conventions

### Architecture

- **MVI pattern** using Orbit - ViewModels expose `ContainerHost<State, SideEffect>`
- **Single-activity** with Navigation Compose
- **Hilt DI** for all ViewModels and repositories
- **Feature-based package structure** under `com.tsehla.exiferaser`

### Code Style

- Kotlin standard conventions
- Compose functions are `@Composable` and PascalCase
- ViewModel methods are camelCase verbs (e.g., `chooseImage()`, `handleSettings()`)
- Side effects are sealed classes/interfaces
- State is `@Parcelize` data classes

### File Organization

Each feature follows this structure:
```
feature/
├── business/
│   ├── FeatureViewModel.kt      # @HiltViewModel with ContainerHost
│   ├── FeatureState.kt          # @Parcelize data class
│   └── FeatureSideEffect.kt     # Sealed class/interface
└── data/
    └── FeatureRepository.kt     # @Singleton with @Inject constructor
```

### Testing

- Unit tests in `app/src/test/`
- Use JUnit and Mockito/MockK
- ViewModel tests should mock repositories
- Test MVI state transitions

### Dependencies

- Use version catalog (`gradle/libs.versions.toml`) for all versions
- Prefer bundles for related dependencies
- Use `@Inject constructor` for Hilt injection
- Use `@param:ApplicationContext` for context injection

### Git

- Feature branches from main
- Conventional commits preferred
- No secrets or credentials in code
