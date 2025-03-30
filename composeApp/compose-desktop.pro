# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

-keep class data.io.** { *; }
-keep class base.** { *; }
-keep class base.navigation.NavigationNode { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class java.net.http.** { *; }
-keep class org.cef.** { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory

# Keep Coil classes
-keep class coil3.** { *; }

# Keep Ktor classes
-keep class io.ktor.client.** { *; }
-keep class io.ktor.utils.io.** { *; }
-keep class io.ktor.utils.io.jvm.** { *; }
-keep class io.ktor.utils.io.nio.** { *; }
-keep class io.ktor.server.config.** { *; }
-keep class io.ktor.serialization.** { *; }

-keep class com.google.firebase.** { *; }
-keep class com.google.firestore.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keep class org.koin.dsl.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class okio.** { *; }
-keep class androidx.datastore.preferences.** { *; }
-keep class androidx.collection.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class android.graphics.drawable.Drawable { *; }
-keep class android.graphics.Bitmap { *; }
-keep class android.graphics.Canvas { *; }
-keep class android.graphics.Shader$TileMode { *; }
-keep class android.graphics.Shader { *; }
-keep class android.graphics.Paint { *; }
-keep class android.graphics.BitmapShader { *; }
-keep class android.database.** { *; }
-keep class android.os.** { *; }
-keep class android.asynclayoutinflater.view.** { *; }
-keep class androidx.core.graphics.drawable.** { *; }
-keep class androidx.core.internal.view.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-keep class android.view.** { *; }
-keep class io.github.alexzhirkevich.compottie.CompottieInitializer.*
-keep class org.apache.commons.logging.** { *; }
-keep class org.sqlite.** { *; }

# JVM
-keep class java.awt.*

-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

-keepclasseswithmembers public class MainKt {
    public static void main(java.lang.String[]);
}

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-assumenosideeffects public class androidx.compose.runtime.ComposerKt {
    void sourceInformation(androidx.compose.runtime.Composer,java.lang.String);
    void sourceInformationMarkerStart(androidx.compose.runtime.Composer,int,java.lang.String);
    void sourceInformationMarkerEnd(androidx.compose.runtime.Composer);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep @kotlinx.serialization.Serializable class * {*;}
# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences

# disable optimisation for descriptor field because in some versions of ProGuard, optimization generates incorrect bytecode that causes a verification error
# see https://github.com/Kotlin/kotlinx.serialization/issues/2719
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

#  caused crashes on release Jvm
#-dontwarn androidx.compose.material.**
#-keep class androidx.compose.material3.** { *; }
#-keep class androidx.compose.runtime.** { *; }



# Keep LocalVariableTable attribute
-keepattributes LocalVariableTable, LocalVariableTypeTable

# Disable specific optimizations that might cause issues
-dontoptimize

# Just in case everything goes south
-ignorewarnings