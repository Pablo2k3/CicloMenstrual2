# Reglas de negocio

Estas reglas son la referencia para el comportamiento de la aplicación. Las
clases de `domain` deben seguir siendo puras y sus cambios deben acompañarse de
pruebas unitarias.

## Fechas

- Las fechas elegidas en calendario, notas y marcadores se convierten desde la
  zona horaria local a una clave estable de día almacenada en medianoche UTC.
- Para sumar días se usa `Calendar.add(Calendar.DAY_OF_MONTH, ...)` sobre la
  clave estable, de modo que los cambios de mes y año se tratan como días de
  calendario y no como intervalos de milisegundos.
- Los instantes de toma y notificación se persisten como milisegundos Unix;
  nunca se reutilizan como fecha de calendario.
- El calendario visual admite una ventana de seis meses hacia atrás y seis hacia
  delante desde el momento en que se configura.

## Hora de referencia de la pauta

- La referencia funcional es la hora peninsular española, no una hora fija
  independiente del calendario de verano.
- En horario de verano español la dosis de referencia es a las 14:00.
- Fuera del horario de verano español la dosis de referencia es a las 13:00.
- Cuando el dispositivo está en otra zona horaria, la aplicación debe convertir
  esa hora de referencia a la hora local del dispositivo. Tres horas menos que
  España implica 11:00 en verano y 10:00 en invierno.
- Esta decisión no define un intervalo absoluto de 24 horas entre instantes; fija
  la hora de referencia española y su representación local.
- El cálculo usa `Europe/Madrid` como referencia: 14:00 durante el horario de
  verano y 13:00 fuera de él. El instante se convierte automáticamente a la
  zona horaria del dispositivo; tres horas menos implica 11:00/10:00.

## Ciclos

- Un ciclo se identifica persistentemente por la clave de calendario de su
  inicio, que es la clave primaria de `cycles`.
- `cycles.end_date = 0` representa un ciclo sin finalizar y se expone como
  `endDate = null` en el dominio.
- Los ciclos se cargan y presentan ordenados por fecha de inicio ascendente.
- Solo el último ciclo sin fin puede ser el ciclo incompleto seleccionado al
  cargar la aplicación.
- El inicio y el fin no pueden ser posteriores a hoy.
- El fin no puede ser anterior al inicio.
- El fin debe ser como máximo el inicio del ciclo siguiente; un fin posterior
  produciría un solapamiento y se rechaza.
- El intervalo de un ciclo completo es inclusivo en ambos extremos.
- Un ciclo incompleto se pinta como activo únicamente si es el último ciclo y
  `daysBetween(start, now) < 7`.
- Si el inicio tiene siete días o más de antigüedad, el ciclo no se persiste por
  sí solo; solo se conserva si se completa en la misma sesión.
- El rango visual del ciclo activo empieza en su fecha de inicio y llega hasta
  hoy; no se proyecta hacia fechas futuras.

## Notas

- Una nota tiene identificador, fecha y contenido.
- No se guardan contenidos vacíos o formados solo por espacios.
- Se conserva el contenido original después de pasar la validación.
- La eliminación usa el identificador persistente de la nota; dos notas con la
  misma fecha y contenido siguen siendo registros independientes.
- `RoomNote.content` puede ser nullable para abrir el esquema heredado, pero el
  modelo de dominio convierte `null` en cadena vacía.

## Predicción y recordatorio de ciclo

- La política actual no aprende del historial: suma 28 días naturales al inicio
  del último ciclo registrado.
- Se calcula una predicción incluso si el último ciclo sigue incompleto.
- Si el día previsto ya llegó o pasó, se muestra como vencido y no se programa
  un aviso nuevo.
- Cuando se solicita o actualiza el recordatorio y la predicción es futura, se
  solicita a WorkManager un trabajo para el día anterior a las 08:15.
- WorkManager ofrece una ejecución diferida, no una garantía de precisión al
  minuto.

## Pauta anticonceptiva

- Cada tratamiento comienza en la fecha de la pastilla 1.
- Los valores predeterminados son 21 días activos y 7 días de placebo. El modelo
  conserva los parámetros de la pauta en la base para cada tratamiento.
- El número de pastilla es `(días desde el inicio % (activos + placebos)) + 1`.
- Las pastillas 1–21 son activas y las 22–28 son placebo con la configuración
  predeterminada.
- Después de la última pastilla del envase se vuelve a la número 1.
- Una fecha fuera del intervalo de un tratamiento no tiene `PillDay`.
- Un tratamiento nuevo cierra el tratamiento activo el día anterior a su inicio
  y conserva las tomas anteriores.
- El nuevo inicio no puede ser igual o anterior al inicio del tratamiento
  activo.

## Estados y tomas

- `UPCOMING`: fecha futura o fecha actual anterior a la hora de dosis.
- `PENDING`: pastilla activa desde la dosis hasta antes de la fecha límite.
- `TAKEN`: existe una toma persistida con estado `TAKEN`.
- `MISSED`: existe una toma persistida con estado `MISSED` o ha pasado la ventana
  de dos horas sin confirmación.
- `AUTO_PLACEBO`: placebo después de la hora de dosis; se registra de forma
  automática.
- La fecha límite de una activa es dos horas después de la dosis: 16:00 en
  Madrid durante el verano, 15:00 en invierno y la hora equivalente en el
  dispositivo.
- Solo las activas de hoy y de fechas pasadas pueden modificarse desde la
  tarjeta. Las futuras no se pueden adelantar y los placebos no se editan
  manualmente.
- Una toma confirmada desde el calendario usa el origen `CALENDAR`; la acción
  de notificación usa `NOTIFICATION`; la reconciliación usa `SYSTEM`; la carga
  inicial de un tratamiento histórico usa `INITIALIZATION`.

## Alarmas y permisos

- El scheduler de pastillas mantiene una única alarma futura y reemplaza la
  anterior cada vez que cambia el estado.
- La primera alarma de una activa es a la hora de referencia convertida a la
  zona del dispositivo: 14:00 en verano y 13:00 en invierno en España.
- Se repite cada 15 minutos durante la ventana de dos horas y se procesa
  `DEADLINE` al terminarla. En Madrid la ventana objetivo es 14:00–16:00 en
  verano y 13:00–15:00 en invierno.
- La planificación usa el instante calculado desde `Europe/Madrid` y lo
  convierte a la zona del dispositivo.
- Un placebo se registra automáticamente al procesarse su alarma y avanza al
  siguiente día.
- En Android 12+ se intenta usar una alarma exacta si existe el permiso especial
  de alarmas; si no existe, se usa una alarma aproximada y se muestra una
  advertencia.
- En Android 13+ las notificaciones requieren `POST_NOTIFICATIONS`. Denegar ese
  permiso no desactiva la persistencia ni los cálculos.
- Tras reinicio, cambio de hora, cambio de zona horaria, actualización del
  paquete o cambio del permiso de alarmas se reconcilian fechas vencidas y se
  programa de nuevo el siguiente evento.

## Privacidad y compatibilidad

- Ciclos, notas, tratamientos y tomas permanecen en la base privada local
  `app_database`.
- La base Room está en la versión 3.
- La migración 1→2 crea únicamente las tablas de tratamientos y tomas y no
  modifica ciclos ni notas heredados; la 2→3 convierte sus fechas a claves de
  calendario estables.
- No hay cuenta, API remota, exportación manual ni recomendaciones clínicas
  sobre dosis omitidas.
