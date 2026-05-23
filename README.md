# Project-Akyat

A Kotlin-based native Android hike tracking application with a custom-built RESTful API backend. Built as the final group project for IT-SYSARCH32, A.Y. 2025–2026.

---

## Overview

Project-Akyat is a Kotlin-based Android mobile application designed to help users monitor and manage their hiking activities. The application enables users to create, view, update, and delete hike records while tracking fitness-related data such as distance traveled, duration, current and maximum speed, pace, elevation gain, step count, and calories burned in real time. It also provides activity history and progress tracking features to help users maintain consistency with their hiking and fitness goals. The system is integrated with a custom RESTful backend secured through JWT-based authentication for cloud synchronization and data management, with planned weather information integration to further support hike planning and safety.

---

## Team

| Role | Member | Responsibilities |
|---|---|---|
| Backend Developer | Earl John C. Almocera | Designed the custom REST API, implemented Express server, MongoDB schemas, and JWT authentication middleware |
| Android Developer – Core | Earl John C. Almocera | Implemented real-time GPS tracking, step detection via SensorManager, and HikeTrackingService foreground service |
| Android Developer – UI/UX | Earl John C. Almocera | User interface design, wireframing, layout structuring, navigation flow planning, user experience optimization |
| API Integrator / Data Layer | Earl John C. Almocera | Built Retrofit client, AuthInterceptor, Room local database, HikeRepository, and offline sync logic |
| QA & Testing | Zhimron Evalin, Niegel Luchavez, David Gastador | Conducted unit and instrumented tests, end-to-end CRUD validation, and edge case verification |
| Documentor | Zhimron Evalin, Niegel Luchavez, David Gastador | Prepared README, API reference documentation, and architecture diagram |
| Project Manager | Earl John C. Almocera | Coordinated sprint schedules, tracked deliverables, and managed overall project timeline |

---

## Features

- Real-time GPS tracking with live distance, elevation, pace, and step counting via foreground service
- Auto-pause on inactivity (under 1 km/h for 10 seconds); manual pause/resume with haptic feedback
- Offline-first architecture — hikes saved locally to Room and synced to the cloud when online
- JWT-based authentication with automatic token attachment on all protected requests
- Difficulty classification (Easy / Moderate / Hard) based on elevation gain
- Hike history with upload and delete support

---

## Download

A prebuilt APK is available under [Releases](https://github.com/skad-die/project-akyat-2026/releases/tag/v1.0.0).

### Installation

1. Download `project-akyat.apk` from the Releases page
2. On your Android device, go to **Settings → Install unknown apps** and allow installation from your browser or file manager
3. Open the downloaded APK and tap **Install**
4. Launch **Project-Akyat** from your app drawer

> **Note:** The backend is hosted on Render's free plan, which spins down after periods of inactivity. The first login or registration attempt may fail or take up to 60 seconds to respond while the server wakes up. If the request fails, wait a moment and try again.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile Client | Android (Kotlin) |
| Networking | Retrofit 2 + Gson |
| Local Database | Room (SQLite) |
| Location | FusedLocationProviderClient |
| Step Counting | Android SensorManager (TYPE_STEP_DETECTOR) |
| Backend | Node.js + Express 5 |
| Cloud Database | MongoDB Atlas (Mongoose) |
| Authentication | JWT (jsonwebtoken) + bcrypt |
| Hosting | Render.com |

---

## Getting Started

### Prerequisites

- Android Studio (Hedgehog or later)
- Node.js v18+
- MongoDB Atlas account (or local MongoDB)

### Backend Setup

```bash
cd backend
npm install
```

Create a `.env` file inside `/backend`:

```env
MONGO_URI=your_mongodb_atlas_connection_string
JWT_SECRET=your_jwt_secret_key
PORT=3000
```

Start the server:

```bash
node server.js
```

The API will be available at `http://localhost:3000`.

### Android Setup

1. Open the `/app` folder in Android Studio.
2. In `RetrofitClient.kt`, update the base URL to your backend:
   ```kotlin
   private const val BASE_URL = "https://your-backend-url.onrender.com/"
   ```
3. Sync Gradle and run on a physical device. GPS features require real hardware.

---

## Environment Variables

| Variable | Description |
|---|---|
| MONGO_URI | MongoDB Atlas connection string |
| JWT_SECRET | Secret key for signing JWT tokens |
| PORT | Server port (defaults to 3000 if not set) |

---

## Repository Structure

```
project-akyat-2026/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/project_akyat/
│       │   │   ├── adapters/
│       │   │   │   └── HikeAdapter.kt
│       │   │   ├── fragments/
│       │   │   │   ├── DashboardFragment.kt
│       │   │   │   ├── HistoryFragment.kt
│       │   │   │   └── ProgressFragment.kt
│       │   │   ├── model/
│       │   │   │   ├── local/
│       │   │   │   │   ├── dao/HikeDao.kt
│       │   │   │   │   ├── db/AppDatabase.kt
│       │   │   │   │   └── HikeEntity.kt
│       │   │   │   ├── remote/
│       │   │   │   │   ├── HikeRequest.kt
│       │   │   │   │   ├── LoginRequest.kt
│       │   │   │   │   ├── MeResponse.kt
│       │   │   │   │   └── RegisterRequest.kt
│       │   │   │   └── HikeRepository.kt
│       │   │   ├── network/
│       │   │   │   ├── ApiService.kt
│       │   │   │   ├── AuthInterceptor.kt
│       │   │   │   ├── RetrofitClient.kt
│       │   │   │   └── TokenManager.kt
│       │   │   ├── utils/
│       │   │   │   └── FormatUtils.kt
│       │   │   ├── viewmodel/
│       │   │   │   └── HikeSummaryViewModel.kt
│       │   │   ├── HikeSummaryActivity.kt
│       │   │   ├── HikeTrackingService.kt
│       │   │   ├── LoginActivity.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── RegisterActivity.kt
│       │   │   ├── SplashActivity.kt
│       │   │   └── StartHikeActivity.kt
│       │   └── res/
│       │       ├── drawable/        # Icons and badge backgrounds
│       │       ├── layout/          # Activity and fragment XML layouts
│       │       ├── menu/            # Bottom nav, side nav, toolbar menus
│       │       └── values/          # Themes, colors, strings
│       ├── androidTest/             # Instrumented tests
│       └── test/                    # Unit tests
├── backend/
│   ├── models/
│   │   ├── hike.model.js
│   │   └── user.model.js
│   ├── db.js
│   ├── server.js
│   └── package.json
└── .gitignore
```

---

## License

This project was developed for academic purposes under IT-SYSARCH32, A.Y. 2025–2026.
