"""Utilidades compartidas por los scripts de detección (formato YOLO)."""
from pathlib import Path

import cv2

# Mismo orden que las clases del modelo actual (y que app/src/main/assets/labels.txt).
NOMBRES = ["1", "10", "11", "12", "13", "2", "3", "4", "5", "6", "7", "8", "9", "C"]
EXTENSIONES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def imagenes(carpeta):
    return sorted(p for p in Path(carpeta).rglob("*") if p.suffix.lower() in EXTENSIONES)


def etiqueta_de(imagen):
    """images/x.jpg -> labels/x.txt (estructura de Roboflow/Ultralytics)."""
    partes = list(imagen.parts)
    for i in range(len(partes) - 1, -1, -1):
        if partes[i] == "images":
            partes[i] = "labels"
            return Path(*partes).with_suffix(".txt")
    return imagen.with_suffix(".txt")


def leer_cajas(archivo, ancho, alto):
    """Devuelve [(clase, x1, y1, x2, y2)] en píxeles."""
    cajas = []
    if not Path(archivo).exists():
        return cajas
    for linea in Path(archivo).read_text().split("\n"):
        v = linea.split()
        if len(v) != 5:
            continue
        c, cx, cy, w, h = int(v[0]), *map(float, v[1:])
        cajas.append((c, (cx - w / 2) * ancho, (cy - h / 2) * alto, (cx + w / 2) * ancho, (cy + h / 2) * alto))
    return cajas


def escribir_cajas(archivo, cajas, ancho, alto):
    Path(archivo).parent.mkdir(parents=True, exist_ok=True)
    with open(archivo, "w") as f:
        for c, x1, y1, x2, y2 in cajas:
            f.write(f"{c} {(x1 + x2) / 2 / ancho:.6f} {(y1 + y2) / 2 / alto:.6f} {(x2 - x1) / ancho:.6f} {(y2 - y1) / alto:.6f}\n")


def leer(imagen):
    im = cv2.imread(str(imagen))
    if im is None:
        raise ValueError(f"No se pudo leer {imagen}")
    return im
