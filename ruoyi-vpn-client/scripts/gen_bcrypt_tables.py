#!/usr/bin/env python3
"""从 Spring Security BCrypt.java 提取 P/S 表，生成 bcrypt_tables.h"""
import re
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = pathlib.Path(
    r"C:\Users\echoy\.cursor\projects\d-cursor-genlot-GenlotVPN-Cloud\agent-tools"
    r"\e0bfa772-e102-4e91-bfc3-538729e3fd6f.txt"
)
OUT = pathlib.Path(__file__).resolve().parents[1] / "src" / "crypto" / "bcrypt_tables.h"

text = SRC.read_text(encoding="utf-8")
p_match = re.search(r"private static final int P_orig\[\] = \{(.*?)\};", text, re.S)
s_match = re.search(r"private static final int S_orig\[\] = \{(.*?)\};", text, re.S)
p_vals = [int(x.strip(), 0) for x in p_match.group(1).replace("\n", " ").split(",") if x.strip()]
s_vals = [int(x.strip(), 0) for x in s_match.group(1).replace("\n", " ").split(",") if x.strip()]

lines = [
    "#pragma once",
    "#include <cstdint>",
    "",
    "namespace vpn::bcrypt_tables {",
    "",
    "inline constexpr uint32_t P_ORIG[18] = {",
]
for i, v in enumerate(p_vals):
    suffix = "," if i < len(p_vals) - 1 else ""
    lines.append(f"    0x{v:08x}U{suffix}")
lines.append("};")
lines.append("")
lines.append("inline constexpr uint32_t S_ORIG[1024] = {")
for i in range(0, len(s_vals), 4):
    chunk = s_vals[i : i + 4]
    suffix = "," if i + 4 < len(s_vals) else ""
    lines.append("    " + ", ".join(f"0x{v:08x}U" for v in chunk) + suffix)
lines.append("};")
lines.append("")
lines.append("inline constexpr uint32_t BF_CRYPT_CIPHERTEXT[6] = {")
lines.append("    0x4f727068U, 0x65616e42U, 0x65686f6cU, 0x64657253U, 0x63727944U, 0x6f756274U")
lines.append("};")
lines.append("")
lines.append("} // namespace vpn::bcrypt_tables")
lines.append("")

OUT.write_text("\n".join(lines), encoding="utf-8")
print(f"Wrote {OUT} (P={len(p_vals)}, S={len(s_vals)})")
