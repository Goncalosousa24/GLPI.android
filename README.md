# 📱 GLPI Mobile Client

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)

A fully native Android application designed to seamlessly integrate with the **GLPI (IT Service Management)** backend. This app empowers IT professionals and regular users to manage support tickets, track hardware inventory, and organize tasks directly from their smartphones.

## ✨ Key Features

* 🎫 **Full Ticket Lifecycle:** Create, view, update, resolve, and delete support tickets on the go.
* 🔒 **Biometric Security:** Secure and fast login using device native Fingerprint/FaceID authentication.
* 📷 **Smart Inventory (QR Scanner):** Built-in barcode and QR code scanner leveraging device camera to instantly identify and manage IT assets.
* 🔔 **Background Sync & Notifications:** Uses Android WorkManager and Firebase Cloud Messaging (FCM) to alert users of new tickets and updates in real-time.
* 📅 **Task Management:** Integrated interactive calendar and agenda views to track assigned tasks and priority deadlines.

## 📸 Screenshots

*(Substitua os links abaixo pelas imagens reais da sua aplicação. Dica: ao editar este ficheiro no GitHub, pode arrastar as fotos do seu computador para o texto e o GitHub cria um link automaticamente)*

| Dashboard | Detalhes do Ticket | Leitor QR | Perfil |
|:---:|:---:|:---:|:---:|
| <img src="URL_DA_IMAGEM_1" width="200"/> | <img src="URL_DA_IMAGEM_2" width="200"/> | <img src="URL_DA_IMAGEM_3" width="200"/> | <img src="URL_DA_IMAGEM_4" width="200"/> |

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
