# Detección de fichas

## Archivos

| Archivo | Dónde | En el repo |
|---|---|---|
| Modelo YOLOv8 exportado a LiteRT | `app/src/main/assets/best_float32.tflite` | No (44 MB) |
| Nombres de las clases, en el orden del modelo | `app/src/main/assets/labels.txt` | Sí |

El orden de `labels.txt` sale de los metadatos del modelo (Ultralytics los guarda dentro del
`.tflite`): `1, 10, 11, 12, 13, 2, 3, …, 9, C`. Si se reentrena con otro `data.yaml`, hay que
regenerarlo. `TfliteDetectorFichas.preparar()` falla con un mensaje claro si la cantidad de
etiquetas no coincide con la del modelo.

`app/build.gradle.kts` declara `noCompress += "tflite"`: el modelo se mapea en memoria desde el
APK y eso no funciona si está comprimido.

## Dos modos

**Vista previa (en vivo).** Cada cuadro de la cámara (640x480) se estira a 640x640 y pasa una vez
por el modelo. Sirve para apuntar y para juegos de pocas fichas.

**Foto (botón «Sacar foto y contar»).** Se saca una foto a resolución completa (reducida a 3072 px
de lado mayor) y se analiza por recortes, al estilo
[SAHI](https://github.com/obss/sahi):

1. Una pasada con la foto entera, que mide el tamaño de las fichas.
2. Recortes cuadrados superpuestos (25 %), del tamaño justo para que cada ficha llegue al modelo
   con unos 96 px de ancho, y como mínimo de un tercio del lado menor (así son 25 recortes como
   mucho). Si en la pasada entera no se vio ninguna ficha, se usa ese mínimo.
3. Se descartan las cajas cortadas por el borde de un recorte (otro recorte las ve enteras) y se
   juntan todas: entre cajas que se pisan queda la de mayor confianza, y se descartan las que
   quedan casi enteras dentro de otra.

Después el resultado se puede corregir a mano: tocar una caja para cambiar su valor o quitarla, y
tocar un lugar vacío para agregar una ficha que faltó. La lógica pura (geometría y fusión) está en
`Recortes.kt` y tiene tests en la JVM.

### Por qué

El modelo trabaja a 640x640. En una foto de una fila de 50 fichas, al reducirla entera cada ficha
queda de 10 a 12 píxeles de ancho y el modelo prácticamente no la ve. Con recortes, la misma ficha
le llega varias veces más grande.

Tampoco conviene que lleguen demasiado grandes: con recortes chicos sobre fichas grandes el modelo
empieza a ver los dos «1» de un 11 como dos fichas de 1. Por eso el tamaño del recorte se calcula
a partir de las fichas medidas en la primera pasada.

## Mediciones

Hechas fuera del teléfono, con el mismo modelo y la misma lógica que la app (reimplementada en
Python), sobre dos conjuntos con las fichas contadas a mano:

- **Fotos reales**: las 36 de `fotos_fichasburako` (132 fichas; la mayoría con una sola ficha
  grande, siete con 3 a 32 fichas). Probablemente parte de ellas se usó para entrenar.
- **Sintéticas**: 18 fotos de 4032x3024 armadas con fichas recortadas de esas fotos, en filas
  pegadas: una fila de 50, dos de 30, tres de 20, etc. (833 fichas).

| | Reales: encontradas | Reales: sobrantes | Reales: fotos exactas | Sintéticas: encontradas | Sintéticas: sobrantes | Sintéticas: fotos exactas |
|---|---|---|---|---|---|---|
| Antes (cuadro de 640x480, una pasada) | 128 / 132 | 6 | 32 / 36 | 356 / 833 (43 %) | 170 | 0 / 18 |
| Foto por recortes (umbral 0,5) | 131 / 132 | 2 | 33 / 36 | 825 / 833 (99 %) | 6 | 11 / 18 |

En la fila de 50 fichas, la forma anterior encontraba 0 y la nueva 49. Los errores que quedan son
casi todos de lectura del número (un 5 leído como 3, un 13 como 10), no fichas perdidas: eso se
corrige reentrenando (ver abajo) y, mientras tanto, con la corrección manual.

Una «foto exacta» es una foto en la que se encontraron justo las fichas que había, ni una más ni
una menos. Con 50 fichas, aun leyendo bien el 99 % de las fichas, alrededor de 4 de cada 10 fotos
van a tener algún error (0,99⁵⁰ ≈ 0,6): por eso la corrección manual no es un parche sino parte
del flujo.

### Tiempo

Con fichas grandes son 3 o 4 pasadas del modelo; con una fila de 50, hasta 25. YOLOv8s en CPU
tarda aproximadamente entre 100 y 400 ms por pasada según el teléfono (estimado: falta medirlo en un teléfono real), así que el peor caso ronda 3 a 10 s. Si
resulta lento, en orden de esfuerzo:

1. Exportar el modelo en float16 (`best_float16.tflite`, misma precisión, la mitad de tamaño).
2. Delegado de GPU de LiteRT (suele ser 3 a 5 veces más rápido; hay que ver si tiene el mismo
   problema de namespaces con AGP 9 que las versiones nuevas de LiteRT).
3. Bajar `ANCHO_FICHA_OBJETIVO` o subir `FRACCION_RECORTE_MINIMA` (menos recortes, algo menos de
   precisión en filas muy largas).

## Reentrenar

Los scripts y el paso a paso están en [`scripts/deteccion/`](../scripts/deteccion/README.md). En
resumen:

1. **Fotos reales de juegos con muchas fichas** (lo que más rinde). `evaluar.py --preetiquetar`
   hace un primer borrador de las etiquetas con el modelo actual, para corregir en lugar de
   dibujar 50 cajas por foto.
2. **Fotos sintéticas** de filas largas (`generar_sinteticas.py`).
3. **Recortar** todo a la escala que usa la app (`recortar_dataset.py`).
4. **Entrenar sin espejar** (`fliplr=0`): un número espejado no existe y confunde 2 con 5.
5. **Medir** con `evaluar.py`, que reproduce la lógica de la app, y elegir el umbral.
