# Media3: consumer-rules gestiti internamente dal pacchetto.
-dontwarn com.google.common.**
-keep class androidx.media3.** { *; }
-keep class okhttp3.** { *; }

# NewPipeExtractor — usa riflessione su Jsoup e Rhino per JS signature decode
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.jsoup.** { *; }
-dontwarn org.mozilla.javascript.tools.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**

# FFmpegKit — interfacce native JNI e callback
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.**

# SearchActivity migration (§10.4)
-keep class org.json.** { *; }
-keep class com.pegasus.videoplayer.SearchActivity
-keep class com.pegasus.videoplayer.VideoPlayerApp
