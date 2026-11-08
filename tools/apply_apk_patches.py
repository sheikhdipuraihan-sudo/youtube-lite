#!/usr/bin/env python3
"""Apply only verified, player-specific patches to an apktool tree."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1] / "build" / "decoded"
player = root / "smali" / "com" / "vanced" / "player" / "watch" / "PlayerDirector.smali"
if not player.exists():
    raise SystemExit(f"Missing expected player file: {player}")

text = player.read_text(encoding="utf-8")
pattern = re.compile(
    r"(?ms)^\.method public final l\(\)Z\n.*?^\.end method$"
)
replacement = ".method public final l()Z\n    .locals 1\n\n    # Player watermark disabled by the owner-requested branding patch.\n    const/4 v0, 0x0\n    return v0\n.end method"
text2, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise SystemExit("PlayerDirector.l() watermark method was not patched exactly once")
player.write_text(text2, encoding="utf-8")

# Remove only the original HeroMods signature marker. Do not delete generic
# HEROMODS resource aliases: apktool uses some as valid resource names.
for name in ("HERO.RSA", "HERO.SF"):
    p = root / "original" / "META-INF" / name
    if p.exists():
        p.unlink()

print("Applied: PlayerDirector.l() now returns false; removed HERO.RSA/HERO.SF")
