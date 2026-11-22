#!/usr/bin/env python3
"""Tests for obd_catalog.py — synthetic captures, no vehicle needed.

Run: python3 -m unittest discover -s tools -p "test_*.py"
"""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from obd_catalog import Frame, build, read_capture, strip_echo, variability  # noqa: E402


def capture(text: str) -> list[Frame]:
    with tempfile.TemporaryDirectory() as d:
        p = Path(d) / "capture.csv"
        p.write_text(text)
        return read_capture(p)


def app(ts: int, hexdata: str) -> str:
    return f"{ts},{hexdata}"


def can(ts: int, bid: str, payload: bytes) -> str:
    return app(ts, bid + payload.hex().upper())


def pad(*b: int) -> bytes:
    """An 8-byte CAN frame, padded the way an ELM327 prints it."""
    return bytes(list(b) + [0] * (8 - len(b)))


def vpw(ts: int, header: bytes, payload: bytes) -> str:
    return app(ts, header.hex().upper() + payload.hex().upper() + "AA")  # trailing CRC


CAN_TWO_READINGS = "\n".join([
    "time_ms,frame",
    can(1000, "7E0", pad(0x02, 0x01, 0x0C)),          # 02 01 0C = request rpm
    can(1200, "7E8", pad(0x04, 0x41, 0x0C, 0x1A, 0xF8)),
    can(1600, "7E0", pad(0x02, 0x01, 0x0C)),
    can(1800, "7E8", pad(0x04, 0x41, 0x0C, 0x1B, 0x00)),
])


class AppCanFormat(unittest.TestCase):
    def test_pairs_request_with_reply(self):
        entries, stats = build(capture(CAN_TWO_READINGS), window_ms=1000, legacy_crc=True)
        self.assertEqual(stats["paired"], 2)
        self.assertIn(("7E0", "010C"), entries)
        self.assertEqual(len(entries[("7E0", "010C")].replies), 2)

    def test_unwraps_the_iso_tp_single_frame(self):
        entries, _ = build(capture(CAN_TWO_READINGS), window_ms=1000, legacy_crc=True)
        first = entries[("7E0", "010C")].replies[0]
        self.assertEqual(first.hex().upper(), "410C1AF8")
        self.assertEqual(strip_echo(first).hex().upper(), "1AF8")

    def test_reports_the_byte_that_moves(self):
        entries, _ = build(capture(CAN_TWO_READINGS), window_ms=1000, legacy_crc=True)
        self.assertEqual(variability(entries[("7E0", "010C")].replies), "0:2 1:2")

    def test_one_reply_has_no_variability(self):
        text = "\n".join(CAN_TWO_READINGS.splitlines()[:3])
        entries, _ = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertEqual(variability(entries[("7E0", "010C")].replies), "")

    def test_marks_are_ignored_but_do_not_break_the_stream(self):
        text = "\n".join([
            "time_ms,frame",
            can(1000, "7E0", pad(0x02, 0x01, 0x0C)),
            '1100,MARK,"Trans temp"',
            can(1200, "7E8", pad(0x04, 0x41, 0x0C, 0x1A, 0xF8)),
        ])
        entries, _ = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertEqual(len(entries), 1)

    def test_orphan_reply_is_counted_not_guessed(self):
        text = "time_ms,frame\n" + can(1200, "7E8", pad(0x04, 0x41, 0x0C, 0x1A, 0xF8))
        entries, stats = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertEqual(len(entries), 0)
        self.assertEqual(stats["orphan_replies"], 1)

    def test_reply_outside_the_window_is_an_orphan(self):
        text = "\n".join([
            "time_ms,frame",
            can(1000, "7E0", pad(0x02, 0x01, 0x0C)),
            can(5000, "7E8", pad(0x04, 0x41, 0x0C, 0x1A, 0xF8)),
        ])
        entries, stats = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertEqual(len(entries), 0)
        self.assertEqual(stats["orphan_replies"], 1)


class IsoTp(unittest.TestCase):
    def test_multi_frame_reply_is_joined(self):
        body = bytes.fromhex("62F19001020304050607")   # 10 bytes, a mode 22 reply
        first, rest = body[:6], body[6:]
        text = "\n".join([
            "time_ms,frame",
            can(1000, "7E0", pad(0x03, 0x22, 0xF1, 0x90)),
            can(1200, "7E8", pad(0x10, len(body), *first)),
            can(1250, "7E8", pad(0x21, *rest)),
        ])
        entries, _ = build(capture(text), window_ms=1000, legacy_crc=True)
        reply = entries[("7E0", "22F190")].replies[0]
        self.assertEqual(reply.hex().upper(), body.hex().upper())
        self.assertEqual(strip_echo(reply).hex().upper(), "01020304050607")


class LegacyFormat(unittest.TestCase):
    def test_vpw_request_and_reply_pair_up(self):
        text = "\n".join([
            "time_ms,frame",
            vpw(2000, bytes([0x68, 0x6A, 0xF1]), bytes([0x01, 0x0C])),        # tester -> 6A
            vpw(2400, bytes([0x68, 0xF1, 0x6A]), bytes([0x41, 0x0C, 0x1A, 0xF8])),  # 6A -> tester
        ])
        entries, _ = build(capture(text), window_ms=2000, legacy_crc=True)
        self.assertEqual(len(entries), 1)
        key = ("6A-68", "010C")
        self.assertIn(key, entries)
        self.assertEqual(entries[key].replies[0].hex().upper(), "410C1AF8")

    def test_crc_byte_can_be_kept(self):
        text = "\n".join([
            "time_ms,frame",
            vpw(2000, bytes([0x68, 0x6A, 0xF1]), bytes([0x01, 0x0C])),
            vpw(2400, bytes([0x68, 0xF1, 0x6A]), bytes([0x41, 0x0C, 0x1A, 0xF8])),
        ])
        entries, _ = build(capture(text), window_ms=2000, legacy_crc=False)
        self.assertEqual(entries[("6A-68", "010CAA")].replies[0].hex().upper(), "410C1AF8AA")


class OtherFormats(unittest.TestCase):
    def test_savvycan_export(self):
        text = "\n".join([
            "Time Stamp,ID,Extended,Dir,Bus,LEN,Data",
            "0.100,7E0,0,0,0,8,02 01 0C 00 00 00 00 00",
            "0.120,7E8,0,0,0,8,04 41 0C 1A F8 00 00 00",
        ])
        entries, _ = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertIn(("7E0", "010C"), entries)

    def test_candump_log(self):
        text = "\n".join([
            "(1742930000.100000) can0 7E0#02010C0000000000",
            "(1742930000.120000) can0 7E8#04410C1AF8000000",
        ])
        entries, _ = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertIn(("7E0", "010C"), entries)

    def test_broadcast_request_takes_replies_from_any_module(self):
        text = "\n".join([
            "time_ms,frame",
            can(1000, "7DF", pad(0x02, 0x01, 0x0C)),
            can(1100, "7E8", pad(0x04, 0x41, 0x0C, 0x1A, 0xF8)),
            can(1150, "7E9", pad(0x04, 0x41, 0x0C, 0x1B, 0x00)),
        ])
        entries, _ = build(capture(text), window_ms=1000, legacy_crc=True)
        self.assertIn(("7E0", "010C"), entries)
        self.assertIn(("7E1", "010C"), entries)


if __name__ == "__main__":
    unittest.main()
