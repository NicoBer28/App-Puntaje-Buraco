# Reentrenar el detector de fichas

Scripts para mejorar el modelo YOLO (`app/src/main/assets/best_float32.tflite`). Se corren en
Google Colab (o cualquier máquina con GPU) junto con el dataset con el que se entrenó el modelo
actual. Ver [docs/DETECCION_FICHAS.md](../../docs/DETECCION_FICHAS.md) para el porqué de cada paso.

| Script | Qué hace | Necesita |
|---|---|---|
| `fichas_desde_dataset.py` | Recorta cada ficha etiquetada del dataset | opencv |
| `generar_sinteticas.py` | Arma fotos de filas largas de fichas pegadas, con etiquetas | opencv |
| `recortar_dataset.py` | Corta las fotos en recortes a la escala que usa la app | opencv |
| `evaluar.py` | Mide un modelo como lo usa la app, o preetiqueta fotos nuevas | ultralytics |

## 1. Conseguir fotos reales de juegos con muchas fichas

Es lo que más ayuda. El modelo actual vio sobre todo fotos de una a quince fichas grandes; lo que
falla son las filas largas. Sacar 50 a 100 fotos de juegos bajados reales (20 a 60 fichas, mesas y
luces distintas, desde la altura a la que se saca con el teléfono) y etiquetarlas.

Etiquetar 50 fichas por foto a mano es lento: mejor dejar que el modelo actual haga un primer
borrador y solo corregirlo.

```bash
pip install ultralytics
python evaluar.py best.pt fotos_nuevas/images --preetiquetar   # escribe fotos_nuevas/labels/
```

Después se suben imágenes y etiquetas a Roboflow (o CVAT) y se corrige lo que esté mal.

## 2. Generar fotos sintéticas

```bash
python fichas_desde_dataset.py dataset/train/images fichas/
# fondos/: 20 o más fotos de mesas y manteles SIN fichas
python generar_sinteticas.py fichas/ fondos/ sinteticas/ 1500
```

Revisar unas cuantas a ojo antes de entrenar.

## 3. Recortar todo a la escala de la app

Con muchas fichas, la app analiza la foto en recortes de un tercio del lado menor, escalados a
640x640: ahí es donde las fichas le llegan más chicas al modelo. Entrenar con recortes del mismo
tamaño hace que el modelo las vea igual que en el teléfono.

```bash
python recortar_dataset.py dataset/train/images     entrenamiento/train
python recortar_dataset.py fotos_nuevas/images      entrenamiento/train
python recortar_dataset.py sinteticas/images        entrenamiento/train
python recortar_dataset.py dataset/valid/images     entrenamiento/valid
# También las fotos enteras, para que siga reconociendo fichas grandes:
cp dataset/train/images/* entrenamiento/train/images/
cp dataset/train/labels/* entrenamiento/train/labels/
```

`entrenamiento/data.yaml`:

```yaml
path: /content/entrenamiento
train: train/images
val: valid/images
names: ["1", "10", "11", "12", "13", "2", "3", "4", "5", "6", "7", "8", "9", "C"]
```

El orden de `names` tiene que ser el mismo que el de `app/src/main/assets/labels.txt`.

## 4. Entrenar

Partiendo del modelo actual (`best.pt`), que ya sabe bastante:

```bash
yolo detect train model=best.pt data=entrenamiento/data.yaml imgsz=640 epochs=120 patience=30 \
     fliplr=0.0 flipud=0.0 degrees=10 mosaic=1.0 close_mosaic=15 batch=16
```

- `fliplr=0.0`: por defecto YOLO espeja la mitad de las imágenes, y un número espejado no existe
  en una ficha real (un 2 espejado se parece a un 5).
- `flipud=0.0`: por lo mismo; además, una ficha dada vuelta arriba-abajo no existe (girada 180° sí,
  y esas ya las genera `generar_sinteticas.py`).

## 5. Medir y elegir el umbral

```bash
python evaluar.py runs/detect/train/weights/best.pt dataset/valid/images
```

Comparar con el modelo actual (`python evaluar.py best.pt dataset/valid/images`). Conviene que la
validación tenga fotos con muchas fichas: si son todas de una ficha, el número no dice nada.
El umbral con menos error de puntos va en `UMBRAL_CONFIANZA_FOTO` de `TfliteDetectorFichas`.

## 6. Exportar a la app

```bash
yolo export model=runs/detect/train/weights/best.pt format=tflite imgsz=640
```

Copiar `best_saved_model/best_float32.tflite` a `app/src/main/assets/`. La versión
`best_float16.tflite` pesa la mitad (22 MB en lugar de 44) con la misma precisión; para usarla
hay que cambiar `ARCHIVO_MODELO` en `TfliteDetectorFichas`.
