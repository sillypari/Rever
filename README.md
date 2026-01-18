# Rever - Lightweight Revision Planner

A modern, minimal, and lightweight Android app to help students plan and track their daily revision of study topics.

## Features

- **Subject Management**: Create and organize subjects with topics
- **Topic Import**: Add topics individually or import a bulk list
- **Smart Planning**: Automatically generates a revision timetable based on daily available time
- **Calendar View**: Week-by-week view of scheduled revision sessions
- **Progress Tracking**: Track completion percentage and view overdue topics
- **Offline First**: All data stored locally for fast access

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern declarative UI
- **Material 3** - Design system with outlined components
- **Room Database** - Local data persistence
- **DataStore** - User preferences
- **Navigation Compose** - Screen navigation
- **ViewModel** - UI state management

## Architecture

```
app/
├── data/
│   ├── dao/           # Room DAOs
│   ├── database/      # Room database setup
│   ├── model/         # Data entities
│   ├── preferences/   # DataStore preferences
│   └── repository/    # Data repositories
├── navigation/        # Navigation routes
└── ui/
    ├── components/    # Reusable UI components
    ├── screens/       # App screens
    ├── theme/         # Colors, Typography, Theme
    └── viewmodel/     # ViewModels
```

## Screens

1. **Welcome** - Onboarding start
2. **Create Subject** - Add a new subject
3. **Add Topics** - Add topics to the subject
4. **Set Daily Time** - Configure daily revision time
5. **Plan** - Calendar view with daily topics
6. **Subjects** - Manage subjects and topics
7. **Progress** - View completion stats

## Design Principles

- **Lightweight**: Minimal animations, no heavy graphics
- **Clean UI**: Outlined components, generous whitespace
- **Modern**: Material 3 design language
- **Fast**: Offline-first, optimized for low-end devices

## Color Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | #2563EB | Buttons, accents |
| Primary Light | #DBEAFE | Backgrounds, containers |
| Secondary | #10B981 | Success, completed items |
| Background | #FAFAFA | Screen background |
| Surface | #FFFFFF | Cards, dialogs |
| Text Primary | #1F2937 | Main text |
| Text Secondary | #6B7280 | Secondary text |
| Border | #E5E7EB | Card borders |

## Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Run on emulator or device

```bash
./gradlew assembleDebug
```

## Requirements

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- Minimum Android 7.0 (API 24)

## License

MIT License
