(() => {
  let host = null;
  let blocked = false;
  let last = Date.now();
  let acc = 0;

  function fmt(ms) {
    const s = Math.max(0, Math.round(ms / 1000));
    const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), r = s % 60;
    return (h ? h + "h " : "") + (h || m ? String(m).padStart(h ? 2 : 1, "0") + "m " : "") + String(r).padStart(2, "0") + "s";
  }

  function pauseMedia() {
    document.querySelectorAll("video,audio").forEach((m) => { try { m.pause(); } catch (e) {} });
  }

  function showOverlay(st) {
    if (!host || !host.isConnected) {
      host = document.createElement("div");
      host.style.cssText = "all:initial;position:fixed;inset:0;z-index:2147483647;";
      const root = host.attachShadow({ mode: "closed" });
      root.innerHTML = `
        <div style="position:fixed;inset:0;background:#0b0b10;color:#fff;font-family:system-ui,sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:24px">
          <div style="font-size:28px;font-weight:700;margin-bottom:8px" id="t"></div>
          <div style="font-size:16px;opacity:.75;margin-bottom:20px" id="r"></div>
          <div style="font-size:44px;font-weight:700;font-variant-numeric:tabular-nums" id="c"></div>
        </div>`;
      host._refs = { t: root.getElementById("t"), r: root.getElementById("r"), c: root.getElementById("c") };
      (document.documentElement).appendChild(host);
    }
    host._until = st.until;
    host._refs.t.textContent = st.reason === "daily" ? "Daily limit reached" : "Session over — cooldown";
    host._refs.r.textContent = st.reason === "daily" ? "Back tomorrow at midnight." : "You can come back in:";
    host._refs.c.textContent = fmt(st.until - Date.now());
    pauseMedia();
  }

  function hideOverlay() {
    if (host) { host.remove(); host = null; }
  }

  function apply(st) {
    if (!st) return;
    blocked = st.blocked;
    if (st.blocked) showOverlay(st); else hideOverlay();
  }

  function send(msg) {
    try {
      return chrome.runtime.sendMessage(msg).then(apply).catch(() => {});
    } catch (e) { /* extension reloaded */ }
  }

  setInterval(() => {
    const now = Date.now();
    const d = now - last;
    last = now;
    if (blocked) {
      if (host && host._refs) {
        host._refs.c.textContent = fmt(host._until - now);
        pauseMedia();
        if (!host.isConnected) document.documentElement.appendChild(host);
      }
      return;
    }
    if (document.visibilityState === "visible") acc += d;
  }, 1000);

  setInterval(() => {
    if (blocked) { send({ type: "status" }); return; }
    const delta = acc;
    acc = 0;
    send({ type: delta > 0 ? "tick" : "status", delta });
  }, 2000);

  send({ type: "status" });
})();
