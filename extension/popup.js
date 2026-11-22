const m = (ms) => Math.ceil(ms / 60000) + " min";
document.getElementById("o").onclick = () => chrome.runtime.openOptionsPage();
chrome.runtime.sendMessage({ type: "status" }).then((st) => {
  if (!st) return;
  if (st.blocked) {
    document.getElementById("a").textContent = st.reason === "daily" ? "Daily limit reached." : "Cooling down.";
    document.getElementById("b").textContent = "Back at " + new Date(st.until).toLocaleTimeString();
  } else {
    document.getElementById("a").textContent = "Session left: " + m(Math.min(st.sessionLeft, st.dailyLeft));
    document.getElementById("b").textContent = "Today left: " + m(st.dailyLeft);
  }
});
