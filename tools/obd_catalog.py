#!/usr/bin/env python3
"""
Turn a raw OBD-II bus capture into a request/reply catalog.

A scan tool's real knowledge is the list of requests it sends. Capture that list once —
while you still have the tool — and the replies can be regenerated any time with a cheap
adapter, because a module's answer to a given request doesn't change.

Input: a capture recorded by another node on the bus (the app's Bus monitor, SavvyCAN, or
candump). Supported line formats:

  1742930000123,68 6A F1 41 0C 1A F8              app Bus monitor ("time_ms,frame")
  Time Stamp,ID,Extended,Dir,Bus,LEN,Data          SavvyCAN CSV export
  (1742930000.123) can0 7E0#02010C                 candump -l log

Output:
  catalog.csv   one row per module+request: how many times asked, a sample reply,
                and which reply bytes took more than one value
  replay.txt    the same requests, deduped, in the order first seen — a replay list

Read-only by construction: this only reads a capture. It never sends anything.

Usage:
  python3 tools/obd_catalog.py capture.csv -o out/
  python3 tools/obd_catalog.py candump.log --bus can --window 800
"""

from __future__ import annotations

import argparse
import sys
import csv
import re
from collections import Counter, OrderedDict
from dataclasses import dataclass, field
from pathlib import Path

HEX_RE = re.compile(r"[^0-9A-Fa-f]")


# --------------------------------------------------------------------------- formats

@dataclass
class Frame:
    """One bus frame: when, who, what. `bid` is the CAN id or the 3-byte legacy header."""
    ts: float
    bid: str
    data: bytes
    is_can: bool


def _hex(s: str) -> bytes | None:
    s = HEX_RE.sub("", s or "")
    if not s or len(s) % 2:
        return None
    try:
        return bytes.fromhex(s)
    except ValueError:
        return None


def _app_line(line: str, bus: str = "auto") -> Frame | None:
    """
    `1742930000123,68 6A F1 41 0C 1A F8` and `1742930000123,MARK,"Trans temp"`.

    The app writes spaces off, so the header length is the only protocol clue: legacy buses
    have a 3-byte header (6 hex chars) plus a CRC, CAN has a 3-char (11-bit) or 8-char
    (29-bit) id and no CRC — which is why a CAN line comes out an odd number of hex chars.
    """
    head, _, frame = line.partition(",")
    if not head.strip().isdigit():
        return None
    raw = frame.strip()
    if raw.upper().startswith("MARK"):
        return None
    s = HEX_RE.sub("", raw).upper()
    if not s:
        return None
    if bus == "can":
        idlen = 8 if s.startswith("18DA") else 3
    elif bus == "legacy":
        idlen = 6
    elif len(s) % 2 == 1:
        idlen = 3                       # 3-char id + an even number of data bytes
    elif s.startswith("18DA"):
        idlen = 8
    else:
        idlen = 6
    if len(s) <= idlen or (len(s) - idlen) % 2:
        return None
    return Frame(float(head.strip()), s[:idlen], bytes.fromhex(s[idlen:]), idlen != 6)


def _savvy_line(line: str) -> Frame | None:
    """SavvyCAN: Time Stamp,ID,Extended,Dir,Bus,LEN,Data."""
    parts = next(csv.reader([line]))
    if len(parts) < 7:
        return None
    ts, bid, extended, _direction, _bus, _length, data = parts[:7]
    try:
        secs = float(ts)
    except ValueError:
        return None
    payload = _hex(data)
    if payload is None:
        return None
    return Frame(secs * 1000.0, bid.strip().upper().removeprefix("0X"), payload, True)


def _candump_line(line: str) -> Frame | None:
    """candump: `(1742930000.123456) can0 7E0#02010C` or `can0 7E0 [4] 02 01 0C 00`."""
    m = re.match(r"^\((\d+\.?\d*)\)\s+\S+\s+([0-9A-Fa-f]+)#(\S*)", line)
    if m:
        ts, bid, payload = m.group(1), m.group(2), m.group(3)
        data = _hex(payload.split("!")[0])  # "!" separates CAN FD flags
        if data is None:
            return None
        return Frame(float(ts) * 1000.0, bid.upper(), data, True)
    m = re.match(r"^\S+\s+([0-9A-Fa-f]+)\s+\[(\d+)\]\s+(.*)$", line)
    if m:
        bid, _length, rest = m.groups()
        data = _hex(rest)
        return Frame(0.0, bid.upper(), data, True) if data else None
    return None


def read_capture(path: Path, bus: str = "auto") -> list[Frame]:
    frames: list[Frame] = []
    for line in path.read_text(errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        lowered = line.lower()
        if lowered.startswith("time stamp") or lowered.startswith("time_ms"):
            continue  # header row
        frame = None
        if line.startswith("("):
            frame = _candump_line(line)
        elif "," in line:
            first = line.split(",", 1)[0].strip()
            frame = _app_line(line, bus) if first.isdigit() else _savvy_line(line)
        if frame:
            frames.append(frame)
    frames.sort(key=lambda f: f.ts)
    return frames


# --------------------------------------------------------------------------- protocol

def is_request(f: Frame) -> tuple[bool, str]:
    """Return (is_a_tester_request, module_key). Module keys are replay targets."""
    if f.is_can:
        try:
            bid = int(f.bid, 16)
        except ValueError:
            return False, ""
        if 0x7E0 <= bid <= 0x7E7:            # 11-bit: tester -> module
            return True, f"{bid:X}"
        if 0x7E8 <= bid <= 0x7EF:            # 11-bit: module -> tester
            return False, f"{bid - 8:X}"
        if 0x7DF == bid:                     # broadcast request
            return True, "7DF"
        if (bid >> 16) == 0x18DA:            # 29-bit: 18DA<dest><src>
            dest, src = (bid >> 8) & 0xFF, bid & 0xFF
            if src == 0xF1:
                return True, f"18DA{dest:02X}F1"
            if dest == 0xF1:
                return False, f"18DA{src:02X}F1"
        return False, ""
    # J1850 / ISO 9141: header is [priority, target, source]
    if len(f.bid) != 6:
        return False, ""
    tgt, src = f.bid[2:4], f.bid[4:6]
    if src == "F1":
        return True, f.bid[2:4] + "-" + f.bid[0:2]
    if tgt == "F1":
        return False, src + "-" + f.bid[0:2]
    return False, ""


def reassemble(frames: list[Frame]) -> list[Frame]:
    """Join ISO-TP multi-frame messages on CAN; leave everything else alone."""
    out: list[Frame] = []
    pending: dict[str, tuple[bytes, float]] = {}
    for f in frames:
        if not f.is_can or not f.data:
            out.append(f)
            continue
        kind = f.data[0] >> 4
        if kind == 0x0:                       # single frame
            n = f.data[0] & 0x0F
            out.append(Frame(f.ts, f.bid, f.data[1:1 + n], True))
        elif kind == 0x1:                     # first frame of a multi-frame message
            total = ((f.data[0] & 0x0F) << 8) | f.data[1]
            pending[f.bid] = (f.data[2:], total)
        elif kind == 0x2:                     # consecutive frame
            if f.bid not in pending:
                continue
            body, total = pending[f.bid]
            body += f.data[1:]
            if len(body) >= total:
                out.append(Frame(f.ts, f.bid, body[:total], True))
                del pending[f.bid]
            else:
                pending[f.bid] = (body, total)
        elif kind == 0x3:                     # flow control — not ours
            continue
        else:
            out.append(f)
    return out


# Byte after which the actual data starts, per positive response service.
ECHO_LEN = {0x41: 2, 0x42: 2, 0x43: 1, 0x44: 1, 0x45: 1, 0x46: 1,
            0x47: 1, 0x48: 1, 0x49: 3, 0x4A: 1, 0x62: 3, 0x63: 1}


def strip_echo(reply: bytes) -> bytes:
    return reply[ECHO_LEN.get(reply[0], 1):]


# --------------------------------------------------------------------------- catalog

@dataclass
class Entry:
    module: str
    request: str
    replies: list[bytes] = field(default_factory=list)
    first_seen: float = 0.0


def build(frames: list[Frame], window_ms: float, legacy_crc: bool) -> tuple[dict, Counter]:
    frames = reassemble(frames)
    stats: Counter = Counter()
    pending: dict[tuple[bool, str], tuple[bytes, float]] = {}
    entries: "OrderedDict[tuple[str, str], Entry]" = OrderedDict()

    for f in frames:
        data = f.data
        if not f.is_can and legacy_crc and data:
            data = data[:-1]                  # J1850 / ISO frames carry a CRC byte at the end
        req, module = is_request(f)
        if req:
            key = (f.is_can, module)
            pending[key] = (data, f.ts)
            stats["requests"] += 1
            continue
        if not module:
            stats["ignored_frames"] += 1
            continue
        # Accept a reply for this module, or the broadcast request's module list.
        src = pending.get((f.is_can, module)) or pending.get((f.is_can, "7DF"))
        if not src or f.ts - src[1] > window_ms:
            stats["orphan_replies"] += 1
            continue
        request, _ = src
        stats["paired"] += 1
        key = (module, request.hex().upper())
        e = entries.setdefault(key, Entry(module, request.hex().upper(), [], f.ts))
        e.replies.append(data)
    return entries, stats


def variability(replies: list[bytes]) -> str:
    """Which payload bytes took more than one value, as `offset:distinct` pairs."""
    if len(replies) < 2:
        return ""
    payloads = [strip_echo(r) for r in replies]
    n = min((len(p) for p in payloads), default=0)
    moving = []
    for i in range(n):
        distinct = len({p[i] for p in payloads})
        if distinct > 1:
            moving.append(f"{i}:{distinct}")
    return " ".join(moving)


def write_catalog(entries: dict, out: Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    with (out / "catalog.csv").open("w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["module", "request", "times_asked", "unique_replies",
                    "sample_reply", "reply_data", "moving_bytes"])
        for e in entries.values():
            counts = Counter(r.hex().upper() for r in e.replies)
            sample = counts.most_common(1)[0][0]
            payload = strip_echo(bytes.fromhex(sample)).hex().upper()
            w.writerow([e.module, e.request, len(e.replies), len(counts), sample, payload,
                        variability(e.replies)])
    with (out / "replay.txt").open("w") as fh:
        for e in entries.values():
            fh.write(f"{e.module} {e.request}\n")


def report(entries: dict, stats: Counter) -> str:
    by_module: dict[str, int] = {}
    for e in entries.values():
        by_module[e.module] = by_module.get(e.module, 0) + 1
    lines = [
        f"paired request/reply exchanges : {stats['paired']}",
        f"distinct module+request pairs  : {len(entries)}",
        f"modules heard from             : {len(by_module)}",
        f"orphan replies (no matching request within the window): {stats['orphan_replies']}",
        "",
        "module        requests",
    ]
    for module, n in sorted(by_module.items(), key=lambda kv: (-kv[1], kv[0])):
        lines.append(f"  {module:<12} {n}")
    moving = [e for e in entries.values() if variability(e.replies)]
    if moving:
        lines += ["", "requests whose reply moves (the interesting ones):"]
        for e in sorted(moving, key=lambda e: -len(e.replies))[:15]:
            lines.append(f"  {e.module:<10} {e.request:<14} {len(e.replies):>5} replies  bytes {variability(e.replies)}")
    return "\n".join(lines)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("capture", type=Path, help="capture file (app CSV, SavvyCAN CSV, or candump log)")
    ap.add_argument("-o", "--out", type=Path, default=Path("out"), help="output directory (default: out/)")
    ap.add_argument("--window", type=float, default=1500.0,
                    help="how long a reply may take, in ms (default 1500; legacy buses are slow)")
    ap.add_argument("--no-legacy-crc", action="store_true",
                    help="keep the last byte of J1850/ISO frames (use if your adapter hides the CRC)")
    ap.add_argument("--bus", choices=["auto", "can", "legacy"], default="auto",
                    help="protocol of an app-format capture, if auto-detection guesses wrong")
    args = ap.parse_args()

    frames = read_capture(args.capture, args.bus)
    if not frames:
        print(f"No frames found in {args.capture}", file=sys.stderr)
        return 1
    entries, stats = build(frames, args.window, legacy_crc=not args.no_legacy_crc)
    write_catalog(entries, args.out)
    print(f"{len(frames)} frames from {args.capture}")
    print(report(entries, stats))
    print(f"\nWrote {args.out / 'catalog.csv'} and {args.out / 'replay.txt'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
