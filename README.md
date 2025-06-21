# JomExplore

🎯 **JomExplore** is an Android application that leverages artificial intelligence and augmented reality to preserve Malaysia’s cultural heritage and promote sustainable tourism.

## 🏆 Core Features (MVP)

### 📸 AI Photo Recognition + AR Model Display (Function 1)
- Uses **TensorFlow Lite** to recognize heritage sites on-device through the camera.
- Upon recognition, loads the corresponding **3D model** and displays it in real-world space using **ARCore**.

### 🗺️ Region-Based Achievement Unlocking (Function 2)
- Users unlock digital achievements by physically visiting heritage sites.
- **Firebase Authentication** handles user management.
- **Firebase Firestore** stores user progress and achievements.

---

## 🧱 Tech Stack

| Module              | Technology                         |
|---------------------|-------------------------------------|
| Frontend            | Android (Java + Jetpack Compose)    |
| Backend             | Firebase (Authentication + Firestore) |
| AI Recognition      | TensorFlow Lite (on-device model)   |
| Augmented Reality   | ARCore + 3D Models (.glb format)    |

---

## 📍 Planned Features (Post-Hackathon)

- ✅ **Friend Leaderboard**: Compare exploration achievements with friends.
- ✅ **Point Redemption System**: Earn points by visiting sites, redeem for tourism coupons.
- ✅ **Map API Integration**: Navigate and discover nearby heritage attractions.

---

## 🚀 Getting Started (Developer Guide)

1. Clone this repository:
```bash
git clone [https://github.com/haoo04/JomExplore.git]
