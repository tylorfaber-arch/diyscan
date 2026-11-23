# Reverse-engineering a scan tool's data, on the wire

The goal: come away from a short loan of a good scan tool with a complete, replayable
description of what that tool asks the vehicle — so the same data can be read afterwards
with a cheap adapter.

This works because the tool's value on the bus is its **list of requests**. A module's reply
to a given request doesn't change over time, so capturing the requests once is enough; the
replies can be regenerated later by anything that can send the same bytes.

It does **not** include dumping or decrypting the tool's own software or database. That is a
licence and legal problem, and the wire gives you the same answers for your own vehicle.

## What you can and can't get

| | |
|---|---|
| Every request the tool sends, and each module's reply | yes |
| Which byte of a reply carries the value on screen | yes, by correlating with what you did |
| PIDs for modules the tool never opened | yes, with a read-only sweep (mode 22) |
| The tool's names/formulas for other vehicles | no — that lives in its database |
| Its encrypted files or firmware | no, and don't try |

## The one-shot capture

You get one session with the tool, so capture everything and decide later.

1. OBD2 Y-splitter. Tool on one branch, sniffer on the other. Key on, engine off first.
2. Start capturing **before** the tool finishes booting, so its start-up traffic is in the file.
3. Walk every menu the tool has: each module (engine, transmission, ABS, body), every
   live-data page, freeze frame, readiness, stored/pending/permanent codes, every
   "test results" screen. Pause a few seconds on each so replies are easy to pair.
4. If the tool has a record-all / all-data / log mode, run it — it asks for things you
   would never click.
5. Stop, save, copy the file off the phone.

### Hardware, per bus

- **CAN (the Cruze)**: use a CANable-class USB-CAN adapter with `candump` or SavvyCAN. A
  USB ELM327 cannot keep up at 500 kbps and a capture with holes is worse than no capture.
- **J1850 VPW (the Ram)**: 10.4 kbps is slow enough that an ELM327 in monitor mode (the
  app's Bus tab) keeps every frame. That is the one case the phone is the right sniffer.

Stick to reading. Requests that run actuator tests, write values, or do the seed/key
handshake can change module state; keep the capture and any replay read-only.

## Turn the capture into a catalog

```bash
python3 tools/obd_catalog.py capture.csv -o out/
```

Accepts the app's `time_ms,frame` captures, SavvyCAN CSV exports, and `candump -l` logs,
including ISO-TP multi-frame replies.

It writes:

- `out/catalog.csv` — one row per module + request: how many times the tool asked, a
  sample reply, and `moving_bytes`, the reply bytes that took more than one value. Those
  are the bytes worth decoding.
- `out/replay.txt` — the same requests deduped and in order: the list to replay later.

Example (synthetic Cruze capture: transmission temperature on module `7E1`, rpm on `7E0`):

```
module,request,times_asked,unique_replies,sample_reply,reply_data,moving_bytes
7E1,221940,4,4,6219405A,5A,0:4
7E0,010C,4,4,410C1AF8,1AF8,1:4
```

`7E1` + `22 19 40` → `62 19 40 5A`: the payload is one byte, `5A`. Read it off the tool's
screen at two different temperatures and the formula is `A-40`.

## After the tool is gone

`replay.txt` is the durable part. Replaying those exact requests with any adapter
reproduces every reading the tool showed.

The app has that screen now: **Bus → Request replay → Import replay.txt → Run**. It sends
only read services (01, 02, 03, 05, 06, 07, 09, 0A, 21, 22) and refuses everything else,
so a replay cannot clear codes, start a routine, write a value, or answer a seed/key
exchange. On a CAN vehicle, **Sweep mode 22** asks the engine (7E0) and transmission (7E1)
for every PID from 0000 to 00FF and keeps whatever answers.

Test it without a vehicle: **Demo plan** runs against the simulator.
