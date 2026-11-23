#!/usr/bin/env python3
"""
Log an ELM327's bus traffic to a CSV, using only the Python standard library.

This is the PC-side sniffer for pre-CAN vehicles (J1850 VPW / PWM, ISO 9141, KWP2000),
where 10.4 kbps is slow enough that an ELM327 in monitor mode keeps every frame. It sends
ATMA and records every line the adapter prints, with a millisecond timestamp, in the same
`time_ms,frame` format the phone app writes — so `tools/obd_catalog.py` reads it directly.

For CAN (anything 2008+) use a CANable and `candump -l`, not this: an ELM327 drops frames
at 500 kbps.

Usage:
  python3 tools/elm_sniff.py /dev/ttyUSB0 -o capture.csv
  python3 tools/elm_sniff.py /dev/ttyUSB0 --baud 115200 -o capture.csv

Needs read/write access to the port: `sudo usermod -aG dialout $USER`, then log back in.
Stop with Ctrl-C; the adapter is left in its normal state.
"""

from __future__ import annotations

import argparse
import os
import select
import sys
import termios
import time
from pathlib import Path

BAUDS = {
    9600: termios.B9600,
    19200: termios.B19200,
    38400: termios.B38400,
    57600: termios.B57600,
    115200: termios.B115200,
}

# Cheap clones answer at 38400; OBDLink SX and most genuine adapters at 115200.
TRY_BAUDS = [38400, 115200, 9600, 57600]


class Port:
    """A raw 8N1 serial port. Small on purpose: no pyserial to install."""

    def __init__(self, path: str, baud: int):
        if baud not in BAUDS:
            raise ValueError(f"unsupported baud {baud}; use one of {sorted(BAUDS)}")
        self.fd = os.open(path, os.O_RDWR | os.O_NOCTTY | os.O_NONBLOCK)
        attrs = termios.tcgetattr(self.fd)
        attrs[0] = 0                                  # iflag: raw
        attrs[1] = 0                                  # oflag: raw
        attrs[2] = termios.CS8 | termios.CREAD | termios.CLOCAL
        attrs[3] = 0                                  # lflag: raw
        attrs[4] = attrs[5] = BAUDS[baud]             # ispeed, ospeed
        attrs[6][termios.VMIN] = 0
        attrs[6][termios.VTIME] = 0
        termios.tcsetattr(self.fd, termios.TCSANOW, attrs)
        termios.tcflush(self.fd, termios.TCIOFLUSH)

    def write(self, data: bytes) -> None:
        os.write(self.fd, data)

    def read(self, wait: float = 0.2) -> bytes:
        ready, _, _ = select.select([self.fd], [], [], wait)
        if not ready:
            return b""
        try:
            return os.read(self.fd, 8192)
        except BlockingIOError:
            return b""

    def close(self) -> None:
        try:
            os.close(self.fd)
        except OSError:
            pass


def _read_until_prompt(port: Port, timeout: float) -> str:
    buf = bytearray()
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        chunk = port.read(0.1)
        if not chunk:
            continue
        buf += chunk
        if b">" in buf:
            break
    return buf.decode(errors="replace")


def _command(port: Port, cmd: str, timeout: float = 2.0) -> str:
    port.write((cmd + "\r").encode())
    return _read_until_prompt(port, timeout)


def connect(port_path: str, baud: int | None) -> tuple[Port, int]:
    """Open the port and prove the adapter answers. Tries the usual baud rates if not given."""
    last: Exception | None = None
    for b in ([baud] if baud else TRY_BAUDS):
        try:
            port = Port(port_path, b)
        except OSError as e:
            raise SystemExit(f"Can't open {port_path}: {e}") from e
        port.write(b"ATI\r")
        reply = _read_until_prompt(port, 2.0)
        if ">" in reply:
            return port, b
        port.close()
        last = RuntimeError(f"no answer at {b} baud")
    raise SystemExit(f"{port_path} isn't answering as an ELM327: {last}")


def initialize(port: Port) -> str:
    """Reset, silence the adapter's own formatting, turn headers on, auto-detect protocol."""
    for cmd in ("ATZ", "ATE0", "ATL0", "ATS0", "ATH1", "ATSP0"):
        _command(port, cmd, 2.0)
    protocol = _command(port, "ATDPN", 2.0).strip().strip(">").strip()
    _command(port, "0100", 15.0)  # first request triggers the protocol search
    return protocol


def capture(port_path: str, out: Path, baud: int | None = None,
            seconds: float | None = None, quiet: bool = False) -> int:
    """Record ATMA until Ctrl-C (or `seconds`), returning how many frames were kept."""
    port, baud_used = connect(port_path, baud)
    kept = 0
    try:
        protocol = initialize(port)
        if not quiet:
            print(f"{port_path} at {baud_used} baud, protocol {protocol or '?'}", file=sys.stderr)
            print("Capturing. Ctrl-C to stop.", file=sys.stderr)

        port.write(b"ATMA\r")
        buf = bytearray()
        start = time.monotonic()
        out.parent.mkdir(parents=True, exist_ok=True)
        with out.open("w") as fh:
            fh.write("time_ms,frame\n")
            while seconds is None or time.monotonic() - start < seconds:
                chunk = port.read(0.1)
                if not chunk:
                    continue
                buf += chunk
                while b"\r" in buf or b"\n" in buf:
                    i = min(x for x in (buf.find(b"\r"), buf.find(b"\n")) if x >= 0)
                    line = buf[:i].decode(errors="replace").strip()
                    del buf[:i + 1]
                    if not line or line.startswith(("SEARCHING", "ATMA", "STOPPED", "BUFFER FULL", ">")):
                        continue
                    fh.write(f"{int(time.time() * 1000)},{line}\n")
                    kept += 1
            fh.flush()
    except KeyboardInterrupt:
        pass
    finally:
        try:
            port.write(b"\r")                     # any byte stops ATMA
            _read_until_prompt(port, 2.0)
            _command(port, "ATH0", 1.0)           # leave it as the app expects
        except OSError:
            pass
        port.close()
    if not quiet:
        print(f"\n{kept} frames → {out}", file=sys.stderr)
    return kept


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("port", help="serial device, e.g. /dev/ttyUSB0 or /dev/ttyACM0")
    ap.add_argument("-o", "--out", type=Path, default=Path("capture.csv"), help="output CSV (default capture.csv)")
    ap.add_argument("--baud", type=int, choices=sorted(BAUDS), help="force a baud rate (default: try 38400, 115200, 9600, 57600)")
    ap.add_argument("--seconds", type=float, help="stop after this many seconds (default: until Ctrl-C)")
    args = ap.parse_args()
    capture(args.port, args.out, args.baud, args.seconds)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
