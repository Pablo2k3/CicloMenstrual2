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
