**Lexi: On-Device Multimodal Assistance for Dyslexia Support**

Lexi is a privacy-first, fully offline Android application designed to provide diagnostic support and phonetic corrections for children with dyslexia. Powered locally by Gemma 4 via LiteRT (formerly TensorFlow Lite), Lexi operates entirely on-device, offering specialized pipelines for both educators and students without requiring external APIs or cloud connectivity.

The application functions in two distinct modes:

**Teacher Mode:** Captures handwritten student essays to perform specialized error-pattern diagnostics and non-destructive rewrites that preserve the child's unique spoken voice and intent.

**Student Mode:** Acts as a warm, patient virtual reading coach that breaks down misspelled words phonetically into accessible sounds and simple memory anchors.

**Technical Stack:**

1. Language: Kotlin (100% Coroutines & Jetpack Compose Layout Lifecycle)

2. Frontend UI Architecture: Jetpack Compose
   
3. Edge ML Engine: Google LiteRT for Language (com.google.ai.edge.litertlm)

4. Local Foundation Model: gemma-4-E2B-it.litertlm (Multimodal Executive Variant running locally)

**Architecture:**

```text
[Physical Essay Screen / Gallery]
               │
               ▼ 
      [MainActivity] (UI Screen of the App)
               │                                               
               ▼   
      [MainViewModel] ──(Injects Role-Specific Prompt Context)
               │                                                
               ▼                                        
     [GemmaModelHandler] (Gemma 4 - 2B)                 
               │                                                 
               ▼                                                 
     [LiteRT Engine Core] 
               │  (Local CPU Execution)
               ▼
       [Structured Text] 
               │
               ▼
     [Android TTS Engine] (For audio output)
```

```text
┌─────────────────────────────────────────────────────────────┐
│                 Target Mobile Device (8GB RAM)              │
│                                                             │
│   ┌──────────────────────┐      ┌───────────────────────┐   │
│   │     Lexi App APK     │      │ File System (External)│   │
│   │                      │      │ /models/              │   │
│   │ ┌──────────────────┐ │      │ ┌───────────────────┐ │   │
│   │ │GemmaModelHandler │─┼─────>│ │gemma-4-E2B-it     │ │   │
│   │ └──────────────────┘ │      │ └───────────────────┘ │   │
│   └──────────────────────┘      └───────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```


**Setup & Installation Instructions**

Follow these steps to deploy and run Lexi locally on an Android device with at least 8GB of system RAM.

Prerequisites:
1. Android Studio

2. A physical Android device with at least 8GB RAM running Android 10 (API 29) or higher

3. USB Debugging enabled on your target device

**Step 1:**  

Clone and Build the Application

1.Clone this repository to your local workstation.

2.Open the project folder inside Android Studio.

3.Allow Gradle synchronization to complete, then build the project.

**Step 2:** 

Push the Gemma 4 Model via ADB

1.Because the application expects the quantized target model file to live within external application storage, you must upload your model file using adb.

2.Download the multimodal model file: gemma-4-E2B-it.litertlm (Available in Hugging Face)

3.Connect your target Android device via USB.

4.Push the model file directly to the app's target directory (replace com.example.lexi with your verified application package identifier if modified):
   adb push gemma-4-E2B-it.litertlm /sdcard/Android/data/com.example.lexi/files/models/

**Step 3:**

Launch and Verify

1.Run the application from Android Studio onto your connected device

2.On initial startup, the device will take approximately 1-2 minutes to initialize the model backend directly onto the CPU.

3.Once the setup phase completes, the system status message will update to: "Model ready. Capture an image to begin." You are now ready to analyze text completely offline.

**Privacy & Data Sovereignty**

The app requires zero network permissions (android.permission.INTERNET is omitted entirely), ensuring that all student files, insights, and processing remain locked down safely on the local device.




