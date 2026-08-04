#!/usr/bin/env swift
/**
 与 Windows genlot-app.ico 对齐的 macOS 图标。
 直接使用 assets/images/genlot-app-icon-official.png（与 exe/托盘同源），
 放入 Big Sur 风格透明边距 + 黑底圆角方块。
 */
import AppKit
import Foundation

let canvasSize: CGFloat = 1024
let bodySize: CGFloat = 824
let bodyRadius: CGFloat = 184

func makeBitmap(width: Int, height: Int) -> NSBitmapImageRep {
    guard let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil,
        pixelsWide: width,
        pixelsHigh: height,
        bitsPerSample: 8,
        samplesPerPixel: 4,
        hasAlpha: true,
        isPlanar: false,
        colorSpaceName: .deviceRGB,
        bytesPerRow: 0,
        bitsPerPixel: 0
    ) else {
        fatalError("无法创建位图")
    }
    rep.size = NSSize(width: width, height: height)
    return rep
}

func writePNG(_ rep: NSBitmapImageRep, to url: URL) {
    guard let data = rep.representation(using: .png, properties: [:]) else {
        fatalError("无法写出 PNG")
    }
    try! data.write(to: url, options: .atomic)
}

func drawIcon(into rep: NSBitmapImageRep, official: NSImage) {
    NSGraphicsContext.saveGraphicsState()
    guard let ctx = NSGraphicsContext(bitmapImageRep: rep) else {
        fatalError("无法创建绘图上下文")
    }
    NSGraphicsContext.current = ctx
    ctx.imageInterpolation = .high
    defer { NSGraphicsContext.restoreGraphicsState() }

    let size = CGFloat(rep.pixelsWide)
    let scale = size / canvasSize

    NSColor.clear.setFill()
    NSRect(x: 0, y: 0, width: size, height: size).fill()

    let offset = (size - bodySize * scale) / 2
    let bodyRect = NSRect(x: offset, y: offset, width: bodySize * scale, height: bodySize * scale)
    let bodyPath = NSBezierPath(roundedRect: bodyRect, xRadius: bodyRadius * scale, yRadius: bodyRadius * scale)
    NSColor.black.setFill()
    bodyPath.fill()

    // official 已是黑底+标志+GENLOT，铺满本体并略留边，避免贴边
    let pad = 28 * scale
    let dest = bodyRect.insetBy(dx: pad, dy: pad)
    bodyPath.setClip()
    let srcRect = NSRect(origin: .zero, size: official.size)
    official.draw(in: dest, from: srcRect, operation: .sourceOver, fraction: 1.0)
}

func resizePNG(from src: URL, size: Int, to dst: URL) {
    let rep = makeBitmap(width: size, height: size)
    NSGraphicsContext.saveGraphicsState()
    let ctx = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.current = ctx
    ctx.imageInterpolation = .high
    let srcImage = NSImage(contentsOf: src)!
    let srcRect = NSRect(origin: .zero, size: srcImage.size)
    srcImage.draw(in: NSRect(x: 0, y: 0, width: size, height: size),
                  from: srcRect, operation: .copy, fraction: 1.0)
    NSGraphicsContext.restoreGraphicsState()
    writePNG(rep, to: dst)
}

let scriptPath = URL(fileURLWithPath: CommandLine.arguments[0]).standardizedFileURL
let root = scriptPath.deletingLastPathComponent().deletingLastPathComponent()
let imagesDir = root.appendingPathComponent("assets/images")

let officialURL = imagesDir.appendingPathComponent("genlot-app-icon-official.png")
guard let official = NSImage(contentsOf: officialURL) else {
    fputs("缺少 \(officialURL.path)\n", stderr)
    exit(1)
}

let pngURL = imagesDir.appendingPathComponent("genlot-app-1024.png")
let icnsURL = imagesDir.appendingPathComponent("genlot-app.icns")
let iconsetURL = imagesDir.appendingPathComponent("genlot-app.iconset")

let master = makeBitmap(width: Int(canvasSize), height: Int(canvasSize))
drawIcon(into: master, official: official)
writePNG(master, to: pngURL)
print("[OK] \(pngURL.path)  (from \(officialURL.lastPathComponent))")

let fm = FileManager.default
try? fm.removeItem(at: iconsetURL)
try! fm.createDirectory(at: iconsetURL, withIntermediateDirectories: true)

let entries: [(String, Int)] = [
    ("icon_16x16.png", 16),
    ("icon_16x16@2x.png", 32),
    ("icon_32x32.png", 32),
    ("icon_32x32@2x.png", 64),
    ("icon_128x128.png", 128),
    ("icon_128x128@2x.png", 256),
    ("icon_256x256.png", 256),
    ("icon_256x256@2x.png", 512),
    ("icon_512x512.png", 512),
    ("icon_512x512@2x.png", 1024),
]
for (name, size) in entries {
    resizePNG(from: pngURL, size: size, to: iconsetURL.appendingPathComponent(name))
}

let proc = Process()
proc.executableURL = URL(fileURLWithPath: "/usr/bin/iconutil")
proc.arguments = ["-c", "icns", iconsetURL.path, "-o", icnsURL.path]
try! proc.run()
proc.waitUntilExit()
if proc.terminationStatus != 0 {
    fputs("iconutil 失败\n", stderr)
    exit(1)
}
try? fm.removeItem(at: iconsetURL)
print("[OK] \(icnsURL.lastPathComponent)")

let app = root.appendingPathComponent("build-macos/GenlotVPN.app")
let appIcns = app.appendingPathComponent("Contents/Resources/genlot-app.icns")
if fm.fileExists(atPath: app.path) {
    try! fm.createDirectory(at: appIcns.deletingLastPathComponent(), withIntermediateDirectories: true)
    try? fm.removeItem(at: appIcns)
    try! fm.copyItem(at: icnsURL, to: appIcns)
    try? fm.setAttributes([.modificationDate: Date()], ofItemAtPath: app.path)
    print("[OK] 已同步到 \(app.path)")
}
