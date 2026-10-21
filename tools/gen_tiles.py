#!/usr/bin/env python3
"""Genera lo repetitivo de las 12 tarjetas: clases DecoTileN, servicios del manifest y etiquetas.

Android exige un servicio distinto por cada tarjeta del Centro de Control, así que en vez de
mantener a mano 12 copias, este script las regenera. Uso (desde la raíz del proyecto):

    python3 tools/gen_tiles.py            # 12 tarjetas
    python3 tools/gen_tiles.py 16         # otro número

Si cambias el número también hay que actualizar DecoLogic.SLOTS y defaultGlyphs en DecoStore.
"""
import pathlib
import re
import sys

N = int(sys.argv[1]) if len(sys.argv) > 1 else 12
ROOT = pathlib.Path(__file__).resolve().parent.parent
APP = ROOT / "app/src/main"
PKG_DIR = APP / "java/com/jonnyuun/decocenter"
BEGIN = "<!-- GENERATED:TILES:BEGIN (tools/gen_tiles.py) -->"
END = "<!-- GENERATED:TILES:END -->"

# 1) Clases Kotlin
kt = [
    "// Generado por tools/gen_tiles.py. No editar a mano.",
    "package com.jonnyuun.decocenter",
    "",
]
for i in range(1, N + 1):
    kt.append(f"class DecoTile{i} : DecoTileService() {{ override val slot = {i - 1} }}")
kt += ["", "val TILE_CLASSES: List<Class<out DecoTileService>> = listOf("]
kt += [f"    DecoTile{i}::class.java," for i in range(1, N + 1)]
kt += [")", ""]
(PKG_DIR / "GeneratedTiles.kt").write_text("\n".join(kt), encoding="utf-8")

# 2) Etiquetas de los servicios (el editor del Centro de Control las muestra)
xml = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Generado por tools/gen_tiles.py. No editar a mano. -->", "<resources>"]
for i in range(1, N + 1):
    xml.append(f'    <string name="tile_service_label_{i}">Deco {i}</string>')
xml += ["</resources>", ""]
(APP / "res/values/tile_labels.xml").write_text("\n".join(xml), encoding="utf-8")

# 3) Servicios en el manifest, entre marcadores
block = [f"        {BEGIN}"]
for i in range(1, N + 1):
    block += [
        "        <service",
        f'            android:name=".DecoTile{i}"',
        '            android:exported="true"',
        '            android:icon="@drawable/ic_tile_default"',
        f'            android:label="@string/tile_service_label_{i}"',
        '            android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">',
        "            <intent-filter>",
        '                <action android:name="android.service.quicksettings.action.QS_TILE" />',
        "            </intent-filter>",
        "        </service>",
        "",
    ]
block[-1] = f"        {END}"
manifest = APP / "AndroidManifest.xml"
text = manifest.read_text(encoding="utf-8")
pattern = re.compile(r"[ \t]*" + re.escape(BEGIN) + r".*?" + re.escape(END), re.S)
if not pattern.search(text):
    sys.exit("No encuentro los marcadores GENERATED:TILES en AndroidManifest.xml")
manifest.write_text(pattern.sub("\n".join(block), text), encoding="utf-8")
print(f"Generadas {N} tarjetas.")
