# Reglas de negocio

## Ciclos

- Un ciclo se identifica persistentemente por el timestamp de su fecha de inicio.
- La fecha inicial y final se almacenan como milisegundos Unix.
- El valor `0` en `cycles.end_date` representa un ciclo sin finalizar.
- Los ciclos se presentan ordenados por inicio ascendente.
- Solo puede editarse como activo el último ciclo cargado si no tiene fin.
- El rango completo incluye tanto el día inicial como el final.
- No se permite registrar fechas futuras, un fin anterior al inicio ni un fin que
  rebase el comienzo del ciclo siguiente.

## Fechas y notas

Las fechas de notas y marcadores se normalizan a las 00:00:00.000 de la zona
horaria local. Los desplazamientos usan días de calendario para respetar cambios
de mes, año y horario de verano.

Una nota contiene identificador, fecha y texto. La eliminación histórica se define
por fecha y contenido; por compatibilidad, notas duplicadas con ambos valores
iguales se eliminan conjuntamente.

## Parámetros fijos

- Longitud usada para predicción: 28 días.
- Ventana visual del calendario: ±6 meses.
- Duración visual máxima de ciclo incompleto: 7 días.
- Recordatorio: día anterior a las 08:15.

## Pauta anticonceptiva

- Pauta fija de 21 activas y 7 placebos; después de la 28 continúa la 1.
- Dosis diaria a las 14:00 en la zona horaria local.
- Recordatorios activos cada 15 minutos en `[14:00, 16:00)`.
- A las 16:00 una activa no confirmada queda omitida.
- Los placebos se registran automáticamente a las 14:00.
- Solo hoy y fechas pasadas pueden corregirse; los placebos no requieren acción.
- El calendario solo resalta en naranja las activas omitidas de días anteriores;
  el número y estado detallados se consultan en la tarjeta del día seleccionado.
- Un nuevo tratamiento cierra el anterior el día previo y conserva sus tomas.
- El registro no recomienda qué hacer clínicamente ante una dosis omitida.
