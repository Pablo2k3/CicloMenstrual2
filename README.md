# CicloMenstrual

Aplicación Android local para registrar ciclos menstruales y notas diarias, visualizar el
historial reciente y estimar el próximo inicio.

## Funciones

- Registro del inicio y fin de ciclos.
- Visualización de días de periodo, ciclo en curso y próxima fecha prevista.
- Notas asociadas a días concretos.
- Recordatorio local el día anterior a la predicción.
- Persistencia exclusivamente local mediante Room.

La aplicación no envía datos a servidores ni requiere una cuenta. Consulte el
[manual funcional](docs/funcionalidades.md) para el comportamiento completo.

## Desarrollo

El proyecto usa Kotlin, Views/XML, MVVM, Room, corrutinas y WorkManager. Requiere
JDK 17 y Android SDK 34.

```text
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

La [arquitectura](docs/arquitectura.md), la [guía de desarrollo](docs/desarrollo.md)
y la [trazabilidad](docs/trazabilidad.md) describen cómo mantenerlo.

## Compatibilidad

Se conservan `com.example.ciclomenstrual`, el fichero `app_database`, su esquema
Room versión 1 y los datos creados por la versión Java.
