# Tareas pendientes

Este es el backlog funcional y técnico de referencia. Las decisiones de la
primera sección están cerradas y no deben volver a clasificarse como errores en
revisiones posteriores. La tabla conserva tanto las correcciones ya completadas
como el trabajo que todavía necesita pruebas o una decisión técnica.

## Decisiones funcionales cerradas

- Un ciclo iniciado hace siete días o más no se persiste por sí solo. Solo se
  conserva si se completa en la misma sesión.
- Al introducir un tratamiento con fecha pasada, las pastillas activas
  anteriores se consideran tomadas y los placebos se registran automáticamente.
- `Ciclo Monstrual` es el nombre visible intencionado y el juego de palabras no
  debe corregirse.
- La hora de referencia de la pauta es la hora peninsular española: 14:00
  durante el horario de verano y 13:00 fuera de él. Al viajar, el aviso se
  convierte a la zona horaria del dispositivo; por ejemplo, tres horas menos
  implica 11:00 en verano y 10:00 en invierno.

La eliminación de notas no forma parte de estas decisiones cerradas. La
implementación actual usa el identificador persistente para no borrar
duplicados no seleccionados, pero la decisión de producto sigue documentada
como tarea revisable y no debe reinterpretarse como una decisión funcional
validada.

## Tareas técnicas y funcionales

| ID | Prioridad | Estado | Causa | Tarea | Criterio de aceptación |
|---|---|---|---|---|---|
| T01 | Alta | Completada | El cambio de inicio usaba la fecha nueva al borrar la fila anterior. | Corregir el cambio de inicio de ciclo para borrar el registro antiguo y no usar la fecha nueva como clave. | Tras cambiar el inicio, el registro antiguo no reaparece al recargar y el nuevo ciclo se conserva correctamente. |
| T02 | Alta | Completada | Los trabajos de WorkManager anteriores no tenían una identidad única reemplazable. | Sincronizar los recordatorios de ciclo cuando cambia el último ciclo o la predicción deja de ser futura. | Nunca queda un aviso para una predicción antigua y solo existe el recordatorio correspondiente a la predicción vigente. |
| T03 | Alta | Completada | La pauta calculaba una hora local fija y no una hora española de referencia. | Calcular la dosis con `Europe/Madrid` como referencia y convertir el instante a la zona horaria del dispositivo. | En Madrid son las 14:00 durante el horario de verano y las 13:00 en invierno; tres horas al oeste son las 11:00 y 10:00 respectivamente. |
| T04 | Alta | Completada | Las fechas de día se guardaban como medianoches que podían reinterpretarse al viajar. | Separar la fecha de calendario de los instantes de notificación para evitar desplazamientos al cambiar de zona horaria. | El historial conserva su día y las alarmas se recalculan de forma determinista después de viajar o cambiar la zona del dispositivo. |
| T05 | Media | Completada | Las alarmas y acciones antiguas no comprobaban siempre el tratamiento activo ni el estado persistido. | Cancelar avisos antiguos al confirmar una toma desde el calendario o al cambiar de tratamiento, y validar acciones de receivers obsoletas. | Una acción de una notificación anterior no modifica un tratamiento cerrado ni reemplaza la alarma del tratamiento activo. |
| T06 | Media | Completada | La eliminación por fecha y contenido no distinguía notas duplicadas. | Definir una identidad inequívoca para las notas y dejar de borrar duplicados no seleccionados. | Eliminar una nota borra únicamente la nota elegida, incluso si existen otras con la misma fecha y contenido. |
| T07 | Media | Completada | La acción de eliminar un ciclo no tenía una segunda confirmación explícita. | Pedir confirmación antes de eliminar un ciclo completo. | Un toque accidental no elimina historial sin una segunda confirmación explícita. |
| T08 | Media | Completada | La cobertura de pruebas específicas de ViewModel, receivers, permisos y ejecución en dispositivo estaba pendiente de cerrar. | Completar pruebas de ViewModel, receivers, permisos, cambios de zona horaria y transiciones de horario de verano. | Los flujos críticos tienen pruebas automatizadas o casos manuales documentados y reproducibles; las pruebas instrumentadas se ejecutan correctamente en un dispositivo conectado. |
| T09 | Media | Completada | La capa de selección solo encontraba el número y no la celda completa. | Resaltar la fecha elegida también al tocar zonas intermedias de la celda del calendario. | El círculo azul aparece tanto al tocar el número como cualquier margen que la librería use para resolver ese día. |
| T10 | Media | Completada | El modo oscuro heredaba fondos blancos y colores sin adaptar de la librería y de la pantalla principal. | Aplicar una paleta oscura coherente a superficies, textos, marcadores, avisos y barras del sistema. | El modo claro conserva sus colores y superficies anteriores; en modo oscuro no quedan superficies blancas inesperadas y el contraste sigue siendo legible. |

## Mejoras futuras

| ID | Prioridad | Estado | Causa | Mejora | Criterio de aceptación |
|---|---|---|---|---|---|
| M01 | Baja | Futuro | La pauta está fijada a 21 activas, 7 placebos y la hora de referencia española. | Permitir configurar la duración de la pauta y la hora de recordatorio. | La configuración se valida, se persiste por tratamiento y se refleja en cálculo, estados y avisos. |
| M02 | Baja | Futuro | La predicción actual siempre suma 28 días al último inicio. | Mejorar la política de predicción para que pueda usar el historial. | La política configurable conserva el comportamiento actual por defecto y tiene pruebas de sus alternativas. |
| M03 | Baja | Futuro | Los datos solo pueden consultarse en la aplicación y no hay salida manual. | Añadir exportación local de datos. | La persona puede generar un archivo local legible sin enviar información a un servidor. |

## Limitaciones de plataforma

Estas limitaciones no son bugs del dominio de la aplicación:

- WorkManager no garantiza la ejecución exacta al minuto.
- Sin permiso de alarmas exactas, Android puede retrasar los avisos de pastilla.
- Algunos fabricantes aplican restricciones de batería que pueden retrasar
  trabajos y alarmas.
