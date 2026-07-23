# Problemas conocidos y backlog

Estos comportamientos se conservan para que el refactor no cambie funcionalidad:

- Cambiar el inicio de un ciclo incompleto intenta borrar en la base usando la
  nueva fecha en vez de la fecha antigua. Puede dejar un registro anterior.
- La eliminación de una nota por fecha y contenido elimina también duplicados
  idénticos.
- La predicción usa siempre 28 días y no aprende del historial.
- El recordatorio es aproximado porque WorkManager no garantiza ejecución exacta.
- La selección de fechas compara timestamps locales; cambiar la zona horaria puede
  desplazar la representación de datos históricos.
- Un ciclo incompleto de siete días o más deja de pintarse como activo, pero sigue
  existiendo como ciclo sin finalizar.
- Sin acceso a alarmas exactas Android puede retrasar los avisos.
- Algunos fabricantes aplican restricciones de batería adicionales; al abrirse,
  la aplicación reconcilia tomas omitidas y placebos.

## Deuda eliminada

- Actividad monolítica con acceso directo a Room y WorkManager.
- Estado mutable duplicado entre base, mapas, adaptador y calendario.
- Executor manual y callbacks al hilo principal.
- Adaptador que eliminaba por posición.
- Entidades Room usadas como modelos de interfaz.
- Navegación, menú, dependencias y textos de plantilla sin uso.

## Mejoras futuras

Corregir la edición del inicio, definir una identidad de nota inequívoca, permitir
configurar duración y recordatorio, mejorar la política de predicción, exportar
datos y definir una estrategia explícita ante cambios de zona horaria.
