# kotlinx.serialization: keep generated serializers for synced entities
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.paisapilot.data.local.entity.** {
    *** Companion;
}
-keepclasseswithmembers class app.paisapilot.data.local.entity.** {
    kotlinx.serialization.KSerializer serializer(...);
}
