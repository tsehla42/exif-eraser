# metadata-extractor
-keep class com.drew.** { *; }
# App classes
-keep class com.none.tom.exiferaser.** { *; }
# Wire (protobuf)
-keep class com.squareup.wire.** { *; }
-keepclassmembers class * extends com.squareup.wire.Message { *; }
-keepclassmembers class * extends com.squareup.wire.EnumType { *; }
-keepclassmembers class * extends com.squareup.wire.ExtendableMessage { *; }
