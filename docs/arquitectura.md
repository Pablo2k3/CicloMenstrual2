# Arquitectura técnica

## Capas

```text
MainActivity (Views/XML)
        │ acciones / estado
        ▼
MainViewModel ── MainUiState + MainUiEvent
        │
        ├── reglas y modelos de domain
        ├── CycleRepository / NoteRepository
        │          └── implementaciones Room
        └── CycleReminderScheduler
                   └── WorkManager
        └── PillReminderScheduler
                   └── AlarmManager + BroadcastReceivers
```

- **presentation** contiene el estado completo, eventos de una sola ejecución,
  adaptadores y coordinación ligada al ciclo de vida.
- **domain** contiene modelos inmutables, normalización de fechas, validaciones,
  predicción y generación determinista de marcadores. No depende de Android.
- **data** contiene entidades y DAO Room más los adaptadores a modelos de dominio.
- **notifications** encapsula canal, Worker y programación/cancelación.

La actividad no accede a Room ni ejecuta reglas. Recoge `StateFlow` solamente
mientras está iniciada. El `ViewModel` usa `viewModelScope`; los DAO suspendibles
de Room realizan el trabajo fuera del hilo de interfaz.

## Modelo de datos

La base se llama `app_database` y está en la versión 2:

```text
cycles(start_date INTEGER PRIMARY KEY, end_date INTEGER NOT NULL)
notes(id INTEGER PRIMARY KEY AUTOINCREMENT, date INTEGER NOT NULL, content TEXT)
contraceptive_regimens(id, start_date, end_date, dose_hour, dose_minute,
                       active_days, placebo_days)
pill_intakes(regimen_id, scheduled_date, pill_number, status, taken_at, source)
```

La migración 1→2 solo añade las tablas anticonceptivas; no modifica ciclos ni
notas. Todo cambio futuro debe incluir una `Migration` explícita y una prueba.

## Estado y flujo

`MainUiState` incluye carga, fecha seleccionada, ciclos, notas, tratamientos,
tomas, pastilla seleccionada, permiso exacto y marcadores. Tras cada escritura se actualiza
el estado inmutable y se recalculan los marcadores. Los mensajes transitorios se
emiten mediante `MainUiEvent`, evitando que reaparezcan al recrear la actividad.

`PillReminderPlanner` calcula el siguiente evento sin Android. El planificador
mantiene una sola alarma futura; los receivers procesan avisos, confirmaciones,
fecha límite y reprogramación tras reinicio, cambio horario o actualización.
