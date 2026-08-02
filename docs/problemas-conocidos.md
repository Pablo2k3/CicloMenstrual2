# Problemas conocidos y limitaciones

El backlog de correcciones, tareas funcionales y mejoras está centralizado en
[tareas-pendientes.md](tareas-pendientes.md). Este documento conserva únicamente
limitaciones dependientes del sistema Android:

- WorkManager no garantiza la ejecución exacta del recordatorio de ciclo.
- Sin acceso a alarmas exactas, Android puede retrasar los avisos de pastilla.
- Algunos fabricantes aplican restricciones de batería adicionales; al abrirse,
  la aplicación reconcilia tomas omitidas y placebos, pero el sistema todavía
  puede retrasar la alarma.

Las decisiones funcionales aceptadas y los problemas de implementación no deben
duplicarse aquí; se mantienen únicamente en el backlog canónico.
