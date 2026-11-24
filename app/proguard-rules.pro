# kotlinx.serialization: keep generated serializers
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$* *;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Credential Manager and provider events
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# kotpass (KeePass/KDBX parsing)
-keep class app.keemobile.kotpass.** { *; }
-dontwarn app.keemobile.kotpass.**

# Optional BouncyCastle dependency referenced by kotpass
-dontwarn org.bouncycastle.**
