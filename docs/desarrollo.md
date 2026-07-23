# Guía de desarrollo

## Convenciones

- Kotlin idiomático, modelos inmutables y dependencias dirigidas hacia `domain`.
- Ninguna regla de negocio ni acceso a datos dentro de Activities, adapters o
  Workers.
- Los DAO son suspendibles; no se crean executors manuales.
- Los textos visibles pertenecen a recursos, salvo contenido de notificaciones
  que deba ser compatible con trabajos ya encolados.
- Cada cambio de comportamiento requiere actualizar documentación, pruebas y
  trazabilidad.

## Añadir una función

1. Definir modelo, regla o interfaz en `domain`.
2. Añadir la implementación de datos o infraestructura necesaria.
3. Exponer la acción y el resultado en `MainViewModel`/`MainUiState`.
4. Renderizar el estado desde la vista sin duplicar reglas.
5. Añadir pruebas unitarias y, si hay interacción Android, instrumentadas.

## Verificación

Ejecutar pruebas unitarias en cada cambio. Antes de publicar, ejecutar también
pruebas instrumentadas, `lintDebug` y `assembleRelease`. La validación manual debe
cubrir recreación de actividad, cambio de zona horaria, cambio de año, permiso de
notificaciones denegado y apertura con una base creada por la versión Java.

## Entrega incremental

Los commits deben mantener el proyecto compilable y separar configuración,
dominio, datos, presentación y documentación. No se debe aprovechar una
refactorización para cambiar reglas o corregir anomalías sin una decisión de
producto y pruebas de aceptación nuevas.
