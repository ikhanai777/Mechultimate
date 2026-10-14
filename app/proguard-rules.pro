# kotlinx.serialization: keep generated serializers for saved-calculation models.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.mechultimate.app.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mechultimate.app.data.**$$serializer { *; }
