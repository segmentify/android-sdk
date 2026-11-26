# Rules applied to every app that uses the Segmentify SDK and builds with
# R8/ProGuard (SFY-28792). Without them a minified app sends events with
# renamed JSON keys ("a", "b", ...) and reads empty responses.

# Request and response models are (de)serialized by Gson through reflection:
# keep every field under its own name, and the constructors Gson calls.
-keep class com.segmentify.segmentifyandroidsdk.model.** {
    <fields>;
    <init>(...);
}

# Gson and Retrofit read generic types (TypeToken subclasses, List<T> fields,
# Call<T> return types) from the class files.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
