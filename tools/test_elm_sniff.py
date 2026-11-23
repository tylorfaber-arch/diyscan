#!/usr/bin/env python3
"""Tests for elm_sniff.py against a fake ELM327 on a pseudo-terminal.

Run: python3 -m unittest discover -s tools -p "test_*.py"
"""

import os
import pty
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from elm_sniff import capture  # noqa: E402

FRAMES = [
    "68 6A F1 41 0C 1A F8",
    "68 F1 6A 41 0C 1A F8",
    "68 6A F1 41 0D 1E",
    "48 6B 10 41 0C 1A F8",
]


class FakeElm(threading.Thread):
    """Answers like an ELM327 with headers on: AT commands get prompts, ATMA streams."""

    def __init__(self, master: int):
        super().__init__(daemon=True)
        self.master = master
        self.running = True
        self.monitoring = False
        self.seen: list[str] = []

    def send(self, text: str) -> None:
        try:
            os.write(self.master, text.encode())
        except OSError:
            pass

    def run(self) -> None:
        buf = b""
        while self.running:
            try:
                chunk = os.read(self.master, 1024)
            except OSError:
                break
            if not chunk:
                continue
            buf += chunk
            while b"\r" in buf:
                i = buf.index(b"\r")
                cmd = buf[:i].decode(errors="replace").strip()
                buf = buf[i + 1:]
                self.handle(cmd)

    def handle(self, cmd: str) -> None:
        if not cmd:
            if self.monitoring:                     # any byte stops ATMA
                self.monitoring = False
                self.send("STOPPED\r\r>")
            return
        self.seen.append(cmd)
        if cmd == "ATI":
            self.send("ELM327 v1.5\r\r>")
        elif cmd == "ATDPN":
            self.send("A2\r\r>")
        elif cmd == "ATMA":
            self.monitoring = True
            threading.Thread(target=self.stream, daemon=True).start()
        elif cmd.startswith("AT"):
            self.send("OK\r\r>")
        else:
            self.send("NO DATA\r\r>")

    def stream(self) -> None:
        n = 0
        while self.monitoring and self.running:
            self.send(FRAMES[n % len(FRAMES)] + "\r")
            n += 1
            time.sleep(0.02)


class ElmSniffTest(unittest.TestCase):
    def test_records_frames_with_timestamps(self):
        master, slave = pty.openpty()
        name = os.ttyname(slave)
        fake = FakeElm(master)
        fake.start()
        try:
            with tempfile.TemporaryDirectory() as d:
                out = Path(d) / "capture.csv"
                kept = capture(name, out, baud=38400, seconds=0.6, quiet=True)
                lines = out.read_text().splitlines()
        finally:
            fake.running = False
            os.close(slave)
            os.close(master)

        self.assertGreater(kept, 2)
        self.assertEqual(lines[0], "time_ms,frame")
        rows = lines[1:]
        self.assertEqual(len(rows), kept)
        for row in rows:
            ts, _, frame = row.partition(",")
            self.assertTrue(ts.isdigit(), row)
            self.assertIn(" ", frame)
            self.assertTrue(all(c in "0123456789ABCDEF " for c in frame), row)

    def test_initializes_headers_and_stops_cleanly(self):
        master, slave = pty.openpty()
        name = os.ttyname(slave)
        fake = FakeElm(master)
        fake.start()
        try:
            with tempfile.TemporaryDirectory() as d:
                capture(name, Path(d) / "c.csv", baud=38400, seconds=0.4, quiet=True)
        finally:
            fake.running = False
            os.close(slave)
            os.close(master)

        # Headers on is what makes the capture decodable, so it must be in the init sequence.
        self.assertIn("ATH1", fake.seen)
        self.assertIn("ATMA", fake.seen)


if __name__ == "__main__":
    unittest.main()
