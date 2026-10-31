# App Financiera — Rocky Sistemas

Aplicación Android para ingresos, gastos, cuotas y proyecciones. Guarda datos localmente con SharedPreferences.

Esta edición comienza vacía. No incluye datos personales, claves de firma, APK ni configuración de la computadora de desarrollo.

## Compilar

Requiere JDK 17, Gradle 8.13 y Android SDK 34. También se puede abrir como proyecto Gradle en Android Studio. Configurar `sdk.dir` en `local.properties` (archivo local excluido de Git) y ejecutar:

```sh
./gradlew assembleDebug
```

La edición pública no configura firma de producción. Para distribuir una release hay que aportar una configuración y una clave propias. No sirve para actualizar automáticamente la app privada ya instalada.

## Funciones

- Ingresos, gastos fijos y variables.
- Compras en cuotas y proyección del próximo mes.
- Cierres mensuales y reporte de texto.

## Estado de publicación

Edición pública independiente. Compilación debug verificada con Gradle 8.13 y Android SDK 34.

[Repositorio público](https://github.com/IngJuanGabriel/app-financiera) · [Catálogo de proyectos](https://rockysistemas.com.ar/).

## Autoría

Juan Gabriel Villar. El código propio se publica para consulta; todavía no se ha elegido una licencia de redistribución. Las herramientas y dependencias de compilación conservan sus propias licencias.
