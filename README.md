# DecoCenter

12 tarjetas para el Centro de Control de Android: decoración y mini-herramientas (interruptor,
linterna, temporizador, contador, portapapeles, dado, post-it y lanzador de apps). La interfaz usa
[Miuix](https://github.com/compose-miuix-ui/miuix) 0.9.4 sobre Jetpack Compose.

## Compilar

Requisitos: **JDK 21** y Android SDK (compileSdk 37). Las versiones están alineadas con Miuix 0.9.4:
Gradle 9.7.1, AGP 9.4.1, Kotlin 2.4.20 (ver `gradle/libs.versions.toml`).

```sh
./gradlew assembleDebug          # APK debug en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # tests de la lógica (dados, contador, copia de seguridad, temas)
./gradlew lint                   # informe en app/build/reports/
```

## Firma del release

Sin configurar nada, `assembleRelease` genera un APK sin firmar. Para firmarlo define estas variables
(entorno o `local.properties`): `KEYSTORE_PATH`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASSWORD`.

En GitHub Actions, el job «APK release firmado» (se lanza a mano con *Run workflow → release*) lee estos
secretos del repositorio: `KEYSTORE_B64` (el `.jks` en base64: `base64 -w0 decocenter.jks`),
`KEYSTORE_PASS`, `KEY_ALIAS` y `KEY_PASSWORD`.

## Estructura

| Archivo | Qué hace |
| --- | --- |
| `DecoLogic.kt`, `Model.kt` | Lógica pura (sin Android) y modelo de datos; la cubren los tests JVM |
| `DecoStore.kt` | Estado único de las 12 tarjetas (estado de Compose) y persistencia con agrupado de escrituras |
| `Secrets.kt` | Cifrado AES-256/GCM con el Android Keystore para los textos del Portapapeles |
| `Backup.kt` | Exportar e importar en JSON (el texto del Portapapeles solo si se pide) |
| `Presets.kt` | Temas rápidos |
| `TapActions.kt` | Lo que hace cada función al tocar (linterna, temporizador con alarma...) |
| `DecoTileService.kt` | Base de las tarjetas del Centro de Control |
| `GeneratedTiles.kt` | `DecoTile1…12`: **generado**, no editar |
| `ui/` | Pantalla en Compose + Miuix: simulador, editor, selectores de símbolo y de app |

Android exige un servicio por tarjeta. Las clases `DecoTileN`, sus entradas del manifest y sus etiquetas
se regeneran con `python3 tools/gen_tiles.py` (si cambias el número, actualiza también `DecoLogic.SLOTS`
y `defaultGlyphs` en `DecoStore`).

## Notas

- **Temporizador**: la alarma usa `setAndAllowWhileIdle` (sin permiso de alarma exacta), así que en Doze
  puede retrasarse unos minutos. Si el móvil se reinicia, la alarma se pierde y la tarjeta muestra «¡Listo!»
  al volver a verse.
- **Portapapeles**: el texto se guarda cifrado con una clave del Keystore. Si el Keystore falla, el texto
  solo vive en memoria durante la sesión (nunca se escribe en claro).
- **Textos**: la interfaz está en `res/values/strings.xml`; el contenido de los temas rápidos
  (nombres y símbolos) está en `Presets.kt`.
- **Play Store**: `targetSdk = 36`, que es lo que Google exige a las apps nuevas y a las actualizaciones
  desde el 31 de agosto de 2026.
