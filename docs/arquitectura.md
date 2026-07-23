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

La base se llama `app_database` y mantiene la versión 1:

```text
cycles(start_date INTEGER PRIMARY KEY, end_date INTEGER NOT NULL)
notes(id INTEGER PRIMARY KEY AUTOINCREMENT, date INTEGER NOT NULL, content TEXT)
```

No se ha realizado una migración destructiva ni se han renombrado tablas o
columnas. Todo cambio futuro del esquema debe incluir una `Migration` explícita y
una prueba que abra una base de la versión anterior.

## Estado y flujo

`MainUiState` incluye carga, fecha seleccionada, ciclos, notas, notas visibles,
marcadores, ciclo activo y predicción. Tras cada escritura confirmada se actualiza
el estado inmutable y se recalculan los marcadores. Los mensajes transitorios se
emiten mediante `MainUiEvent`, evitando que reaparezcan al recrear la actividad.
