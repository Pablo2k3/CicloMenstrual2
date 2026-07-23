# Manual funcional

## Pantalla principal

La pantalla contiene un calendario, la lista de notas del día seleccionado y un
botón flotante para operar sobre ese día. El calendario permite navegar entre seis
meses anteriores y seis posteriores a la fecha actual.

Al tocar un día se selecciona y se muestran sus notas. El botón flotante abre las
acciones correspondientes:

- **Marcar inicio del ciclo**: crea un ciclo incompleto. No admite fechas posteriores
  al momento actual.
- **Cambiar inicio de ciclo**: reemplaza el inicio del ciclo incompleto activo.
- **Marcar fin del ciclo**: completa el ciclo activo. El fin no puede ser futuro,
  anterior al inicio ni contener el comienzo de otro ciclo.
- **Eliminar ciclo**: aparece cuando el día pertenece a un ciclo completo y elimina
  el ciclo entero previa selección de la acción.
- **Añadir nota**: guarda texto no vacío en el día.

Cada nota de la lista puede eliminarse después de confirmar la operación.

## Pastillas anticonceptivas

La configuración solicita la fecha de la pastilla 1. La aplicación calcula una
pauta continua de 28 días: 21 activas y 7 de placebo. Si la fecha ya pasó, las
activas anteriores se inicializan como tomadas y los placebos como automáticos.

El número y estado de la pastilla aparecen en la tarjeta inferior al seleccionar
un día. Para mantener el calendario legible, las pastillas tomadas, próximas y de
placebo no añaden símbolos a las celdas. El número del día se muestra en naranja
cuando corresponde a una fecha pasada con una pastilla activa omitida.

Las activas de hoy o anteriores pueden marcarse o desmarcarse; las futuras no
pueden adelantarse.

«Comenzar nuevo tratamiento» cierra el anterior el día previo y conserva su
historial.

## Avisos de pastilla

Una activa avisa a las 14:00 y cada 15 minutos hasta las 15:45. La acción
«Tomada» confirma desde la notificación. A las 16:00 se cancela el aviso y queda
como omitida. Los placebos producen un único aviso y se registran automáticamente.

Android 12 o posterior requiere acceso especial a «Alarmas y recordatorios». Sin
él se usa una alarma aproximada y se muestra una advertencia.

## Leyenda del calendario

| Apariencia | Significado |
|---|---|
| Rosa | Día perteneciente a un ciclo completo |
| Rosa claro | Día transcurrido de un ciclo incompleto todavía considerado activo |
| Morado | Inicio previsto del próximo ciclo |
| 📝 | El día contiene una o más notas |
| ⚠️ | La fecha prevista ya ha pasado |
| 📝 + ⚠️ | Fecha prevista pasada que también contiene notas |

Un ciclo incompleto se considera activo solamente si es el último registrado y han
transcurrido menos de siete días desde su inicio.

## Predicción y recordatorio

La predicción suma 28 días naturales a la fecha de inicio del último ciclo
registrado. Al completar o eliminar el último ciclo se cancelan los recordatorios
anteriores y, si la nueva predicción es posterior al día actual, se programa una
notificación para el día anterior a las 08:15.

En Android 13 o posterior la aplicación solicita permiso para mostrar
notificaciones. Si se deniega, el resto de funciones continúa disponible.

## Persistencia y privacidad

Ciclos y notas permanecen en una base de datos privada del dispositivo. Android
puede incluirla en sus mecanismos de copia de seguridad según la configuración del
sistema. La aplicación no implementa exportación manual, estadísticas, historial
separado ni ajustes; los recursos de plantilla que sugerían esas opciones no eran
funcionales y se han retirado.
