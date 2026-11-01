# SafeSpot R8 rules (release only).
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,Exceptions
-renamesourcefileattribute SourceFile

# ics-openvpn: JNI callbacks, java-serialized VpnProfile, AIDL, reflection on prefs. Keep everything.
-keep class de.blinkt.openvpn.** { *; }
-keep interface de.blinkt.openvpn.** { *; }
-keep class net.openvpn.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID; private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>; private void writeObject(java.io.ObjectOutputStream); private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace(); java.lang.Object readResolve(); }

# Our app is small; keep it whole (manifest components, receivers, crash reporter, JSON model names).
-keep class com.safespot.scanner.** { *; }

# AdMob / UMP / Play services ship consumer rules; just silence optional deps.
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**
