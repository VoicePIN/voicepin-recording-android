VoicePIN Android Recorder
=========================

Library for sound recording and playback that may be beneficial for integrating android application with VoicePIN.

Use `com.voicepin.android.audio.Recorder` for recording and `com.voicepin.android.audio.Player` for playback.

Gradle/Maven dependency
=======================

Add VoicePIN.com Maven repository to `build.gradle`:

    repositories {
        maven { url 'https://nexus.voicepin.com/repository/maven-releases'}
    }
    
Add voicepin-recording-android dependency:

    dependencies {
        compile('com.voicepin:voicepin-recording-android:1.0@aar') {
            transitive = true
        }
    }