# kotlinx.serialization: keep generated serializers and companions of @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Navigation type-safe routes: route classes are resolved by serializer and name at runtime.
-keep,allowobfuscation,allowshrinking class io.github.personan1.setloop.ui.navigation.Route { *; }
-keep,allowobfuscation,allowshrinking class io.github.personan1.setloop.ui.navigation.Route$* { *; }

# Shortcuts and manifest components are referenced by name from XML.
-keep class io.github.personan1.setloop.shortcuts.WorkoutCommandActivity
