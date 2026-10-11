"""Recorta cada ficha etiquetada de un dataset YOLO, para armar fotos sintéticas después.

Uso:
    python fichas_desde_dataset.py <dataset>/train/images fichas/

Escribe fichas/<n>.png y fichas/meta.json = [[archivo, etiqueta], ...]. Solo guarda fichas
verticales (más altas que anchas), que son las que se usan para armar filas.
"""
import json
import sys
from pathlib import Path

import cv2

from comun import NOMBRES, etiqueta_de, imagenes, leer, leer_cajas


def main(carpeta_imagenes, salida):
    salida = Path(salida)
    salida.mkdir(parents=True, exist_ok=True)
    meta = []
    for imagen in imagenes(carpeta_imagenes):
        im = leer(imagen)
        alto, ancho = im.shape[:2]
        for i, (c, x1, y1, x2, y2) in enumerate(leer_cajas(etiqueta_de(imagen), ancho, alto)):
            w, h = x2 - x1, y2 - y1
            if h < w * 1.1 or w < 24:  # horizontales o demasiado chicas
                continue
            recorte = im[max(int(y1), 0):int(y2), max(int(x1), 0):int(x2)]
            if recorte.size == 0:
                continue
            archivo = salida / f"{imagen.stem}_{i}.png"
            cv2.imwrite(str(archivo), recorte)
            meta.append([archivo.name, NOMBRES[c]])
    (salida / "meta.json").write_text(json.dumps(meta))
    print(f"{len(meta)} fichas en {salida}")


if __name__ == "__main__":
    main(*sys.argv[1:3])
