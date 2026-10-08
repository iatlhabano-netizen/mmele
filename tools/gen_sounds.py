#!/usr/bin/env python3
"""Procedurally synthesise Mmele's short UI sounds (no third-party samples, no licensing issues).

Writes 16-bit mono WAVs to a temp dir and encodes them to small OGG Vorbis files in
app/src/main/res/raw/ with ffmpeg. Deterministic (fixed RNG seed).
"""
import os, subprocess, tempfile, wave
import numpy as np

SR = 44100
rng = np.random.default_rng(1234)
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'raw')


def t(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, attack=0.002, decay=0.08):
    x = np.arange(n) / SR
    a = np.clip(x / attack, 0, 1)
    return a * np.exp(-x / decay)


def lowpass(sig, alpha):
    out = np.zeros_like(sig); acc = 0.0
    for i, v in enumerate(sig):
        acc += alpha * (v - acc); out[i] = acc
    return out


def bandnoise(n, alpha_lo, alpha_hi):
    w = rng.standard_normal(n)
    return lowpass(w, alpha_hi) - lowpass(w, alpha_lo)


def mix(dst, src, off):
    m = src[:max(0, len(dst) - off)]
    dst[off:off + len(m)] += m


def marimba(freq, dur, decay=0.35):
    x = t(dur)
    s = (np.sin(2 * np.pi * freq * x) + 0.35 * np.sin(2 * np.pi * freq * 4.0 * x) * np.exp(-x / 0.05)
         + 0.12 * np.sin(2 * np.pi * freq * 9.2 * x) * np.exp(-x / 0.02))
    return s * env(len(x), 0.003, decay)


def stone_click(dur=0.12, pitch=1.0, weight=1.0):
    n = int(SR * dur); x = t(dur)
    click = bandnoise(n, 0.05, 0.6) * env(n, 0.0005, 0.012 * weight)
    body = np.sin(2 * np.pi * 180 * pitch * x) * env(n, 0.001, 0.03 * weight) * 0.6
    ring = np.sin(2 * np.pi * 1450 * pitch * x) * env(n, 0.001, 0.02) * 0.18
    return click * 0.9 + body + ring


def norm(s, peak=0.85):
    s = s - np.mean(s)
    m = np.max(np.abs(s)) or 1
    return s / m * peak


def write(name, s, peak=0.85):
    s = norm(s, peak)
    fade = min(len(s), int(SR * 0.01))
    s[-fade:] *= np.linspace(1, 0, fade)
    pcm = (s * 32767).astype(np.int16)
    tmp = os.path.join(tempfile.gettempdir(), name + '.wav')
    with wave.open(tmp, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    out = os.path.join(OUT, name + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '4', out], check=True)
    print(f'{out}: {os.path.getsize(out)} bytes')


def main():
    os.makedirs(OUT, exist_ok=True)
    write('place', stone_click(0.14, 1.0, 1.0))
    write('select', stone_click(0.06, 1.6, 0.4), peak=0.5)
    # slide: short gritty scrape ending in a soft click
    n = int(SR * 0.22)
    scrape = bandnoise(n, 0.02, 0.25) * np.sin(np.linspace(0, np.pi, n)) ** 2 * 0.5
    end = np.zeros(n); c = stone_click(0.1, 0.9, 0.8); end[-len(c):] = c
    write('slide', scrape + end * 0.8)
    # fly: airy whoosh with a rising band
    n = int(SR * 0.35)
    wh = bandnoise(n, 0.01, 0.12) * np.sin(np.linspace(0, np.pi, n)) ** 1.5
    end = np.zeros(n); c = stone_click(0.1, 1.1, 0.8); end[-len(c):] = c
    write('fly', wh * 0.7 + end * 0.6)
    # mill: bright marimba triad (D major-ish pentatonic) with tiny stagger
    dur = 0.9; n = int(SR * dur); s = np.zeros(n)
    for i, f in enumerate([587.33, 739.99, 880.0, 1174.66]):
        off = int(SR * 0.035 * i); m = marimba(f, dur - 0.035 * i, 0.3)
        mix(s, m * (1.0 - 0.12 * i), off)
    write('mill', s)
    # shoot: deep thump + crack
    n = int(SR * 0.4); x = t(0.4)
    thump = np.sin(2 * np.pi * (90 * np.exp(-x / 0.12) + 45) * x) * env(n, 0.001, 0.12)
    crack = bandnoise(n, 0.1, 0.9) * env(n, 0.0003, 0.02)
    write('shoot', thump + crack * 0.7)
    # invalid: soft low double knock
    n = int(SR * 0.22); s = np.zeros(n)
    k = stone_click(0.08, 0.5, 0.6); s[:len(k)] += k; s[int(SR * 0.1):int(SR * 0.1) + len(k)] += k * 0.8
    write('invalid', s, peak=0.45)
    # win: rising marimba arpeggio
    notes = [440.0, 554.37, 659.25, 880.0, 1108.73]
    dur = 1.4; n = int(SR * dur); s = np.zeros(n)
    for i, f in enumerate(notes):
        off = int(SR * 0.11 * i); m = marimba(f, dur - 0.11 * i, 0.45)
        mix(s, m, off)
    write('win', s)
    # lose / draw: gentle descending pair
    dur = 1.0; n = int(SR * dur); s = np.zeros(n)
    for i, f in enumerate([523.25, 392.0, 329.63]):
        off = int(SR * 0.16 * i); m = marimba(f, dur - 0.16 * i, 0.4)
        mix(s, m, off)
    write('end', s, peak=0.7)


if __name__ == '__main__':
    main()
