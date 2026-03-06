# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }

# SQLCipher
-keep class net.zetetic.** { *; }

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }
