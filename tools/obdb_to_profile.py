#!/usr/bin/env python3
"""
Convert an OBDb vehicle file into a Torque-style PID CSV this app can import.

The OBD database (https://github.com/OBDb) is a community, CC BY-SA 4.0 collection of
manufacturer-specific parameters: request bytes, module address, and how to decode the
reply. It's the open equivalent of the tables that live inside a dealer tool, and it has
repos for Chevrolet (Cruze) and Ford (F-150, Explorer, Fusion, …).

OBDb ships JSON; this app imports the Torque CSV format. This bridges the two.

Usage:
  python3 tools/obdb_to_profile.py cruze.json -o cruze-pids.csv
  python3 tools/obdb_to_profile.py --from-github Chevrolet-Cruze -o cruze-pids.csv

Then in the app: Settings → PID profile → Import CSV.

What gets skipped, and why (printed at the end):
  - signals that aren't a plain 8/16/24/32-bit big-endian number
  - signed values wider than one byte (the app's formula language has no 16-bit SIGNED)
  - commands marked fcm1: their payload may start one byte later, and that isn't confirmed

If you publish the converted file, keep the OBDb attribution: CC BY-SA 4.0.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.request
from pathlib import Path

RAW = "https://raw.githubusercontent.com/OBDb/{repo}/main/signalsets/v3/default.json"

# OBDb unit names → what to show on a gauge.
UNITS = {
    "celsius": "°C",
    "fahrenheit": "°F",
    "kilometersPerHour": "km/h",
    "milesPerHour": "mph",
    "rpm": "rpm",
    "percent": "%",
    "volts": "V",
    "kilopascal": "kPa",
    "psi": "psi",
    "gramsPerSecond": "g/s",
    "seconds": "s",
    "milliseconds": "ms",
    "kilometers": "km",
    "meters": "m",
    "liters": "L",
    "degrees": "°",
    "scalar": "",
    "hex": "",
    "offon": "",
    "ratio": "",
}

LETTERS = "ABCD"


def raw_expr(nbytes: int) -> str:
    """Big-endian read of nbytes: A, A*256+B, A*65536+B*256+C, …"""
    parts = []
    for i in range(nbytes):
        mult = 256 ** (nbytes - 1 - i)
        parts.append(LETTERS[i] if mult == 1 else f"{LETTERS[i]}*{mult}")
    return "+".join(parts)


def _wrap(expr: str) -> str:
    """Parenthesise when the next operator would otherwise bind to the last term."""
    return f"({expr})" if ("+" in expr or "-" in expr) else expr


def _num(v: float) -> str:
    return f"{v:g}"


def build_formula(fmt: dict) -> str | None:
    bits = fmt.get("len")
    if bits not in (8, 16, 24, 32):
        return None
    nbytes = bits // 8
    expr = raw_expr(nbytes)
    if fmt.get("sign"):
        if nbytes != 1:
            return None                      # no two's complement for 16/32 in this language
        expr = f"SIGNED({LETTERS[0]})"
    mul, div, add = fmt.get("mul", 1), fmt.get("div", 1), fmt.get("add", 0)
    if mul != 1:
        expr = f"{_wrap(expr)}*{_num(mul)}"
    if div != 1:
        expr = f"{_wrap(expr)}/{_num(div)}"
    if add != 0:
        expr = f"{_wrap(expr)}{add:+g}"
    return expr


def _cell(s: str) -> str:
    return '"' + s.replace('"', '""') + '"' if any(c in s for c in ',"\n') else s


def convert(data: dict) -> tuple[str, list[str]]:
    rows: list[list[str]] = []
    skipped: list[str] = []
    seen: dict[str, int] = {}
    for cmd in data.get("commands", []):
        signals = cmd.get("signals") or []
        c = cmd.get("cmd") or {}
        header = cmd.get("hdr")
        if len(signals) != 1:
            skipped.append(f"{header} {c}: {len(signals)} signals in one reply — not supported")
            continue
        sig = signals[0]
        name = (sig.get("name") or sig.get("id") or "").strip()
        if cmd.get("fcm1"):
            skipped.append(f"{name}: fcm1 (payload may start one byte late; unconfirmed)")
            continue
        service = next(iter(c), None)
        pid = c.get(service) if service else None
        if not service or not pid:
            skipped.append(f"{name}: no service/PID")
            continue
        request = f"{service}{pid}".upper()
        fmt = sig.get("fmt") or {}
        formula = build_formula(fmt)
        if formula is None:
            skipped.append(f"{name}: {fmt.get('len')}-bit value {fmt.get('sign') and 'signed' or ''}".strip())
            continue
        unit = UNITS.get(fmt.get("unit", ""), fmt.get("unit", ""))
        n = seen.get(name, 0) + 1
        seen[name] = n
        if n > 1:
            name = f"{name} ({n})"
        rows.append([
            _cell(name),
            _cell((sig.get("id") or name)[:14]),
            request,
            formula,
            str(fmt.get("min", 0)),
            str(fmt.get("max", "")),
            _cell(unit),
            header or "",
        ])
    out = ["Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header"]
    out += [",".join(r) for r in rows]
    return "\n".join(out) + "\n", skipped


def load(source: str) -> dict:
    if source.startswith("http"):
        with urllib.request.urlopen(source, timeout=30) as r:
            return json.load(r)
    path = Path(source)
    if path.exists():
        return json.loads(path.read_text())
    # Anything else that looks like an OBDb repo name is fetched from GitHub.
    if re.fullmatch(r"[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)?", source):
        with urllib.request.urlopen(RAW.format(repo=source), timeout=30) as r:
            return json.load(r)
    raise SystemExit(f"No such file and not an OBDb repo: {source}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source", nargs="?", help="OBDb default.json path, or a repo like Chevrolet-Cruze")
    ap.add_argument("-o", "--out", type=Path, default=Path("obdb-pids.csv"))
    ap.add_argument("--from-github", metavar="REPO", help="shorthand for an OBDb repo, e.g. Chevrolet-Cruze")
    args = ap.parse_args()

    source = args.from_github or args.source
    if not source:
        ap.error("give a default.json path, a repo name, or --from-github REPO")
    data = load(source)
    csv, skipped = convert(data)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(csv)
    kept = csv.count("\n") - 1
    print(f"{kept} PIDs → {args.out}")
    if skipped:
        print(f"{len(skipped)} skipped:")
        for s in skipped[:12]:
            print("  -", s)
        if len(skipped) > 12:
            print(f"  … and {len(skipped) - 12} more")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
