// Shared budget across twitter/x, tiktok, instagram.
const DEFAULTS = { sessionMin: 20, cooldownMin: 60, dailyMin: 120 };
const MIN = 60000;

let chain = Promise.resolve();
function locked(fn) {
  const p = chain.then(fn, fn);
  chain = p.catch(() => {});
  return p;
}

function dayKey(t) {
  const d = new Date(t);
  return d.getFullYear() + "-" + (d.getMonth() + 1) + "-" + d.getDate();
}
function nextMidnight(t) {
  const d = new Date(t);
  d.setHours(24, 0, 0, 0);
  return d.getTime();
}

async function load() {
  const { settings, state } = await chrome.storage.local.get(["settings", "state"]);
  return { settings: { ...DEFAULTS, ...(settings || {}) }, state: state || {} };
}

function normalize(settings, st, now) {
  const s = { day: dayKey(now), dailyMs: 0, countedUntil: 0, sessionStart: 0, ...st };
  if (s.day !== dayKey(now)) { s.day = dayKey(now); s.dailyMs = 0; }
  // Claude-style window: starts at first use, runs on the wall clock, then cooldown, then fresh.
  if (s.sessionStart && now >= s.sessionStart + (settings.sessionMin + settings.cooldownMin) * MIN) s.sessionStart = 0;
  return s;
}

function status(settings, s, now) {
  const dailyLimit = settings.dailyMin * MIN;
  const out = {
    blocked: false, reason: null, until: 0,
    sessionLeft: s.sessionStart ? Math.max(0, s.sessionStart + settings.sessionMin * MIN - now) : settings.sessionMin * MIN,
    dailyLeft: Math.max(0, dailyLimit - s.dailyMs),
  };
  if (s.dailyMs >= dailyLimit) {
    out.blocked = true; out.reason = "daily"; out.until = nextMidnight(now);
  } else if (s.sessionStart && now >= s.sessionStart + settings.sessionMin * MIN) {
    out.blocked = true; out.reason = "cooldown";
    out.until = s.sessionStart + (settings.sessionMin + settings.cooldownMin) * MIN;
  }
  return out;
}

async function handle(deltaMs) {
  const now = Date.now();
  const { settings, state } = await load();
  const s = normalize(settings, state, now);
  let st = status(settings, s, now);
  if (deltaMs > 0 && !st.blocked) {
    const d = Math.min(deltaMs, 15000);
    const from = Math.max(now - d, s.countedUntil);
    const add = Math.max(0, now - from);
    s.countedUntil = now;
    if (add > 0) {
      if (!s.sessionStart) s.sessionStart = from;
      s.dailyMs += add;
    }
    st = status(settings, s, now);
  }
  await chrome.storage.local.set({ state: s });
  return st;
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg && (msg.type === "tick" || msg.type === "status")) {
    locked(() => handle(msg.type === "tick" ? msg.delta : 0)).then(sendResponse);
    return true;
  }
});

chrome.runtime.onInstalled.addListener(async () => {
  const { settings } = await chrome.storage.local.get("settings");
  if (!settings) await chrome.storage.local.set({ settings: DEFAULTS });
});
