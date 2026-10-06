"""Erzeugt die Hintergrundmusik der Trailer per Synthese.

Selbst erzeugt statt heruntergeladen: Die Musik gehört damit OneLiteFeather, es gibt keine
Lizenzbedingungen, keine Namensnennung und keine Content-ID-Treffer. Deterministisch (fester Seed),
ein erneuter Lauf erzeugt dieselben Samples.

Die Szenen jeder Variante kommen aus src/variants.json, damit Herzschlag, Anstieg und Schlag sitzen:
  erste Szene        Spieluhr-Motiv über Drone
  bis zum Treffer    Drone + Wind, Herzschlag wird schneller
  vor dem Treffer    Anstieg, dann Stille
  Treffer ("hit")    tiefer Schlag, Drone kehrt verstimmt zurück
  Endkarte           Spieluhr-Motiv, Ausklang

Aufruf: python3 scripts/generate_music.py <ausgabeordner>   (schreibt <variante>.wav je Variante)
"""

import json
import sys
import wave
from pathlib import Path

import numpy as np

RATE = 44100
ROOT = Path(__file__).resolve().parent.parent


def t_axis(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def one_pole_lowpass(signal, cutoff):
    # Vektorisiert über die geschlossene Form wäre unlesbar; Blockweise reicht für 30 s Audio.
    alpha = 1 - np.exp(-2 * np.pi * cutoff / RATE)
    out = np.empty_like(signal)
    acc = 0.0
    for i, x in enumerate(signal):
        acc += alpha * (x - acc)
        out[i] = acc
    return out


def reverb(signal, seconds=2.8, mix=0.35, rng=None):
    # Faltung mit exponentiell abklingendem Rauschen: ein großer, dunkler Raum.
    n = int(seconds * RATE)
    ir = rng.standard_normal(n) * np.exp(-np.arange(n) / (RATE * seconds / 6))
    ir = one_pole_lowpass(ir, 2500)
    ir /= np.sqrt(np.sum(ir**2))
    size = 1 << int(np.ceil(np.log2(len(signal) + n)))
    wet = np.fft.irfft(np.fft.rfft(signal, size) * np.fft.rfft(ir, size), size)[: len(signal)]
    return (1 - mix) * signal + mix * wet


def envelope(length, points):
    """Stückweise lineare Hüllkurve aus (sekunde, wert)-Paaren."""
    times = [p[0] for p in points]
    values = [p[1] for p in points]
    return np.interp(np.arange(length) / RATE, times, values)


def drone(length, detune=0.0):
    t = np.arange(length) / RATE
    # A1 mit kleiner Sekunde und Tritonus: beides klassische Dissonanzen, sehr leise beigemischt.
    voices = [(55.0, 1.0), (55.0 * 1.003, 0.8), (110.0, 0.35), (58.27, 0.18), (77.78, 0.12)]
    out = np.zeros(length)
    for freq, amp in voices:
        f = freq * (1 + detune)
        # Drei Obertöne geben Körper, langsames LFO lässt den Ton atmen.
        tone = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t) + 0.12 * np.sin(6 * np.pi * f * t)
        lfo = 0.75 + 0.25 * np.sin(2 * np.pi * (0.07 + freq / 10000) * t)
        out += amp * tone * lfo
    return out / 3


def wind(length, rng):
    noise = rng.standard_normal(length)
    low = one_pole_lowpass(noise, 400)
    t = np.arange(length) / RATE
    gust = 0.5 + 0.5 * np.sin(2 * np.pi * 0.11 * t) * np.sin(2 * np.pi * 0.043 * t + 1)
    return low * gust * 2.2


def bell(freq, seconds=3.0):
    t = t_axis(seconds)
    # Spieluhr: Grundton plus unharmonische Partialtöne, schnelles Abklingen der hohen Anteile.
    partials = [(1, 1.0, 1.4), (2.76, 0.4, 3.5), (5.4, 0.18, 6.0), (8.93, 0.08, 9.0)]
    out = sum(a * np.sin(2 * np.pi * freq * r * t) * np.exp(-t * d) for r, a, d in partials)
    attack = np.minimum(1, t / 0.004)
    return out * attack


def thump(seconds=0.35, freq=52):
    t = t_axis(seconds)
    pitch = freq * (1 + 1.5 * np.exp(-t * 30))
    phase = 2 * np.pi * np.cumsum(pitch) / RATE
    return np.sin(phase) * np.exp(-t * 14)


def boom(rng, seconds=3.5):
    t = t_axis(seconds)
    pitch = 38 * (1 + 2.5 * np.exp(-t * 8))
    sub = np.sin(2 * np.pi * np.cumsum(pitch) / RATE) * np.exp(-t * 1.6)
    crack = one_pole_lowpass(rng.standard_normal(len(t)), 1800) * np.exp(-t * 18) * 3
    return sub + crack


def place(track, clip, at_seconds, gain=1.0):
    start = int(at_seconds * RATE)
    end = min(len(track), start + len(clip))
    if start < len(track):
        track[start:end] += clip[: end - start] * gain


# Spieluhr-Motiv in a-Moll, die letzte Note einen Halbton zu tief: vertraut, dann falsch.
MOTIF = [(0.0, 880.0), (0.45, 1046.5), (0.9, 987.8), (1.35, 830.6), (2.1, 659.3)]


def music_box(track, at, gain, detune=1.0):
    for offset, freq in MOTIF:
        place(track, bell(freq * detune), at + offset, gain)


def compose(variant, rng):
    starts, acc = {}, 0.0
    for scene, seconds in variant["scenes"]:
        starts.setdefault(scene, acc)
        acc += seconds
    total = acc
    length = int(total * RATE)
    hit = starts[variant["hit"]]
    end = starts["end"]
    first_beat = variant["scenes"][0][1]

    track = np.zeros(length)

    # Drone und Wind über alles, kurz vor dem Schlag weggenommen (Stille ist der Schreck).
    bed = 0.55 * drone(length) + 0.12 * wind(length, rng)
    bed *= envelope(length, [(0, 0), (1.5, 0.7), (hit - 2.0, 1.0), (hit - 0.15, 0.25), (hit, 0.0), (hit + 0.6, 0.0), (hit + 2.0, 0.6), (total - 1.5, 0.5), (total, 0)])
    track += bed

    # Verstimmte zweite Drone ab dem Schlag: dieselbe Welt, aber kaputt.
    broken = 0.4 * drone(length, detune=-0.03) * envelope(length, [(0, 0), (hit + 0.5, 0), (hit + 2.5, 0.8), (total - 1.5, 0.6), (total, 0)])
    track += broken

    music_box(track, 0.2, 0.22)

    # Herzschlag nach dem Cold Open bis kurz vor dem Schlag, Tempo 58 -> 110 bpm.
    t = first_beat
    while t < hit - 0.6:
        progress = (t - first_beat) / max(0.1, hit - first_beat)
        bpm = 58 + 52 * progress
        place(track, thump(), t, 0.9)
        place(track, thump(freq=46), t + 0.22, 0.6)
        t += 60 / bpm

    # Anstieg: gefiltertes Rauschen und ein steigender Ton, die mitten im Höhepunkt abreißen.
    rise_len = 2.2
    rt = t_axis(rise_len)
    riser = one_pole_lowpass(rng.standard_normal(len(rt)), 1200) * 1.5
    riser += 0.4 * np.sin(2 * np.pi * np.cumsum(220 + 500 * (rt / rise_len) ** 2) / RATE)
    riser *= (rt / rise_len) ** 2
    place(track, riser, hit - rise_len, 0.5)

    place(track, boom(rng), hit, 1.0)

    # Endkarte: Motiv einen Halbton tiefer, langsamer Ausklang.
    music_box(track, end + 0.4, 0.2, detune=0.944)

    track = reverb(track, rng=rng)
    track = np.tanh(track * 1.2)
    fade = envelope(length, [(0, 0), (0.05, 1), (total - 0.8, 1), (total, 0)])
    track *= fade
    track /= np.max(np.abs(track)) / 0.89  # Spitze bei etwa -1 dBFS

    # Leichte Stereobreite über Verzögerung; mono-kompatibel.
    delay = int(0.011 * RATE)
    left = track
    right = np.concatenate([np.zeros(delay), track[:-delay]]) * 0.92 + track * 0.08
    return np.stack([left, right], axis=1)


def write_wav(path, stereo):
    data = (np.clip(stereo, -1, 1) * 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())


def main():
    out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else ROOT / "public/music")
    out_dir.mkdir(parents=True, exist_ok=True)
    variants = json.loads((ROOT / "src/variants.json").read_text())["variants"]
    for variant in variants:
        rng = np.random.default_rng(sum(map(ord, variant["id"])))
        stereo = compose(variant, rng)
        path = out_dir / f"{variant['id']}.wav"
        write_wav(path, stereo)
        print(f"{path} ({len(stereo) / RATE:.1f} s)")


if __name__ == "__main__":
    main()
