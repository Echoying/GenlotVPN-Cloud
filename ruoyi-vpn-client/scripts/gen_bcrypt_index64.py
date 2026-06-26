#!/usr/bin/env python3
"""从 Spring BCrypt.java 生成 bcrypt_index64.h"""
import re
from pathlib import Path

SRC = Path(
    r"C:\Users\echoy\.cursor\projects\d-cursor-genlot-GenlotVPN-Cloud\agent-tools"
    r"\e0bfa772-e102-4e91-bfc3-538729e3fd6f.txt"
)
OUT = Path(__file__).resolve().parents[1] / "src" / "crypto" / "bcrypt_index64.h"

text = SRC.read_text(encoding="utf-8")
m = re.search(r"static private final byte index_64\[\] = \{(.*?)\};", text, re.S)
vals = [int(x.strip()) for x in m.group(1).replace("\n", " ").split(",") if x.strip()]

lines = [
    "#pragma once",
    "#include <array>",
    "#include <cstdint>",
    "",
    "namespace vpn::bcrypt_tables {",
    "",
    "inline constexpr std::array<int8_t, 128> INDEX_64 = {",
]
for i in range(0, len(vals), 16):
    chunk = vals[i : i + 16]
    suffix = "," if i + 16 < len(vals) else ""
    lines.append("    " + ", ".join(f"{v:3d}" for v in chunk) + suffix)
lines.append("};")
lines.append("")
lines.append("} // namespace vpn::bcrypt_tables")
lines.append("")

OUT.write_text("\n".join(lines), encoding="utf-8")
print(f"Wrote {OUT} ({len(vals)} entries)")
