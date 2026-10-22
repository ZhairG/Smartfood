# Fuentes de los sonidos

Sonidos empaquetados en la app (`app/src/main/res/raw`). Todos son de licencia libre.

| Archivo | Uso | Original | Autor | Licencia | Fuente |
|---|---|---|---|---|---|
| `timer_alarm.ogg` | Temporizador de la cocina guiada al llegar a 0 (spec 009, CA-RIA-04-04) | "Alarm clock - 01.ogg" (*An alarm clock ringing. Audio file taken in France.*) | Mathieu Kappler | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | [https://commons.wikimedia.org/wiki/File:Alarm_clock_-_01.ogg](https://commons.wikimedia.org/wiki/File:Alarm_clock_-_01.ogg) |

**Cambios sobre el original** (`tools/timer_sound.py`): se recortó un ciclo de 1 s (de 3,30 s a 4,30 s, cuatro bips), se
normalizó el volumen y se silenció el ruido de fondo entre los bips. La app lo repite hasta que la persona detiene la
alarma. Como la licencia es CC BY-SA 4.0, el recorte se comparte con la misma licencia.
