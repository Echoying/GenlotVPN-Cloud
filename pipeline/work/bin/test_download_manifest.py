# -*- coding: utf-8 -*-
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from download_manifest import (
    WIN_SDK,
    build_manifest,
    pick_max_sdk,
    pick_max_xyz,
    parse_xyz,
    write_manifest,
)


class ManifestTest(unittest.TestCase):
    def test_parse_xyz(self):
        self.assertEqual(parse_xyz("1.0.2"), (1, 0, 2))
        self.assertIsNone(parse_xyz("1.0"))

    def test_pick_max_xyz(self):
        names = [
            "GenlotVPN-win64-1.0.1.zip",
            "GenlotVPN-win64-1.0.2.zip",
            "readme.txt",
        ]
        got = pick_max_xyz(names, r"^GenlotVPN-win64-(\d+\.\d+\.\d+)\.zip$")
        self.assertEqual(got[0], "GenlotVPN-win64-1.0.2.zip")
        self.assertEqual(got[1], "1.0.2")

    def test_pick_max_sdk(self):
        names = [
            "EnUES_win_v3.3.7.0114_SDK.exe",
            "EnUES_win_v3.3.7.0132_SDK.exe",
        ]
        got = pick_max_sdk(names, WIN_SDK)
        self.assertEqual(got, "EnUES_win_v3.3.7.0132_SDK.exe")

    def test_write_manifest(self):
        root = Path(tempfile.mkdtemp())
        (root / "windows").mkdir()
        (root / "macos").mkdir()
        (root / "windows" / "EnUES_win_v3.3.7.0132_SDK.exe").write_bytes(b"x")
        dest = root / "out" / "manifest.json"
        expected = build_manifest(root)
        got = write_manifest(root, dest)
        self.assertEqual(got, expected)
        self.assertTrue(dest.is_file())
        self.assertEqual(json.loads(dest.read_text(encoding="utf-8")), expected)

    def test_missing_windows_client_is_null(self):
        root = Path(tempfile.mkdtemp())
        (root / "windows").mkdir()
        (root / "macos").mkdir()
        (root / "windows" / "EnUES_win_v3.3.7.0132_SDK.exe").write_bytes(b"x")
        (root / "macos" / "EnUESBOX_mac_v3.3.7.0114_SDK.pkg").write_bytes(b"x")
        (root / "macos" / "GenlotVPN-macos-x86_64-1.0.1.zip").write_bytes(b"x")
        m = build_manifest(root)
        self.assertIsNone(m["windows"]["client"])
        self.assertEqual(m["windows"]["sdk"]["version"], "3.3.7.0132")
        self.assertEqual(m["windows"]["sdk"]["file"], "windows/EnUES_win_v3.3.7.0132_SDK.exe")
        self.assertEqual(m["macos"]["client"]["version"], "1.0.1")
        self.assertEqual(m["macos"]["client"]["file"], "macos/GenlotVPN-macos-x86_64-1.0.1.zip")

    def test_ignore_unpacked_app_dir(self):
        root = Path(tempfile.mkdtemp())
        (root / "macos").mkdir()
        unpacked = root / "macos" / "GenlotVPN-macos-x86_64-1.0.0"
        unpacked.mkdir()
        (unpacked / "GenlotVPN.app").mkdir()
        (root / "macos" / "GenlotVPN-macos-x86_64-1.0.1.zip").write_bytes(b"x")
        m = build_manifest(root)
        self.assertEqual(m["macos"]["client"]["version"], "1.0.1")


if __name__ == "__main__":
    unittest.main()
