# 字体

本目录用于放置 **PT Sans** 拉丁文 woff2（如 `PTSans-Regular.woff2`）。

- 拉丁字母：预置 PT Sans（若本目录有 woff2，在 `css/app.css` 用 `@font-face` 引用）。
- 中文：不内嵌中文字体文件，使用系统栈 `PingFang SC` / `Microsoft YaHei`。

页面 `font-family`：

```css
font-family: "PT Sans", "PingFang SC", "Microsoft YaHei", sans-serif;
```

本期未入库 woff2：公开源需运行时请求 Google Fonts / 第三方 CDN，与 spec「不请求 Google Fonts」冲突；本地预置需单独授权文件。实现时未方便获得可入库的拉丁 woff2，故 CSS 只声明上述字体栈，有 PT Sans 的环境会用，否则回退到 PingFang SC / Microsoft YaHei / sans-serif。

请勿在运行时从 genlot.com 或 fonts.googleapis.com 拉字体。
