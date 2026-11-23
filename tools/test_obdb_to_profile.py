#!/usr/bin/env python3
"""Tests for obdb_to_profile.py — fixtures mirror the real OBDb Cruze file.

Run: python3 -m unittest discover -s tools -p "test_*.py"
"""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from obdb_to_profile import build_formula, convert  # noqa: E402


def sig(name, length, **fmt):
    f = {"len": length, "max": 255, "unit": "scalar"}
    f.update(fmt)
    return {"name": name, "id": name.upper().replace(" ", "_"), "fmt": f}


def cmd(header, service, pid, signal, **extra):
    c = {"hdr": header, "cmd": {service: pid}, "signals": [signal]}
    c.update(extra)
    return c


class Formulas(unittest.TestCase):
    def test_byte_widths(self):
        self.assertEqual(build_formula({"len": 8}), "A")
        self.assertEqual(build_formula({"len": 16}), "A*256+B")
        self.assertEqual(build_formula({"len": 32}), "A*16777216+B*65536+C*256+D")

    def test_offset_and_scale(self):
        self.assertEqual(build_formula({"len": 8, "add": -40}), "A-40")
        self.assertEqual(build_formula({"len": 16, "mul": 4}), "(A*256+B)*4")
        self.assertEqual(build_formula({"len": 8, "div": 10}), "A/10")
        self.assertEqual(build_formula({"len": 8, "mul": 100, "div": 255}), "A*100/255")
        self.assertEqual(build_formula({"len": 8, "mul": 3, "div": 5, "add": -21.6}), "A*3/5-21.6")

    def test_signed_only_at_one_byte(self):
        self.assertEqual(build_formula({"len": 8, "sign": True}), "SIGNED(A)")
        self.assertIsNone(build_formula({"len": 16, "sign": True}))

    def test_odd_widths_are_refused(self):
        self.assertIsNone(build_formula({"len": 1}))
        self.assertIsNone(build_formula({"len": 12}))


CRUZE = {"commands": [
    cmd("7E0", "22", "1940", sig("Transmission fluid temperature", 8, add=-40, min=-40,
                                 max=215, unit="celsius")),
    cmd("7E0", "22", "000D", sig("Vehicle speed", 8, unit="kilometersPerHour")),
    cmd("7E0", "22", "2345", sig("Engine oil pressure", 8, mul=400, div=100, max=1020,
                                 unit="kilopascal")),
    cmd("7E0", "22", "11A1", sig("Time since engine start", 16, max=65535, unit="seconds")),
    cmd("7E2", "22", "1940", sig("Transmission fluid temperature", 8, add=-40, min=-40,
                                 max=215, unit="celsius")),
    # fcm1: payload may start a byte later, so it is left out rather than guessed
    cmd("7E0", "22", "0010", sig("Mass airflow sensor", 16, mul=100, div=10000,
                                 unit="gramsPerSecond"), fcm1=True),
]}


class Convert(unittest.TestCase):
    def setUp(self):
        self.csv, self.skipped = convert(CRUZE)
        self.rows = [r for r in self.csv.strip().splitlines()[1:]]

    def test_header(self):
        self.assertEqual(self.csv.splitlines()[0],
                         "Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header")

    def test_known_cruze_rows(self):
        self.assertIn('Transmission fluid temperature,TRANSMISSION_F,221940,A-40,-40,215,°C,7E0', self.rows)
        self.assertIn("Vehicle speed,VEHICLE_SPEED,22000D,A,0,255,km/h,7E0", self.rows)
        self.assertIn("Engine oil pressure,ENGINE_OIL_PRE,222345,A*400/100,0,1020,kPa,7E0", self.rows)
        self.assertIn("Time since engine start,TIME_SINCE_ENG,2211A1,A*256+B,0,65535,s,7E0", self.rows)

    def test_same_pid_on_two_modules_stays_distinct(self):
        self.assertTrue(any(r.endswith(",7E2") and ",221940," in r for r in self.rows))
        self.assertTrue(any(r.endswith(",7E0") and ",221940," in r for r in self.rows))

    def test_duplicate_names_get_a_suffix(self):
        self.assertTrue(any(r.startswith("Transmission fluid temperature (2),") for r in self.rows))

    def test_fcm1_is_skipped_with_a_reason(self):
        self.assertEqual(len(self.rows), 5)
        self.assertTrue(any("fcm1" in s for s in self.skipped))

    def test_odb_vectors_decode(self):
        # OBDb's own test vectors: 62 19 40 73 → 75 °C, 62 23 45 00 → 0 kPa,
        # 62 11 A1 00 25 → 37 s, 62 00 0D 00 → 0 km/h.
        def eval_row(pid):
            row = next(r for r in self.rows if f",{pid}," in r)
            return row.split(",")[3]

        def apply(formula, a=0, b=0):
            return eval(formula.replace("^", "**"), {"A": a, "B": b, "__builtins__": {}})

        self.assertEqual(apply(eval_row("221940"), a=0x73), 75)
        self.assertEqual(apply(eval_row("222345"), a=0x00), 0)
        self.assertEqual(apply(eval_row("2211A1"), a=0x00, b=0x25), 37)
        self.assertEqual(apply(eval_row("22000D"), a=0x00), 0)


if __name__ == "__main__":
    unittest.main()
