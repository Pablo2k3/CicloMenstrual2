# CicloMenstrual

Aplicación Android local para registrar ciclos menstruales, añadir notas por día
y llevar una pauta anticonceptiva de 21 pastillas activas más 7 de placebo. La
aplicación calcula una fecha prevista, muestra el historial en un calendario y
programa recordatorios en el dispositivo.

No requiere cuenta ni conexión a Internet para funcionar. Los datos se guardan
en la base de datos privada del dispositivo y la aplicación no los envía a un
servidor. El seguimiento es informativo: no sustituye indicaciones médicas ni
decide qué hacer ante una dosis omitida.

## Funcionamiento resumido

1. En el primer arranque, si no existe un tratamiento activo, se solicita la
   fecha de la pastilla 1. La fecha máxima seleccionable es hoy; la configuración
   también puede hacerse después desde la tarjeta de pastillas.
2. La pantalla principal muestra un calendario con una ventana de seis meses
   anteriores y seis posteriores. Al seleccionar un día se cargan sus notas y,
   si corresponde, el estado de la pastilla de ese día.
3. El botón flotante permite marcar el inicio o el fin de un ciclo, cambiar el
   inicio del ciclo incompleto seleccionado, eliminar un ciclo completo y añadir
   una nota.
4. La pauta de pastillas se calcula a partir de la fecha de inicio: los días
   1–21 son activos, los días 22–28 son placebo y después se repite el envase.
   La referencia horaria prevista es la hora peninsular española: 14:00 durante
   el horario de verano y 13:00 fuera de él. Al viajar, se convierte a la zona
   horaria del dispositivo; por ejemplo, tres horas menos implica 11:00 en
   verano y 10:00 en invierno. La fecha del historial es independiente del
   instante de la alarma, por lo que viajar no desplaza los días guardados.
5. Las notificaciones se programan de una en una. Una pastilla activa avisa a
   la hora de dosis, repite el aviso cada 15 minutos durante la ventana activa y
   se marca como omitida al terminarla. Los placebos se registran
   automáticamente.

La descripción detallada de los flujos de usuario, estados y validaciones está
en el [manual funcional](docs/funcionalidades.md).

## Presentación visual

La selección del calendario ocupa toda la celda del día: tocar cerca del número mantiene el círculo azul en la misma fecha que ha resuelto el calendario.

La pantalla conserva la apariencia original en modo claro y adapta únicamente el calendario, la lista de notas, los avisos y las barras del sistema cuando se usa el modo oscuro.

## Estructura del proyecto

```text
app/src/main/java/com/example/ciclomenstrual/
├── presentation/   Estado de pantalla, ViewModel, calendario y notas
├── domain/         Modelos y reglas puras de ciclos y pastillas
├── data/           Room, DAOs, entidades y repositorios
└── notifications/  WorkManager, AlarmManager y BroadcastReceivers
docs/               Documentación funcional, técnica y de mantenimiento
```

El flujo principal es:

```text
MainActivity (Views/XML)
        │ eventos y renderizado
        ▼
MainViewModel ── StateFlow<MainUiState>
        ├── reglas de domain
        ├── repositorios ── Room / app_database
        ├── WorkManager ── recordatorio de ciclo
        └── AlarmManager ── recordatorio de pastilla
```

La [arquitectura técnica](docs/arquitectura.md) explica las responsabilidades de
cada capa y el flujo de carga, persistencia y notificaciones.

## Requisitos de desarrollo

- Android Studio o un entorno con Gradle Wrapper.
- JDK 17.
- Android SDK 34 para compilar; `minSdk` 21 y `targetSdk` 34.
- Un dispositivo o emulador para las pruebas instrumentadas.

Las versiones principales están centralizadas en
[`gradle/libs.versions.toml`](gradle/libs.versions.toml): Kotlin 1.9.24, Android
Gradle Plugin 8.13.2, Room 2.6.1, WorkManager 2.9.1 y
`material-calendar-view` 1.9.2.

## Compilar y probar

Desde la raíz del repositorio:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

`testDebugUnitTest` valida las reglas de dominio, la política de alarmas y los
flujos principales del ViewModel sin dispositivo. Las pruebas instrumentadas
comprueban Room, la compatibilidad de la base heredada, permisos y receivers,
por lo que necesitan un emulador o dispositivo conectado. La guía de desarrollo
describe el orden recomendado y la validación manual
([`docs/desarrollo.md`](docs/desarrollo.md)).

## Persistencia y compatibilidad

La base local se llama `app_database`. Room está en la versión 3 y conserva las
tablas históricas `cycles` y `notes`. La migración 1→2 añade las tablas de
tratamientos y tomas; la migración 2→3 convierte las fechas heredadas en claves
de calendario estables, sin borrar los datos existentes. El detalle del esquema
y de las migraciones está en [arquitectura](docs/arquitectura.md).

Android puede incluir la base en sus mecanismos de copia de seguridad según la
configuración del sistema. El proyecto no implementa exportación manual,
sincronización, cuentas ni estadísticas remotas.

## Documentación disponible

- [Manual funcional](docs/funcionalidades.md): qué puede hacer la persona usuaria
  y cómo evolucionan los estados.
- [Arquitectura técnica](docs/arquitectura.md): capas, estado, datos y
  notificaciones.
- [Reglas de negocio](docs/reglas-negocio.md): validaciones y constantes de
  cálculo.
- [Guía de desarrollo](docs/desarrollo.md): convenciones, pruebas y entrega.
- [Matriz de trazabilidad](docs/trazabilidad.md): relación entre funciones,
  implementación y pruebas.
- [Tareas pendientes](docs/tareas-pendientes.md): decisiones cerradas,
  correcciones, tareas funcionales y mejoras futuras.
- [Problemas conocidos y limitaciones](docs/problemas-conocidos.md): límites
  derivados de Android.

## Identidad de la aplicación

El `applicationId` y el `namespace` son `com.example.ciclomenstrual`. Se conserva
el nombre de base `app_database` para mantener la compatibilidad con datos
creados por la versión anterior.
