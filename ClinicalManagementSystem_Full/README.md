# Clinical Management System — Android Java

## Features
- Register, search, edit and delete patient records
- Create, search, edit, status-update and delete appointments
- Add and delete clinical notes linked to patients
- Dashboard counts for patients and appointment statuses
- Input validation and local SQLite persistence
- Database migration from the original version without dropping existing tables

## Open and run
1. Extract the ZIP.
2. Open the `ClinicalManagementSystem_Full` folder in Android Studio (the folder containing `settings.gradle`).
3. Install Android SDK Platform 35 if prompted.
4. Allow Gradle sync to complete.
5. Run on an emulator or Android device.

Android Studio may need to generate a Gradle wrapper if your installation requires one. The project uses Android Gradle Plugin 8.7.3.

## Limitations and safe use
This is an educational prototype, not a hospital-ready application. It has no authentication, encryption, audit log, secure backup, role-based access, or compliance validation. Use fictional patient data only. Back up any test data before replacing an older project.
