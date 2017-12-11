# Options
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-dontoptimize
-dontpreverify
-dontobfuscate

# Android
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Dependencies
-keep class com.google.android.gms.**
-keep class com.wealthfront.magellan.Screen
-dontwarn com.google.auto.value.**
-dontwarn com.google.common.**
-dontnote android.net.http.**
-dontnote org.apache.http.**
-dontnote sun.misc.Unsafe
-dontnote com.google.common.util.concurrent.MoreExecutors
-dontnote com.google.api.client.util.Key
-dontnote com.google.common.collect.MapMakerInternalMap$ReferenceEntry
-dontwarn javax.lang.model.element.Modifier
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**

# FareBot
-keep class com.codebutler.farebot.base.ui.FareBotUiTree
