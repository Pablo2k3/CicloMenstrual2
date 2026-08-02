# Arquitectura técnica

La aplicación es un módulo Android de Views/XML organizado alrededor de un
`ViewModel`, modelos de dominio independientes de Android y repositorios que
encapsulan Room. Las notificaciones se mantienen fuera de la pantalla porque
pueden ejecutarse cuando la actividad no está abierta.

## Capas y responsabilidades

```text
MainActivity + Views/XML
        │ eventos de usuario / renderizado de StateFlow
        ▼
MainViewModel ─────────────── MainUiState + MainUiEvent
        │
        ├── domain
        │     ├── reglas de ciclos y fechas
        │     ├── marcadores del calendario
        │     └── cálculo, estado y planificación de pastillas
        │
        ├── CycleRepository / NoteRepository /
        │   ContraceptiveRepository
        │           └── implementaciones Room
        │
        ├── CycleReminderScheduler
        │           └── WorkManager + NotificationWorker
        │
        └── PillReminderScheduler
                    └── AlarmManager + BroadcastReceivers
```

### `presentation`

- `MainActivity` infla `activity_main.xml`, conecta eventos de calendario y
  diálogos y observa partes de `MainUiState` mientras el ciclo de vida está en
  `STARTED`.
- `MainViewModel` coordina lecturas, escrituras, validaciones, reconstrucción de
  marcadores y programación de recordatorios. Usa `viewModelScope` para que las
  operaciones sobrevivan a una recreación de la actividad.
- `MainUiState` contiene la fuente de verdad de la pantalla: ciclos, notas,
  fecha seleccionada, marcadores, tratamientos, tomas, estado de pastilla y
  permiso de alarma exacta.
- `MainUiEvent` se entrega por un `Channel` para mensajes transitorios, como
  errores de validación, sin repetirlos al recrear la actividad.
- `NotesAdapter` solo renderiza notas y devuelve la intención de eliminar; no
  accede a Room ni decide reglas.
- `CalendarInterop` concentra la llamada Java necesaria para la API heredada de
  `material-calendar-view`. `CalendarSelectionOverlay` dibuja la selección sin
  pedir al adaptador del calendario que reconstruya sus páginas.

### `domain`

Esta capa no depende de Android. Contiene modelos inmutables (`Cycle`, `Note`,
`ContraceptiveRegimen`, `PillIntake`, `PillDay`) e implementa:

- `DateNormalizer`, que representa cada fecha de calendario como una clave
  estable en medianoche UTC, convierte las entradas de la interfaz desde la
  zona local y avanza con `Calendar.add` sin reinterpretar el historial;
- `CycleRules`, que valida fechas y determina cuándo un ciclo incompleto sigue
  siendo visualmente activo;
- `CyclePredictionPolicy`, cuya implementación actual suma 28 días al inicio
  del último ciclo;
- `CalendarMarkerFactory`, que combina rangos de ciclos, predicción, notas y
  pastillas en un único marcador por fecha;
- `PillScheduleCalculator`, que convierte una fecha en número de pastilla y hora
  de dosis. La referencia es `Europe/Madrid`: 14:00 en horario de verano y
  13:00 fuera de él; el instante resultante se muestra y programa en la zona
  del dispositivo;
- `PillAlarmPolicy`, que decide si una alarma o acción todavía corresponde al
  tratamiento, día, pastilla y ventana actuales antes de que Android toque la
  persistencia;
- `PillStatusResolver`, que convierte una toma almacenada y la hora actual en
  un estado de interfaz;
- `PillReminderPlanner`, que calcula el siguiente evento sin usar APIs de
  Android. Esto permite probar la secuencia de alarmas con pruebas unitarias.

### `data`

`data.local` contiene las entidades Room y los DAO. `data.repository` traduce
esas entidades a modelos de dominio y oculta los detalles de SQL:

- `RoomCycleRepository` trabaja con el sentinel `end_date = 0` para representar
  un ciclo incompleto;
- `RoomNoteRepository` convierte el contenido nullable del esquema heredado en
  una cadena vacía en el dominio;
- `RoomContraceptiveRepository` gestiona tratamientos y tomas y usa una
  transacción al cerrar el tratamiento anterior, insertar el nuevo y guardar su
  historial inicial.

### `notifications`

- `WorkManagerCycleReminderScheduler` programa un trabajo único etiquetado para
  el día anterior a la predicción. `NotificationWorker` muestra el aviso y
  mantiene un texto de reserva para trabajos antiguos que no llevasen mensaje.
- `AlarmManagerPillReminderScheduler` cancela la alarma anterior, obtiene el
  siguiente plan del dominio y mantiene una sola alarma futura. Usa la alarma
  exacta cuando el sistema lo permite y una aproximada como fallback.
- `PillAlarmReceiver` procesa la hora de dosis, las repeticiones y el cierre de
  ventana; después calcula y programa el siguiente evento.
- `PillActionReceiver` recibe la acción `Tomada` de la notificación, guarda la
  toma y reprograma.
- `PillRescheduleReceiver` reconstruye el estado después de reinicio, ajuste de
  hora, cambio de zona horaria, actualización de paquete o cambio de permiso de
  alarmas exactas.
- `NotificationHelper` crea los canales, comprueba `POST_NOTIFICATIONS` y
  centraliza la creación/cancelación de avisos.

## Arranque y flujo de estado

El arranque sigue esta secuencia:

1. `MainActivity` crea u obtiene la instancia singleton de `AppDatabase` y
   construye los repositorios y planificadores.
2. La creación de `MainViewModel` inicia `load()` en `viewModelScope`.
3. `load()` lee ciclos, notas, tratamientos y tomas.
4. Si hay tratamiento activo, reconcilia fechas vencidas que todavía no tengan
   toma persistida: placebos automáticos y activas omitidas después de dos horas.
5. Construye `PillDay` para la ventana de seis meses alrededor de hoy y calcula
   los marcadores del calendario.
6. Consulta el permiso de alarmas exactas y programa el siguiente evento de
   pastilla.
7. La actividad observa `StateFlow` y actualiza cada componente sin leer Room
   directamente.

Cada escritura vuelve a cargar o reconstruir la parte derivada afectada:

```text
acción de UI
   ▼
MainViewModel valida y persiste mediante un repositorio
   ▼
recarga ciclos/notas o tratamientos/tomas
   ▼
recalcula selección, estados, marcadores y siguiente aviso
   ▼
StateFlow → Activity → vistas
```

Además, el `ViewModel` actualiza los estados derivados de pastillas cada minuto
para que `Próxima`, `Pendiente` y `Omitida` cambien aunque la persona no toque la
pantalla. El bucle se puede desactivar únicamente en las pruebas unitarias para
que el `TestDispatcher` pueda cerrar cada escenario de forma determinista; en
la aplicación está activado por defecto.

## Cálculo de marcadores

`CalendarMarkerFactory.create()` combina la información en un `LinkedHashMap`
indexado por fecha normalizada:

1. añade todos los días de cada ciclo completo como `PERIOD`;
2. añade el inicio de cada ciclo incompleto y, si es el último y sigue activo,
   los días hasta hoy como `ONGOING`;
3. calcula la predicción del último ciclo y marca el día como `PREDICTED`, además
   de indicar si ya venció;
4. incorpora el indicador de notas;
5. adjunta el `PillDay` correspondiente sin añadir símbolos propios.

La actividad convierte cada `CalendarMarker` en un `CalendarDay` de la librería.
El color de fondo representa ciclos o predicción, el color naranja del número
representa una activa omitida en una fecha pasada y los iconos de texto se usan
solo para notas y predicción vencida.

## Modelo de datos y esquema Room

La base se llama `app_database`, exporta cuatro entidades y está en la versión 3:

```text
cycles
  start_date INTEGER NOT NULL PRIMARY KEY
  end_date   INTEGER NOT NULL              -- 0 = incompleto

notes
  id         INTEGER PRIMARY KEY AUTOINCREMENT
  date       INTEGER NOT NULL
  content    TEXT                         -- nullable por compatibilidad

contraceptive_regimens
  id          INTEGER PRIMARY KEY AUTOINCREMENT
  start_date  INTEGER NOT NULL
  end_date    INTEGER
  dose_hour   INTEGER NOT NULL             -- legado; la referencia actual usa Madrid
  dose_minute INTEGER NOT NULL             -- legado; la referencia actual usa 00
  active_days INTEGER NOT NULL             -- por defecto 21
  placebo_days INTEGER NOT NULL            -- por defecto 7

pill_intakes
  regimen_id     INTEGER NOT NULL
  scheduled_date INTEGER NOT NULL
  pill_number    INTEGER NOT NULL
  status         TEXT NOT NULL              -- TAKEN/MISSED/AUTO_PLACEBO
  taken_at       INTEGER
  source         TEXT NOT NULL              -- origen de la toma
  PRIMARY KEY (regimen_id, scheduled_date)
  FOREIGN KEY (regimen_id) REFERENCES contraceptive_regimens(id) ON DELETE CASCADE
```

Las columnas que representan días (`start_date`, `end_date`, `date` y
`scheduled_date`) almacenan claves de calendario: milisegundos de la medianoche
UTC que contienen los campos de año, mes y día, no un instante de notificación.
Las entradas de calendario se convierten desde la zona del dispositivo antes de
persistirse y la interfaz vuelve a convertir la clave a medianoche local solo
para dibujarla. Los instantes reales de dosis, toma y alarma siguen siendo
milisegundos Unix independientes de esas claves. Las notas mantienen el
contenido nullable en la entidad Room para poder abrir el esquema generado por
la versión Java anterior; el modelo de dominio expone `content` no nullable.

`MIGRATION_1_2` crea `contraceptive_regimens`, `pill_intakes` y el índice de
`regimen_id`. No modifica `cycles` ni `notes`. `MIGRATION_2_3` convierte las
fechas que guardaba la versión anterior como medianoches locales a claves
estables, interpretándolas con `Europe/Madrid`. Toda futura modificación del
esquema debe añadir una migración explícita y una prueba de compatibilidad.

## Programación de recordatorios

### Ciclo

`WorkManagerCycleReminderScheduler` calcula las 08:15 del día anterior al inicio
previsto. Si la predicción no es posterior al final del día actual, no crea un
trabajo. Antes de programar cancela los trabajos con la etiqueta
`cycle_notification`, por lo que no se acumulan avisos obsoletos.

### Pastilla

`PillReminderPlanner` calcula la transición según la hora actual y las tomas
persistidas:

```text
antes de la dosis        DOSE a la hora de dosis
durante la ventana       REMINDER en el siguiente cuarto de hora
después de la ventana    DEADLINE al terminar las dos horas
pastilla completada      DOSE del siguiente día aplicable
```

La hora de referencia funcional es 14:00 en España durante el horario de verano
y 13:00 fuera de él. En otra zona se programa el instante equivalente del
dispositivo, por ejemplo 11:00/10:00 si hay tres horas menos. No se trata de
imponer una separación absoluta de 24 horas: la hora civil española cambia al
entrar o salir del horario de verano para conservar el instante equivalente.

El receiver guarda `MISSED` al procesar `DEADLINE`; para un placebo guarda
`AUTO_PLACEBO`. El scheduler usa un único `PendingIntent` identificable para
cancelar y reemplazar la alarma. En Android 12+ consulta
`AlarmManager.canScheduleExactAlarms()` y, si no tiene permiso, usa
`setAndAllowWhileIdle` como alternativa.

## Dependencias y pruebas

El módulo usa Kotlin, Android Views/XML, ViewBinding, AndroidX Lifecycle,
coroutines, Room, WorkManager, RecyclerView, Material Components y
`material-calendar-view`. La lógica de domain se valida con pruebas unitarias de
ciclos, marcadores, claves de fecha, cálculo de pauta, estados, política de
alarmas y `MainViewModel`. La compatibilidad de Room 1→3 y la configuración de
permisos y receivers se valida con pruebas instrumentadas
(`AppDatabaseCompatibilityTest`, `AndroidConfigurationTest`) cuando hay un
dispositivo conectado.

Las convenciones para cambios y los comandos de verificación están en la
[guía de desarrollo](desarrollo.md).
