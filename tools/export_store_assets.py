#!/usr/bin/env python3
"""Copy the Paparazzi-rendered store assets to /workspace/mmele-store (or argv[1]) with Play-ready
formats: icon 512x512 32-bit PNG; feature graphic and screenshots 24-bit PNG (no alpha).
Render first:  ./gradlew :app:recordPaparazziDebug"""
import sys, os
from PIL import Image

src = os.path.join(os.path.dirname(__file__), "..", "app", "src", "test", "snapshots", "images")
dst = sys.argv[1] if len(sys.argv) > 1 else "/workspace/mmele-store"
os.makedirs(dst, exist_ok=True)
jobs = {
    "bw.mmele_StoreIconTest_icon_icon_512.png": ("icon-512.png", (512, 512), "RGBA"),
    "bw.mmele_StoreFeatureGraphicTest_featureGraphic_feature_graphic.png": ("feature-graphic-1024x500.png", (1024, 500), "RGB"),
    "bw.mmele_StoreScreenshotsTest_s1Home_phone_1_home.png": ("phone-1-home.png", (1080, 1920), "RGB"),
    "bw.mmele_StoreScreenshotsTest_s2Classic_phone_2_classic.png": ("phone-2-classic-board.png", (1080, 1920), "RGB"),
    "bw.mmele_StoreScreenshotsTest_s3Khutlotharo_phone_3_khutlotharo.png": ("phone-3-khutlotharo-triangle.png", (1080, 1920), "RGB"),
    "bw.mmele_StoreScreenshotsTest_s4PieceSets_phone_4_bird_eggs.png": ("phone-4-piece-sets-bird-eggs.png", (1080, 1920), "RGB"),
    "bw.mmele_StoreScreenshotsTest_s5Scene_phone_5_scene_tsodilo.png": ("phone-5-scene-tsodilo.png", (1080, 1920), "RGB"),
    "bw.mmele_StoreScreenshotsTest_s6OnlineRoomCode_phone_6_online_room_code.png": ("phone-6-online-room-code.png", (1080, 1920), "RGB"),
}
for name, (out, size, mode) in jobs.items():
    im = Image.open(os.path.join(src, name))
    assert im.size == size, f"{name}: {im.size} != {size}"
    if mode == "RGB":
        bg = Image.new("RGB", im.size, (0x12, 0x14, 0x17))
        bg.paste(im, mask=im.convert("RGBA").split()[3])
        im = bg
    path = os.path.join(dst, out)
    im.save(path, optimize=True)
    kb = os.path.getsize(path) // 1024
    assert kb < (1024 if out.startswith("icon") else 8192), f"{out} too large ({kb} KB)"
    print(f"{path}  {im.size[0]}x{im.size[1]} {im.mode}  {kb} KB")
