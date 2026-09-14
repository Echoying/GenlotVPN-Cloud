(function () {
  var KEY_P = "gv-platform";
  var KEY_L = "gv-lang";

  function detectPlatform() {
    var s = sessionStorage.getItem(KEY_P);
    if (s === "windows" || s === "macos") return s;
    var ua = navigator.userAgent || "";
    return /Mac|iPhone|iPad|iPod/.test(ua) ? "macos" : "windows";
  }

  function detectLang() {
    var s = sessionStorage.getItem(KEY_L);
    if (s === "zh" || s === "en") return s;
    var lang = (navigator.language || "en").toLowerCase();
    return lang.indexOf("zh") === 0 ? "zh" : "en";
  }

  function isManual() {
    return /\/manual\/?$/.test(location.pathname) || location.pathname.indexOf("/manual/") !== -1;
  }

  var state = { platform: detectPlatform(), lang: detectLang(), manifest: null, fail: false };

  function t(key) {
    return window.GV_I18N[state.lang][key];
  }

  function setPlatform(p) {
    state.platform = p;
    sessionStorage.setItem(KEY_P, p);
    render();
  }

  function setLang(l) {
    state.lang = l;
    sessionStorage.setItem(KEY_L, l);
    render();
  }

  function fileName(rel) {
    if (!rel) return "";
    var i = rel.lastIndexOf("/");
    return i >= 0 ? rel.slice(i + 1) : rel;
  }

  function cardHtml(kind, entry) {
    var name = kind === "sdk" ? t("sdkName") : t("clientName");
    var kicker = kind === "sdk" ? t("sdkKicker") : t("clientKicker");
    if (!entry) {
      return (
        '<article class="card"><span class="card-kicker">' + kicker + "</span><h3>" + name +
        "</h3><p class=\"muted\">—</p>" +
        '<button type="button" class="btn" disabled>' + t("unavailable") + "</button></article>"
      );
    }
    var href = "/genlotvpn/download/files/" + entry.file;
    return (
      '<article class="card"><span class="card-kicker">' + kicker + "</span><h3>" + name + "</h3>" +
      "<p>" + fileName(entry.file) + "</p>" +
      '<p class="ver">' + (entry.version || "") + "</p>" +
      '<a class="btn" href="' + href + '">' + t("download") + "</a></article>"
    );
  }

  function render() {
    document.title = t("title");
    document.documentElement.lang = state.lang === "zh" ? "zh-CN" : "en";
    document.getElementById("site-title").textContent = t("title");
    document.getElementById("hero-title").textContent = t("title");
    document.getElementById("hero-sub").textContent = t("heroSub");
    document.getElementById("nav-download").textContent = t("navDownload");
    document.getElementById("nav-manual").textContent = t("navManual");
    document.getElementById("nav-download").className = isManual() ? "" : "current";
    document.getElementById("nav-manual").className = isManual() ? "current" : "";
    document.getElementById("lang-toggle").value = state.lang;
    document.getElementById("btn-win").className = state.platform === "windows" ? "on" : "";
    document.getElementById("btn-mac").className = state.platform === "macos" ? "on" : "";
    document.getElementById("page-download").hidden = isManual();
    document.getElementById("page-manual").hidden = !isManual();
    document.getElementById("footer").textContent = t("footer");

    var box = document.getElementById("cards");
    if (state.fail) {
      box.innerHTML = "<p class=\"error\">" + t("manifestFail") + "</p>";
    } else if (state.manifest) {
      var plat = state.manifest[state.platform] || {};
      box.innerHTML = cardHtml("sdk", plat.sdk) + cardHtml("client", plat.client);
    }

    var platKey = state.platform === "macos" ? "macos" : "windows";
    var client = state.manifest && state.manifest[platKey] && state.manifest[platKey].client;
    var head = document.getElementById("manual-head");
    head.textContent = client && client.version
      ? t("currentClient").replace("{version}", client.version)
      : "";
    document.getElementById("guide-win").textContent = t("winGuide");
    document.getElementById("guide-mac").textContent = t("macGuide");
    document.getElementById("guide-win").className = state.platform === "windows" ? "on" : "";
    document.getElementById("guide-mac").className = state.platform === "macos" ? "on" : "";
    var steps = state.platform === "macos" ? t("macSteps") : t("winSteps");
    document.getElementById("steps").innerHTML = steps.map(function (s) {
      return "<li>" + s + "</li>";
    }).join("");
  }

  function bind() {
    document.getElementById("lang-toggle").onchange = function () {
      setLang(this.value);
    };
    document.getElementById("btn-win").onclick = function () { setPlatform("windows"); };
    document.getElementById("btn-mac").onclick = function () { setPlatform("macos"); };
    document.getElementById("guide-win").onclick = function () { setPlatform("windows"); };
    document.getElementById("guide-mac").onclick = function () { setPlatform("macos"); };
    document.getElementById("nav-download").setAttribute("href", "/genlotvpn/download/");
    document.getElementById("nav-manual").setAttribute("href", "/genlotvpn/manual/");
  }

  bind();
  render();
  fetch("/genlotvpn/download/manifest.json", { cache: "no-store" })
    .then(function (r) {
      if (!r.ok) throw new Error("bad");
      return r.json();
    })
    .then(function (j) {
      state.manifest = j;
      state.fail = false;
      render();
    })
    .catch(function () {
      state.fail = true;
      render();
    });
})();
