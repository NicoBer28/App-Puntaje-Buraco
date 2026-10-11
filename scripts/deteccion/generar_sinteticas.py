"""Genera fotos sintéticas de juegos bajados: filas largas de fichas pegadas, con sus etiquetas YOLO.

El modelo actual se entrenó sobre todo con pocas fichas grandes por foto. Estas imágenes le muestran
lo que más le cuesta: 20 a 60 fichas chicas, pegadas, en filas y columnas.

Uso:
    python generar_sinteticas.py fichas/ fondos/ sinteticas/ 1500 [semilla]

- fichas/: salida de fichas_desde_dataset.py
- fondos/: fotos de mesas, manteles, etc. SIN fichas (cuanto más variadas, mejor)
Escribe sinteticas/images/*.jpg y sinteticas/labels/*.txt.
"""
import json
import random
import sys
from pathlib import Path

import cv2
import numpy as np

from comun import NOMBRES, escribir_cajas, imagenes

# (filas, fichas por fila, foto apaisada): de una fila larga a varias cortas.
CONFIGURACIONES = [(1, 50, True), (2, 30, True), (3, 20, True), (4, 15, False), (1, 35, True),
                   (6, 10, False), (5, 12, True), (8, 7, False)]


def cargar_fichas(carpeta):
    carpeta = Path(carpeta)
    fichas = []
    for archivo, etiqueta in json.loads((carpeta / "meta.json").read_text()):
        im = cv2.imread(str(carpeta / archivo))
        if im is not None:
            fichas.append((im, etiqueta))
    return fichas


def fondo(fondos, ancho, alto, rng):
    bg = cv2.imread(str(rng.choice(fondos)))
    s = max(ancho / bg.shape[1], alto / bg.shape[0]) * rng.uniform(1.0, 1.4)
    bg = cv2.resize(bg, None, fx=s, fy=s)
    y = rng.randint(0, bg.shape[0] - alto)
    x = rng.randint(0, bg.shape[1] - ancho)
    return bg[y:y + alto, x:x + ancho].copy()


def generar(fichas, fondos, rng, filas, por_fila, apaisada):
    ancho, alto = (4032, 3024) if apaisada else (3024, 4032)
    img = fondo(fondos, ancho, alto, rng)
    tw = int(ancho * rng.uniform(0.75, 0.94) / por_fila)
    th = int(tw / rng.uniform(0.68, 0.76))
    if filas * th * 1.25 > alto * 0.95:
        th = int(alto * 0.95 / (filas * 1.25))
        tw = int(th * 0.72)
    y = max(5, int((alto - filas * th * 1.25) / 2 + rng.uniform(-0.05, 0.05) * alto))
    cajas = []
    for f in range(filas):
        n = por_fila if f == 0 else rng.randint(max(3, por_fila // 2), por_fila)
        x = max(5, int((ancho - n * tw) / 2 + rng.uniform(-0.03, 0.03) * ancho))
        for i in range(n):
            ficha, etiqueta = rng.choice(fichas)
            # A veces un hueco más grande: separa un juego del siguiente en la misma fila.
            if i and rng.random() < 0.08:
                x += int(tw * rng.uniform(0.3, 1.0))
            jy = rng.randint(-3, 3)
            if x + tw < ancho and 0 <= y + jy and y + jy + th < alto:
                img[y + jy:y + jy + th, x:x + tw] = cv2.resize(ficha, (tw, th), interpolation=cv2.INTER_AREA)
                cajas.append((NOMBRES.index(etiqueta), x, y + jy, x + tw, y + jy + th))
            x += tw + rng.randint(0, 3)
        y += int(th * rng.uniform(1.1, 1.4))

    # Perspectiva suave: la foto casi nunca se saca desde arriba exacto.
    d = 0.04
    src = np.float32([[0, 0], [ancho, 0], [ancho, alto], [0, alto]])
    dst = np.float32([[rng.uniform(0, d) * ancho, rng.uniform(0, d) * alto],
                      [ancho - rng.uniform(0, d) * ancho, rng.uniform(0, d) * alto],
                      [ancho - rng.uniform(0, d) * ancho, alto - rng.uniform(0, d) * alto],
                      [rng.uniform(0, d) * ancho, alto - rng.uniform(0, d) * alto]])
    m = cv2.getPerspectiveTransform(src, dst)
    img = cv2.warpPerspective(img, m, (ancho, alto), borderMode=cv2.BORDER_REFLECT)
    nuevas = []
    for c, x1, y1, x2, y2 in cajas:
        p = cv2.perspectiveTransform(np.float32([[[x1, y1], [x2, y1], [x2, y2], [x1, y2]]]), m)[0]
        nuevas.append((c, p[:, 0].min(), p[:, 1].min(), p[:, 0].max(), p[:, 1].max()))

    # Girar la foto entera conserva las etiquetas (la ficha sigue siendo la misma).
    giro = rng.choice([None, None, cv2.ROTATE_90_CLOCKWISE, cv2.ROTATE_180, cv2.ROTATE_90_COUNTERCLOCKWISE])
    if giro is not None:
        img, nuevas = girar(img, nuevas, giro)

    img = cv2.convertScaleAbs(img, alpha=rng.uniform(0.8, 1.15), beta=rng.uniform(-20, 20))
    if rng.random() < 0.5:
        img = cv2.GaussianBlur(img, (0, 0), rng.uniform(0.5, 1.5))
    _, jpg = cv2.imencode(".jpg", img, [cv2.IMWRITE_JPEG_QUALITY, rng.randint(80, 95)])
    return cv2.imdecode(jpg, cv2.IMREAD_COLOR), nuevas


def girar(img, cajas, giro):
    alto, ancho = img.shape[:2]
    girada = cv2.rotate(img, giro)
    resultado = []
    for c, x1, y1, x2, y2 in cajas:
        if giro == cv2.ROTATE_180:
            resultado.append((c, ancho - x2, alto - y2, ancho - x1, alto - y1))
        elif giro == cv2.ROTATE_90_CLOCKWISE:
            resultado.append((c, alto - y2, x1, alto - y1, x2))
        else:  # 90 antihorario
            resultado.append((c, y1, ancho - x2, y2, ancho - x1))
    return girada, resultado


def main(carpeta_fichas, carpeta_fondos, salida, cantidad, semilla=0):
    rng = random.Random(int(semilla))
    fichas = cargar_fichas(carpeta_fichas)
    fondos = imagenes(carpeta_fondos)
    if not fichas or not fondos:
        sys.exit("Faltan fichas o fondos")
    salida = Path(salida)
    (salida / "images").mkdir(parents=True, exist_ok=True)
    for k in range(int(cantidad)):
        filas, por_fila, apaisada = CONFIGURACIONES[k % len(CONFIGURACIONES)]
        img, cajas = generar(fichas, fondos, rng, filas, por_fila, apaisada)
        nombre = f"sint_{k:05d}"
        cv2.imwrite(str(salida / "images" / f"{nombre}.jpg"), img)
        escribir_cajas(salida / "labels" / f"{nombre}.txt", cajas, img.shape[1], img.shape[0])
        if k % 50 == 0:
            print(f"{k}/{cantidad}", flush=True)


if __name__ == "__main__":
    main(*sys.argv[1:6])
