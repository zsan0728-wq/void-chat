# VOID CHAT

### Brutalism × Liquid Glass — a monochrome Matrix client

VOID CHAT is a UI concept that rebuilds [Element X Android](https://github.com/element-hq/element-x-android) around two opposing ideas: the hard, architectural typography of **Brutalism** and the soft, refracting surfaces of **Liquid Glass**. Every screen is reduced to black, white and gray, then layered with floating glass that bends the content behind it.

The app is a real, end-to-end encrypted [Matrix](https://matrix.org/) client — not a static mockup.

## Highlights

- **Strict monochrome system.** Black, white and gray only, with large bold display type and architectural spacing. Accent color is kept to a minimum.
- **Liquid glass floating islands.** The top bar, composer and navigation are rounded glass surfaces built on [Haze](https://github.com/chrisbanes/haze), with tuned blur, Fresnel edges and specular highlights.
- **Edge-to-edge timeline.** The whole conversation scrolls beneath the glass and is refracted through it, instead of being clipped below the bars.
- **Solid opaque bubbles.** Unified rounded corners, no borders, with automatic light/dark appearance following the system and guaranteed text contrast.
- **Programmatic grayscale wallpapers.** Concrete, mist and sharp mountain scenes rendered in code, plus gallery images.
- **A–Z contacts.** Fast section index and search across the contact list.


## Tech stack

- **Kotlin** with **Jetpack Compose**
- **Matrix Rust SDK** for the encrypted messaging layer
- **Haze 2.1** for glass, blur and refraction
- **Paparazzi** for headless UI verification

## Build

The project uses Gradle version catalogs and JDK 21. Provide the Android SDK path in a local `local.properties` file, then build the debug APK:

```bash
./gradlew :app:assembleFdroidDebug
```

## Credits & license

Derived from Element X Android by Element, used under the AGPL-3.0-only / Element Commercial license. Original copyright belongs to Element and New Vector. This project keeps the same licensing terms; see the LICENSE files in the repository root.
