# Manual funcional

Este documento describe el comportamiento que implementa actualmente la
aplicación. Las fechas elegidas se interpretan en la zona horaria local del
dispositivo y se guardan como claves de día estables, no como instantes que
puedan cambiar al viajar. Las notificaciones sí son instantes y se recalculan
para la zona actual del dispositivo.

Las decisiones funcionales aceptadas y el trabajo todavía pendiente se mantienen
en [tareas-pendientes.md](tareas-pendientes.md). Cuando este manual indique una
función pendiente, se distinguirá explícitamente del comportamiento actual.

## Primer arranque y pantalla principal

Al abrirse la aplicación, `MainViewModel` carga ciclos, notas, tratamientos y
tomas desde Room. Si no hay un tratamiento activo, la actividad muestra un
selector para configurar la fecha de la pastilla 1. La fecha no puede ser
posterior a hoy. El diálogo se puede cancelar y la configuración queda
disponible en la tarjeta de pastillas.

La pantalla principal contiene:

- un calendario navegable desde seis meses antes hasta seis meses después del
  momento actual;
- una indicación de la fecha seleccionada;
- una tarjeta con la pauta y el estado de la pastilla del día, si existe;
- una lista de notas del día seleccionado;
- un botón flotante con las operaciones de ciclos y notas.

Al tocar un día, la aplicación convierte la fecha local a su clave de calendario,
actualiza el estado de selección y recalcula qué notas y qué pastilla
corresponden a ese día. La selección visual se dibuja sobre el calendario sin
alterar los marcadores de ciclos o pastillas.

## Selección y apariencia

La selección visual se aplica a la celda completa del día, no solo al rectángulo que ocupa el número. Por eso un toque en el margen o en el espacio entre el número y el icono conserva el círculo azul sobre el día que la librería ha seleccionado.

El tema claro conserva la apariencia anterior. El tema oscuro usa superficies, texto, marcadores y avisos adaptados a fondos oscuros. El fondo interno fijo del calendario también se reemplaza por la superficie de la aplicación.

## Registro de ciclos

El botón flotante ofrece acciones distintas según el día y el estado del último
ciclo:

### Marcar el inicio

`Marcar inicio del ciclo` crea un ciclo sin fecha de fin y lo selecciona. No se
aceptan fechas futuras. Si ya hay un ciclo incompleto seleccionado, la acción se
presenta como `Cambiar inicio de ciclo` y sustituye el inicio que se está
editando.

Un ciclo incompleto se considera visualmente activo solo cuando se cumplen las
dos condiciones siguientes:

1. es el último ciclo registrado;
2. han pasado menos de siete días desde su inicio.

El día inicial se colorea como periodo y los días transcurridos hasta hoy se
colorean como ciclo en curso. A partir del séptimo día el registro no desaparece
del estado de la sesión, pero deja de rellenar automáticamente el rango como
ciclo activo. Por decisión funcional, si se introduce un ciclo con siete días o
más de antigüedad no se persiste por sí solo; solo queda guardado si se completa
en la misma sesión.

### Marcar el fin

`Marcar fin del ciclo` completa el ciclo incompleto seleccionado. La fecha de
fin:

- no puede ser futura;
- no puede ser anterior al inicio;
- no puede rebasar el inicio del ciclo siguiente.

El rango de un ciclo completo incluye tanto el inicio como el fin. Si no hay
inicio seleccionado, la aplicación muestra un mensaje y no escribe nada. Si la
fecha produciría una superposición con el siguiente ciclo, se rechaza y se
descarta de la interfaz el ciclo incompleto que estaba en edición.

### Eliminar un ciclo

Cuando el día seleccionado pertenece a un ciclo completo, la acción de inicio se
convierte en `Eliminar ciclo`. La operación borra el ciclo completo, incluidos
todos los días que cubría, y actualiza inmediatamente el calendario. Si era el
último ciclo, también se cancela y vuelve a calcular el recordatorio de la nueva
predicción, si existe.

## Notas diarias

Desde las opciones del día se puede añadir una nota. El texto vacío o compuesto
solo por espacios se ignora; el texto no vacío se conserva tal como se escribió y
la fecha se guarda normalizada.

Las notas aparecen en una lista asociada al día seleccionado. Cada fila tiene un
botón de eliminación y pide confirmación antes de borrar. Cada nota usa el
identificador generado por Room, por lo que borrar una nota no afecta a otra
con la misma fecha y contenido.

## Calendario y marcadores

Los marcadores se generan en `CalendarMarkerFactory` a partir de ciclos, notas,
la predicción y las tomas de pastillas. La leyenda visible es:

| Apariencia | Significado |
|---|---|
| Rosa intenso | Día incluido en un ciclo completo |
| Rosa claro | Día transcurrido de un ciclo incompleto que aún se considera activo |
| Morado | Inicio previsto del próximo ciclo |
| Azul | Día seleccionado |
| 📝 | El día contiene una o más notas |
| ⚠️ | La fecha prevista del ciclo ya ha pasado |
| Número naranja | Pastilla activa omitida en una fecha pasada |

Las notas y el aviso de predicción pueden coexistir en el mismo día. Las tomas
de pastillas no añaden un icono para no saturar cada celda; su número y estado se
consultan en la tarjeta inferior.

## Predicción del próximo ciclo

La política actual es deliberadamente fija:

```text
predicción = inicio del último ciclo registrado + 28 días naturales
```

Se usa el último ciclo tanto si está completo como si sigue incompleto. La
predicción se recalcula después de cada cambio que modifica el conjunto de
ciclos. Si cae en el día actual o en una fecha pasada, se marca con el icono de
advertencia; no se programa un recordatorio para una fecha que ya no es futura.

Cuando la predicción es futura, `WorkManager` puede programar un aviso para el
día anterior a las 08:15 con el texto «Tu próximo ciclo está previsto para
mañana». El sistema puede ejecutar ese trabajo con retraso, porque WorkManager
no es un reloj exacto.

## Seguimiento de pastillas

### Configuración de la pauta

La configuración solicita la fecha de la pastilla 1. Cada tratamiento usa por
defecto una pauta de 28 días:

| Números | Tipo | Comportamiento |
|---|---|---|
| 1–21 | Activas | Requieren confirmación |
| 22–28 | Placebo | Se registran automáticamente |

Al llegar al día 29, el cálculo vuelve a la pastilla 1. El cálculo del número de
pastilla usa días de calendario y por eso continúa correctamente al cambiar de
mes o de año.

La hora de referencia aceptada para la pauta es la hora peninsular española:

- 14:00 durante el horario de verano;
- 13:00 fuera del horario de verano;
- la hora equivalente en el dispositivo cuando la persona viaja. Por ejemplo,
  un dispositivo situado tres horas por detrás debe avisar a las 11:00 en
  verano y a las 10:00 en invierno.

El cálculo usa `Europe/Madrid` como zona de referencia y convierte el instante a
la zona del dispositivo. La decisión no consiste en mantener una separación
absoluta de 24 horas entre horas locales, sino en conservar el instante
equivalente de la pauta española: al salir del horario de verano, Madrid pasa de
14:00 a 13:00; al volver a entrar, recupera las 14:00.

Si la fecha de inicio es anterior a hoy, la configuración inicial crea el
historial anterior: las pastillas activas se consideran tomadas y los placebos
se registran como automáticos. El día actual no se marca por adelantado.

`Comenzar nuevo tratamiento` pide confirmación, cierra el tratamiento activo el
día anterior a la nueva fecha y conserva todas sus tomas. El nuevo inicio debe
ser posterior al inicio del tratamiento activo.

### Estados de una pastilla

La tarjeta del día puede mostrar estos estados:

- `Próxima`: el día todavía es futuro o aún no ha llegado la hora de dosis;
- `Pendiente`: es una activa y se encuentra desde la hora de dosis hasta antes
  de la fecha límite de dos horas;
- `Tomada`: existe una confirmación guardada;
- `Omitida`: terminó la ventana de dos horas sin confirmación o se guardó ese
  resultado;
- `Placebo automático`: es un placebo después de la hora de dosis.

Las activas de hoy y de días pasados pueden marcarse o desmarcarse desde la
tarjeta. Las fechas futuras no se pueden adelantar y los placebos no ofrecen una
acción manual. Si se desmarca una toma antigua, el estado calculado puede volver
a ser `Omitida` inmediatamente porque la hora límite ya pasó.

El número y el estado se calculan para todos los tratamientos que cubren la
ventana del calendario. Por eso el historial de un tratamiento cerrado sigue
siendo consultable al seleccionar una fecha antigua.

## Avisos y permisos de Android

La aplicación crea dos canales de notificación: recordatorios de ciclo y
recordatorios de pastilla.

### Pastillas

Para una pastilla activa, `AlarmManager` mantiene una única alarma futura. La
ventana se cuenta desde la hora de dosis que corresponda:

```text
hora dosis       aviso de dosis
+15 min          primer aviso de repetición
+30…+105 min     avisos de repetición cada 15 minutos
+120 min         cierre de ventana y registro como omitida
```

La hora de referencia española se convierte a la zona del dispositivo. Por tanto,
en Madrid la ventana es 14:00–16:00 en verano y 13:00–15:00 en invierno; en un
dispositivo tres horas retrasado es 11:00–13:00 o 10:00–12:00 respectivamente.

La acción `Tomada` de la notificación guarda la toma con origen
`NOTIFICATION`, cancela el aviso visible y programa el siguiente evento. Si la
notificación llega después de que la pastilla ya se guardó como tomada, solo se
cancela el aviso y se continúa con el día siguiente.

Los placebos no requieren confirmación: al llegar su hora se guarda una toma con
origen de sistema, se muestra un aviso informativo y se programa el siguiente
día.

En Android 12 o posterior, la puntualidad de las alarmas depende del permiso
especial `Alarmas y recordatorios`. Si no está disponible, la aplicación usa una
alarma permitida pero potencialmente inexacta y muestra una advertencia que abre
la configuración del sistema. En versiones anteriores se usan alarmas exactas
compatibles con la plataforma.

### Ciclos

El recordatorio del ciclo usa una ejecución única de WorkManager a las 08:15 del
día anterior a la predicción. Al completar o eliminar el último ciclo se cancela
el trabajo anterior y se programa de nuevo según la predicción actual.

### Permiso de notificaciones y recuperación

En Android 13 o posterior se solicita `POST_NOTIFICATIONS` al iniciar la
actividad. Si se deniega, los ciclos, notas, tomas y cálculos siguen funcionando;
solo no se pueden mostrar avisos.

Al cargar la aplicación y al volver a primer plano se reconcilian las tomas
históricas que falten: los placebos vencidos se registran como automáticos y las
activas cuya ventana de dos horas terminó se registran como omitidas. También se
reprograma la siguiente alarma. El mismo proceso se activa tras reinicio,
cambio de hora, cambio de zona horaria, actualización de la aplicación o cambio
del permiso de alarmas exactas.

## Persistencia y privacidad

Toda la información funcional se guarda localmente en Room, en la base
`app_database`:

```text
cycles(start_date PRIMARY KEY, end_date)
notes(id PRIMARY KEY, date, content)
contraceptive_regimens(id PRIMARY KEY, start_date, end_date,
                       dose_hour, dose_minute, active_days, placebo_days)
pill_intakes(regimen_id, scheduled_date PRIMARY KEY, pill_number, status,
             taken_at, source)
```

`cycles.end_date = 0` representa un ciclo incompleto en el almacenamiento y se
expone como `null` en el dominio. Las tomas tienen una clave compuesta por
tratamiento y fecha, y se eliminan automáticamente si se elimina su tratamiento.

La base está en la versión 3. La migración 1→2 añade solo las tablas de
tratamientos y tomas, conservando ciclos y notas de la versión anterior. La
migración 2→3 convierte las fechas heredadas a claves de calendario estables y
conserva los datos. Android puede incluir la base en su copia de seguridad; la
aplicación no ofrece exportación manual ni sincronización con servicios externos.

## Límites funcionales conocidos

- La predicción siempre usa 28 días y no aprende de la duración histórica.
- WorkManager y las alarmas aproximadas pueden retrasarse por el sistema o por
  restricciones de batería del fabricante.
- La aplicación no recomienda acciones clínicas ante una pastilla omitida.
