# M18H.1 — Short visual terminals

Follow-up visual polish after M18H manual QA.

## Change
- Shortened the visible terminal leads of resistor, capacitor, diode, LED, SPST switch, ground, and DC voltage source.
- Logical terminal anchors remain on the authored component perimeter.
- Snapping, hit-testing, endpoint IDs, connectivity, nets, clipboard, and history are unchanged.
- The DC voltage-source circle is larger inside the same authored bounds so its perimeter-to-body stems are shorter.
- M18H routing hardening remains in place.

## Intent
Avoid the visually prominent cross/T shapes seen when a wire runs through a terminal anchor while a long symbol lead continues into the component body.
