const D = { sessionMin: 20, cooldownMin: 60, dailyMin: 120 };
const ids = { sessionMin: "s", cooldownMin: "c", dailyMin: "d" };
chrome.storage.local.get("settings").then(({ settings }) => {
  const v = { ...D, ...(settings || {}) };
  for (const k in ids) document.getElementById(ids[k]).value = v[k];
});
document.getElementById("save").onclick = async () => {
  const settings = {};
  for (const k in ids) {
    const n = Math.round(Number(document.getElementById(ids[k]).value));
    settings[k] = n >= 1 ? n : D[k];
    document.getElementById(ids[k]).value = settings[k];
  }
  await chrome.storage.local.set({ settings });
  const m = document.getElementById("msg");
  m.textContent = "Saved";
  setTimeout(() => (m.textContent = ""), 1500);
};
