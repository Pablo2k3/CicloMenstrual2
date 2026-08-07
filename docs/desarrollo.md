# Guía de desarrollo

## Entorno

El módulo `app` usa Kotlin/JVM 17, Android SDK 34, Views/XML y ViewBinding. Las
versiones de plugins y dependencias se mantienen en
[`gradle/libs.versions.toml`](../gradle/libs.versions.toml). El proyecto conserva
el `applicationId` `com.example.ciclomenstrual` y la base Room
`app_database` por compatibilidad.

## Convenciones

- Kotlin idiomático, modelos inmutables y dependencias dirigidas hacia
  `domain`.
- Las reglas de negocio se implementan en clases puras y no se duplican en
  `Activity`, adapters, DAO o receivers.
- `MainActivity` renderiza `StateFlow<MainUiState>` y envía acciones al
  `MainViewModel`; no consulta DAOs.
- Los DAO son `suspend` y Room se encarga del trabajo fuera del hilo de interfaz;
  no se crean executors manuales.
- Las operaciones que pueden ejecutarse con la app cerrada (alarmas y
  notificaciones) deben leer el estado persistido y ser idempotentes.
- Los textos visibles pertenecen a recursos cuando corresponda. Los textos de
  notificación deben conservar compatibilidad con trabajos ya encolados.
- Cada cambio de comportamiento actualiza documentación, pruebas y
  [trazabilidad](trazabilidad.md).

## Comandos de verificación

Desde la raíz del repositorio, en Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

En cada cambio conviene ejecutar primero las pruebas unitarias. Antes de una
entrega, ejecutar también `connectedDebugAndroidTest`, `lintDebug` y la
compilación release. Las pruebas instrumentadas necesitan un emulador o
dispositivo conectado y cubren Room/migraciones y la configuración de permisos y receivers; la
cobertura de interfaz y los retrasos producidos por Android requieren
validación manual adicional.

## Añadir o modificar una función

1. Identificar la regla y el modelo necesarios en `domain`.
2. Añadir o modificar la interfaz de repositorio si hay persistencia.
3. Crear la entidad/DAO y su adaptador Room en `data`.
4. Exponer la acción y los cambios de estado desde `MainViewModel`.
5. Renderizar el nuevo estado desde la vista sin volver a implementar reglas.
6. Si el cambio toca una alarma o un receiver, hacer que el planificador de
   `domain` describa el siguiente evento y dejar Android como adaptador.
7. Añadir pruebas unitarias para la regla y pruebas instrumentadas cuando haya
   Room, permisos, notificaciones o ciclo de vida.
8. Actualizar el manual funcional, la arquitectura o las reglas según el
   alcance y añadir una fila a la matriz de trazabilidad.

## Cambios de base de datos

Todo cambio de esquema debe:

- incrementar la versión de `AppDatabase`;
- añadir una `Migration` explícita y registrarla en `getInstance()`;
- conservar las tablas y datos históricos salvo decisión documentada;
- añadir o actualizar una prueba instrumentada que abra la forma anterior;
- revisar entidades nullable, nombres de columna, claves y foreign keys.

No se debe sustituir una migración por `fallbackToDestructiveMigration`, porque
los ciclos y notas forman parte del historial personal de la aplicación.

## Validación manual recomendada

Antes de publicar, comprobar:

- primer arranque con y sin tratamiento configurado;
- selección de día, cambio de mes/año y recreación de actividad;
- selección tocando directamente el número y tocando las zonas intermedias de la celda;
- modo oscuro: calendario, superficies, tarjeta, lista, aviso y barras del sistema;
- ciclo completo, ciclo incompleto, cambio de inicio, solapamiento y eliminación;
- nota vacía, nota con contenido y eliminación individual cuando existen
  duplicados;
- configuración histórica de pastillas, cambio de tratamiento y consulta de
  tratamientos cerrados;
- estados `Próxima`, `Pendiente`, `Tomada`, `Omitida` y `Placebo automático`;
- confirmación desde calendario y desde notificación;
- permiso de notificaciones concedido/denegado;
- permiso de alarmas exactas concedido/denegado en Android 12+;
- reinicio, cambio de hora, cambio de zona horaria y restricciones de batería;
- dosis a las 14:00 en verano español, a las 13:00 en invierno y conversión a
  11:00/10:00 en un dispositivo tres horas retrasado;
- apertura con una base Room de versión 1.

## Casos de aceptación del backlog

Estos casos completan la cobertura manual de T08 y deben repetirse cuando se
toquen las áreas indicadas:

1. Cambiar el inicio de un ciclo incompleto, cerrar y volver a abrir la
   aplicación; debe quedar solo el nuevo inicio.
2. Cambiar o eliminar el último ciclo y comprobar que el recordatorio de ciclo
   anterior desaparece y solo queda el de la predicción vigente.
3. Configurar dos notas con la misma fecha y texto; borrar una debe conservar
   la otra.
4. Marcar una pastilla desde el calendario y cambiar de tratamiento; la
   notificación visible y las alarmas anteriores deben desaparecer.
5. Pulsar una acción de una notificación antigua tras cambiar de tratamiento;
   no debe modificar la toma del tratamiento activo.
6. Probar permisos de notificaciones y alarmas exactas concedidos y denegados,
   reinicio, cambio de hora y cambio de zona horaria. Con Madrid en verano e
   invierno deben verse 14:00/13:00; tres horas menos debe mostrar 11:00/10:00.
7. Eliminar un ciclo completo y cancelar la confirmación; el historial debe
   permanecer intacto. Confirmar debe eliminarlo y actualizar la predicción.

Las pruebas unitarias de `MainViewModelTest` cubren el cambio de inicio, la
reprogramación de predicción, la identidad de notas y la cancelación de avisos.
`PillAlarmPolicyTest` cubre acciones y alarmas obsoletas sin levantar Android.
`AndroidConfigurationTest` comprueba permisos y receivers desde el manifiesto;
se ha ejecutado correctamente con `connectedDebugAndroidTest` en el emulador

## Entrega incremental

Los commits deben mantener el proyecto compilable y separar, cuando sea posible,
configuración, dominio, datos, presentación, infraestructura de notificaciones y
documentación. Una refactorización no debe cambiar reglas de producto sin una
decisión explícita y nuevas pruebas de aceptación.
