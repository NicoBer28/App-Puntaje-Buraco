"""Corta cada imagen de un dataset YOLO en recortes cuadrados superpuestos, con sus etiquetas.

La app detecta las fotos por recortes (TfliteDetectorFichas.detectarFoto): cada recorte ocupa un
tercio del lado menor de la foto y se escala a 640x640. Entrenando con recortes del mismo tamaño
relativo, el modelo ve las fichas a la misma escala que las va a ver en el teléfono.

Uso:
    python recortar_dataset.py <dataset>/train/images recortes/train [fraccion=0.33] [solape=0.25]

Escribe recortes/train/images/*.jpg (640x640) y recortes/train/labels/*.txt. Se conservan las
fichas que quedan visibles en al menos el 60 % (recortadas al borde); las demás se descartan.
"""
import sys
from pathlib import Path

import cv2

from comun import escribir_cajas, etiqueta_de, imagenes, leer, leer_cajas

LADO_MODELO = 640
VISIBLE_MINIMO = 0.6


def posiciones(largo, lado, paso):
    resultado, p = [], 0
    while p + lado < largo:
        resultado.append(p)
        p += paso
    resultado.append(largo - lado)
    return list(dict.fromkeys(resultado))


def main(carpeta_imagenes, salida, fraccion=0.33, solape=0.25):
    fraccion, solape = float(fraccion), float(solape)
    salida = Path(salida)
    (salida / "images").mkdir(parents=True, exist_ok=True)
    total = vacios = 0
    for imagen in imagenes(carpeta_imagenes):
        im = leer(imagen)
        alto, ancho = im.shape[:2]
        cajas = leer_cajas(etiqueta_de(imagen), ancho, alto)
        lado = min(max(int(min(ancho, alto) * fraccion), min(LADO_MODELO, ancho, alto)), ancho, alto)
        paso = max(1, int(lado * (1 - solape)))
        for y0 in posiciones(alto, lado, paso):
            for x0 in posiciones(ancho, lado, paso):
                dentro = []
                for c, x1, y1, x2, y2 in cajas:
                    cx1, cy1 = max(x1, x0), max(y1, y0)
                    cx2, cy2 = min(x2, x0 + lado), min(y2, y0 + lado)
                    if cx2 <= cx1 or cy2 <= cy1:
                        continue
                    visible = (cx2 - cx1) * (cy2 - cy1) / ((x2 - x1) * (y2 - y1))
                    if visible >= VISIBLE_MINIMO:
                        dentro.append((c, cx1 - x0, cy1 - y0, cx2 - x0, cy2 - y0))
                # Recortes vacíos: se guarda uno de cada cuatro (sirven como ejemplos negativos).
                if not dentro:
                    vacios += 1
                    if vacios % 4:
                        continue
                recorte = cv2.resize(im[y0:y0 + lado, x0:x0 + lado], (LADO_MODELO, LADO_MODELO), interpolation=cv2.INTER_AREA)
                nombre = f"{imagen.stem}_{x0}_{y0}"
                cv2.imwrite(str(salida / "images" / f"{nombre}.jpg"), recorte, [cv2.IMWRITE_JPEG_QUALITY, 92])
                escribir_cajas(salida / "labels" / f"{nombre}.txt", dentro, lado, lado)
                total += 1
    print(f"{total} recortes en {salida}")


if __name__ == "__main__":
    main(*sys.argv[1:5])
