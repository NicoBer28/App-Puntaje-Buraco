"""Mide un modelo YOLO como lo usa la app: una pasada (vista previa) y por recortes (foto).

Necesita `pip install ultralytics`. Sirve con el .pt o con el .tflite exportado.

Uso:
    python evaluar.py best.pt dataset/valid/images              # compara los dos modos
    python evaluar.py best.pt fotos_nuevas/ --preetiquetar      # escribe labels/ con lo detectado

Con etiquetas (labels/*.txt al lado de images/) informa, por modo y umbral de confianza: fichas
encontradas, faltantes, sobrantes, fotos contadas perfecto y error medio de puntos. Para elegir el
umbral de la app (UMBRAL_CONFIANZA_FOTO en TfliteDetectorFichas) conviene el que dé menos error
de puntos.

--preetiquetar guarda lo que detecta en formato YOLO para corregirlo en Roboflow (o CVAT) en lugar
de etiquetar 50 fichas por foto desde cero.
"""
import argparse
from collections import Counter

import cv2
import numpy as np
from ultralytics import YOLO

from comun import NOMBRES, escribir_cajas, etiqueta_de, imagenes, leer, leer_cajas

# Mismos valores que TfliteDetectorFichas.
ANCHO_FICHA_OBJETIVO = 96
UMBRAL_CONFIANZA_FOTO = 0.5
FRACCION_RECORTE_MINIMA = 0.33
SOLAPE = 0.25
MARGEN_BORDE = 0.01
IOU = 0.5
CONTENCION = 0.75
LADO_MAXIMO_FOTO = 3072
PUNTOS = {"1": 15, "2": 20, "C": 50, **{str(n): 5 for n in range(3, 8)}, **{str(n): 10 for n in range(8, 14)}}


def inferir(modelo, imagen_cuadrada, conf):
    """El modelo se entrenó con imágenes estiradas a 640x640: se estira antes de pasarla."""
    entrada = cv2.resize(imagen_cuadrada, (640, 640), interpolation=cv2.INTER_AREA)
    r = modelo.predict(entrada, imgsz=640, conf=conf, iou=0.7, agnostic_nms=True, verbose=False)[0]
    nombres = r.names
    return [(nombres[int(c)], float(s), b.tolist())
            for b, s, c in zip(r.boxes.xyxyn.cpu().numpy(), r.boxes.conf.cpu().numpy(), r.boxes.cls.cpu().numpy())]


def posiciones(largo, lado, solape):
    """Igual que Recortes.cubrir: recortes repartidos de forma pareja."""
    if largo <= lado:
        return [0]
    cantidad = int(np.ceil((largo - lado) / (lado * (1 - solape)))) + 1
    return [round(i * (largo - lado) / (cantidad - 1)) for i in range(cantidad)]


def lado_para_fichas(enteras, ancho, alto):
    """Igual que Recortes.ladoParaFichas."""
    menor = min(ancho, alto)
    minimo = min(max(int(menor * FRACCION_RECORTE_MINIMA), 640), menor)
    anchos = sorted(min((b[2] - b[0]) * ancho, (b[3] - b[1]) * alto) for _, s, b in enteras if s >= UMBRAL_CONFIANZA_FOTO)
    if not anchos:
        return minimo
    return int(min(max(anchos[len(anchos) // 2] * 640 / ANCHO_FICHA_OBJETIVO, minimo), menor))


def area(b):
    return max(0, b[2] - b[0]) * max(0, b[3] - b[1])


def interseccion(a, b):
    return max(0, min(a[2], b[2]) - max(a[0], b[0])) * max(0, min(a[3], b[3]) - max(a[1], b[1]))


def fusionar(fichas):
    elegidas = []
    for f in sorted(fichas, key=lambda x: -x[1]):
        repetida = False
        for e in elegidas:
            i = interseccion(e[2], f[2])
            union = area(e[2]) + area(f[2]) - i
            if (union > 0 and i / union >= IOU) or (area(f[2]) > 0 and i / area(f[2]) >= CONTENCION):
                repetida = True
                break
        if not repetida:
            elegidas.append(f)
    return elegidas


def detectar_foto(modelo, im, conf):
    alto, ancho = im.shape[:2]
    escala = min(1.0, LADO_MAXIMO_FOTO / max(alto, ancho))
    if escala < 1:
        im = cv2.resize(im, (round(ancho * escala), round(alto * escala)), interpolation=cv2.INTER_AREA)
        alto, ancho = im.shape[:2]
    enteras = inferir(modelo, im, conf)
    lado = lado_para_fichas(enteras, ancho, alto)
    candidatas = list(enteras)
    for y0 in posiciones(alto, lado, SOLAPE):
        for x0 in posiciones(ancho, lado, SOLAPE):
            for nombre, s, b in inferir(modelo, im[y0:y0 + lado, x0:x0 + lado], conf):
                m = MARGEN_BORDE
                if (x0 > 0 and b[0] < m) or (y0 > 0 and b[1] < m) or (x0 + lado < ancho and b[2] > 1 - m) or (y0 + lado < alto and b[3] > 1 - m):
                    continue
                candidatas.append((nombre, s, [(x0 + b[0] * lado) / ancho, (y0 + b[1] * lado) / alto,
                                               (x0 + b[2] * lado) / ancho, (y0 + b[3] * lado) / alto]))
    return fusionar(candidatas)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("modelo")
    p.add_argument("imagenes")
    p.add_argument("--umbrales", default="0.3,0.4,0.5,0.6")
    p.add_argument("--preetiquetar", action="store_true")
    a = p.parse_args()
    modelo = YOLO(a.modelo)
    umbrales = [float(u) for u in a.umbrales.split(",")]

    resultados = {"una pasada": [], "por recortes": []}
    for imagen in imagenes(a.imagenes):
        im = leer(imagen)
        alto, ancho = im.shape[:2]
        recortes = detectar_foto(modelo, im, min(umbrales))
        if a.preetiquetar:
            cajas = [(NOMBRES.index(n), b[0] * ancho, b[1] * alto, b[2] * ancho, b[3] * alto)
                     for n, s, b in recortes if s >= 0.5]
            escribir_cajas(etiqueta_de(imagen), cajas, ancho, alto)
            continue
        verdad = [NOMBRES[c] for c, *_ in leer_cajas(etiqueta_de(imagen), ancho, alto)]
        resultados["una pasada"].append((verdad, inferir(modelo, im, min(umbrales))))
        resultados["por recortes"].append((verdad, recortes))
        print(imagen.name, len(verdad), "fichas", flush=True)

    if a.preetiquetar:
        print("Etiquetas escritas en labels/")
        return
    for modo, filas in resultados.items():
        for u in umbrales:
            tp = fp = fn = exactas = 0
            error_puntos = []
            for verdad, detectadas in filas:
                d = Counter(n for n, s, _ in detectadas if s >= u)
                v = Counter(verdad)
                ok = sum((d & v).values())
                tp, fp, fn = tp + ok, fp + sum(d.values()) - ok, fn + sum(v.values()) - ok
                exactas += d == v
                error_puntos.append(abs(sum(PUNTOS[n] * k for n, k in d.items()) - sum(PUNTOS[n] * k for n, k in v.items())))
            print(f"{modo:13s} umbral {u:.2f}: encontradas {tp}/{tp + fn}, sobran {fp}, "
                  f"fotos exactas {exactas}/{len(filas)}, error medio {np.mean(error_puntos):.1f} puntos")


if __name__ == "__main__":
    main()
