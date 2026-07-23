# Matriz de trazabilidad

| ID | Función/regla | Implementación principal | Verificación |
|---|---|---|---|
| F01 | Cargar ciclos y notas | `MainViewModel`, repositorios Room | Prueba DAO/instrumentada pendiente |
| F02 | Seleccionar día y listar notas | `selectDate`, `MainUiState` | Prueba de ViewModel pendiente |
| F03 | Iniciar/cambiar ciclo | `CycleRules`, `markCycleStart` | `CycleRulesTest` |
| F04 | Finalizar y validar ciclo | `CycleRules`, `markCycleEnd` | `CycleRulesTest` |
| F05 | Eliminar ciclo | `CycleRepository`, `deleteCycle` | Prueba de ViewModel pendiente |
| F06 | Añadir/eliminar nota | `NoteRepository`, `MainViewModel` | Prueba DAO/instrumentada pendiente |
| F07 | Pintar rangos e iconos | `CalendarMarkerFactory` | `CalendarMarkerFactoryTest` |
| F08 | Predecir a 28 días | `FixedCyclePredictionPolicy` | `CalendarMarkerFactoryTest` |
| F09 | Mostrar ciclo activo 7 días | `CycleRules.isOngoing` | `CycleRulesTest` |
| F10 | Programar recordatorio | `CycleReminderScheduler` | Prueba WorkManager pendiente |
| F11 | Conservar esquema versión 1 | entidades `data.local` | Prueba de migración pendiente |
| F12 | Solicitar permiso Android 13+ | `MainActivity` | Prueba instrumentada pendiente |
| F13 | Calcular pastilla 1–28 | `PillScheduleCalculator` | `PillScheduleCalculatorTest` |
| F14 | Resolver estados diarios | `PillStatusResolver` | `PillStatusResolverTest` |
| F15 | Avisar 14:00–15:45 y cerrar 16:00 | `PillReminderPlanner` | `PillReminderPlannerTest` |
| F16 | Confirmar desde notificación | `PillActionReceiver` | Prueba instrumentada pendiente |
| F17 | Migrar Room 1→2 | `MIGRATION_1_2` | `AppDatabaseCompatibilityTest` |
| F18 | Conservar tratamientos | `ContraceptiveRepository` | Prueba de repositorio pendiente |

Las filas marcadas como pendientes requieren un dispositivo/emulador o la
infraestructura específica indicada antes de considerar una publicación.
