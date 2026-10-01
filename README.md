# 📱 GLPI Mobile Client

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)

A fully native Android application designed to seamlessly integrate with the **GLPI (IT Service Management)** backend. This app empowers IT professionals and regular users to manage support tickets, track hardware inventory, and organize tasks directly from their smartphones.

## ✨ Key Features & Modules

The application is divided into several powerful modules, offering a comprehensive mobile experience for GLPI:

* 🏠 **Interactive Dashboard:** Get an instant overview of your IT landscape with real-time statistics. Track tickets by status (New, In Progress, Resolved, Priority) and monitor the latest system activities at a glance.
* 🎫 **Full Ticket Lifecycle:** Complete ITIL workflow support. Create, view, update, resolve, assign, and delete support tickets on the go. View detailed conversation histories and follow-ups.
* 📦 **Smart Inventory & Reservations:** Browse and manage assigned IT equipment (computers, monitors, network gear). Features a built-in **QR Code & Barcode Scanner** leveraging the device camera to instantly identify, audit, and securely **reserve devices** for future use.
* 📅 **Agenda & Task Management:** Keep track of your workload with an interactive calendar. Easily view priority deadlines, scheduled interventions, and open tasks for any specific day.
* 👤 **Profile & Security:** Personal overview displaying your assigned devices, monthly ticket statistics, and account details. Enforces **Biometric Security** (Fingerprint/FaceID) for secure, password-less logins.
* 🔌 **GLPI REST API Integration:** Communicates entirely via the official GLPI REST API, ensuring secure, fast, and reliable data synchronization with your existing IT infrastructure without requiring middleware.
* 🔔 **Background Sync & Notifications:** Built on Android `WorkManager` and Firebase Cloud Messaging (FCM) to keep data synchronized and alert users of critical updates in real-time.

## 📸 Screenshots

| Página Principal | Inventário | Agenda | Perfil |
|:---:|:---:|:---:|:---:|
| <img width="220" alt="dashboard" src="https://github.com/user-attachments/assets/f6252ab5-347b-456a-b875-d0a1ec8108f4" /> | <img width="220" alt="inventario" src="https://github.com/user-attachments/assets/bc6a9fd6-caaa-470f-a22f-0a1d5e0c1257" /> | <img width="220" alt="agenda" src="https://github.com/user-attachments/assets/56bab24f-9e5f-4c3e-8d20-ac512c9cdfee" /> | <img width="220" alt="perfil" src="https://github.com/user-attachments/assets/36047b5f-80f1-4d36-b759-4af9d4a75852" /> |

> ℹ️ **Note:** The screenshots above were captured using the application in **Offline Mode**. All visible data (names, tickets, hardware) is dynamically generated mock data designed purely for demonstration and portfolio purposes without requiring a real GLPI server.

## 🛠️ Tech Stack & Architecture

This project was built with modern Android development standards:

* **Language:** [Kotlin](https://kotlinlang.org/)
* **Asynchronous Programming:** Kotlin Coroutines (`lifecycleScope`, `Dispatchers`)
* **Networking:** [Retrofit2](https://square.github.io/retrofit/) & [OkHttp3](https://square.github.io/okhttp/)
* **Background Processing:** AndroidX `WorkManager`
* **Cloud Services:** Firebase Cloud Messaging (FCM)
* **Hardware APIs:** `androidx.camera` and `BiometricPrompt`
* **Local Storage:** `SharedPreferences`
* **UI Components:** `RecyclerView`, Custom Adapters, BottomSheets, and SwipeRefreshLayouts.

## 🚀 How to Run the Project

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Goncalosousa24/GLPI.android.git
   ```
2. **Firebase Setup:**
   * Due to security reasons, the `google-services.json` file is ignored in this repository.
   * You need to create a project in Firebase, register your Android app, and place the generated `google-services.json` inside the `app/` directory.
3. **GLPI API Configuration:**
   * Open the app, go to Settings/Login, and input your GLPI Server URL and App-Token to establish the connection.
4. Build and run the project using **Android Studio**.

## 👨‍💻 Author

**Gonçalo Sousa**
* GitHub: [@Goncalosousa24](https://github.com/Goncalosousa24)
