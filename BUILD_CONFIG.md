# Build Configuration — SpatialFlow UI Overhaul

## Dependencies to add in `app/build.gradle.kts`

```kotlin
dependencies {
    // ── SMOOTH CORNERS (squircle/continuous curve) ──
    implementation("io.github.racra:compose-smooth-corner-rect:1.0.0")

    // ── ROOM (persistent color scheme cache) ──
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── GOOGLE FONTS (Montserrat) ──
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")

    // ── COIL (image loading) ──
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ── MATERIAL (HCT color utilities) ──
    implementation("com.google.android.material:material:1.12.0")
}
```

## `res/values/arrays.xml` — Required for Google Fonts

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
</resources>
```

## Room Database — Add `ColorSchemeDao` to your `@Database` class

```kotlin
@Database(entities = [CachedColorScheme::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun colorSchemeDao(): ColorSchemeDao
}
```
