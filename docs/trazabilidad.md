# Matriz de trazabilidad

La matriz conecta cada comportamiento con su punto de entrada principal y con
la verificación disponible actualmente.

| ID | Función o regla | Implementación principal | Verificación actual |
|---|---|---|---|
| F01 | Cargar ciclos, notas y tratamientos | `MainViewModel.load`, repositorios Room | `AppDatabaseCompatibilityTest` cubre lectura básica; flujo completo pendiente |
| F02 | Seleccionar día y listar notas | `MainViewModel.selectDate`, `MainUiState` | `MainViewModelTest` |
| F03 | Iniciar o cambiar ciclo | `CycleRules`, `MainViewModel.markCycleStart` | `CycleRulesTest` |
| F04 | Finalizar y validar ciclo | `CycleRules`, `MainViewModel.markCycleEnd` | `CycleRulesTest` |
| F05 | Eliminar ciclo y recalcular aviso | `RoomCycleRepository`, `MainViewModel.deleteCycle`, `WorkManagerCycleReminderScheduler` | `MainViewModelTest`; flujo manual documentado |
| F06 | Añadir y eliminar nota por identidad | `RoomNoteRepository`, `NoteDao`, `MainViewModel` | `MainViewModelTest` y `AppDatabaseCompatibilityTest` cubren duplicados por fecha/contenido |
| F07 | Pintar rangos, notas y predicción | `CalendarMarkerFactory`, `MainActivity` | `CalendarMarkerFactoryTest` |
| F08 | Predecir a 28 días | `FixedCyclePredictionPolicy` | `CalendarMarkerFactoryTest` |
| F09 | Mostrar ciclo incompleto durante 7 días | `CycleRules.isOngoing` | `CycleRulesTest` |
| F10 | Programar recordatorio de ciclo | `WorkManagerCycleReminderScheduler`, `NotificationWorker` | Prueba WorkManager pendiente |
| F11 | Conservar esquema histórico v1 | entidades `data.local`, `RoomNote.content` nullable | `AppDatabaseCompatibilityTest` cubre forma heredada |
| F12 | Solicitar permiso Android 13+ | `MainActivity.requestNotificationPermission` | `AndroidConfigurationTest`; `connectedDebugAndroidTest` ejecutado en emulador conectado |
| F13 | Calcular pastillas 1–28 | `PillScheduleCalculator` | `PillScheduleCalculatorTest` |
| F14 | Resolver estado diario | `PillStatusResolver` | `PillStatusResolverTest` |
| F15 | Avisar desde la hora de dosis, repetir durante dos horas y cerrar la ventana | `PillReminderPlanner`, `AlarmManagerPillReminderScheduler` | `PillReminderPlannerTest`; casos DST en `PillScheduleCalculatorTest` |
| F16 | Confirmar desde notificación | `PillActionReceiver`, `PillAlarmPolicy`, `NotificationHelper` | `PillAlarmPolicyTest`; caso manual de acción desde notificación documentado |
| F17 | Migrar Room 1→3 | `AppDatabase.MIGRATION_1_2`, `MIGRATION_2_3` | `AppDatabaseCompatibilityTest` |
| F18 | Conservar tratamientos y tomas | `RoomContraceptiveRepository` | Prueba de repositorio pendiente |
| F19 | Reconciliar después de reinicio o cambio horario | `PillRescheduleReceiver`, `MainViewModel` | `PillAlarmPolicyTest`; casos manuales de después de reinicio y cambio horario documentados |
| F20 | Convertir la hora de referencia de España a la zona del dispositivo | `PillScheduleCalculator`, `AlarmManagerPillReminderScheduler` | `PillScheduleCalculatorTest`: Madrid 14:00/13:00 y viaje 11:00/10:00 |
| F21 | Mantener fechas de calendario al viajar | `DateNormalizer`, `MIGRATION_2_3`, `MainActivity` | `DateNormalizerTest`; migración instrumentada |
| F22 | Validar permisos y receivers declarados | `AndroidManifest.xml`, `PillAlarmReceiver`, `PillActionReceiver`, `PillRescheduleReceiver` | `AndroidConfigurationTest`; `connectedDebugAndroidTest` ejecutado en `Pixel_7 (AVD) - 16` |
| F23 | Resaltar el día seleccionado también al tocar el margen de su celda | CalendarSelectionOverlay, MainActivity | Validación manual de toque sobre el número y sobre las zonas intermedias de la celda |
| F24 | Mantener una apariencia coherente en modo claro y oscuro | temas, colores values-night, activity_main.xml, CalendarInterop | lintDebug, assembleDebug; validación visual manual en ambos modos |

Las filas pendientes no indican que la función no exista; indican que todavía no
hay una prueba automatizada específica para ese flujo. La suite instrumentada de
configuración, permisos y migraciones ya se ha ejecutado en un emulador; antes de
publicar una versión conviene repetir los casos manuales de receivers,
recreación de actividad y restricciones de batería del fabricante.
