// "Your turn" alert: chime + vibration + tab title, once, when the ticket becomes called.
(function () {
  const originalTitle = document.title;
  let wasCalled = null;   // null = haven't looked yet
  let audioCtx = null;

  const btn = document.getElementById("sound-btn");
  const titleText = document.getElementById("turn-title").textContent.trim();

  function chime() {
    if (!audioCtx) return;
    const t0 = audioCtx.currentTime;
    [660, 880].forEach(function (freq, i) {
      const start = t0 + i * 0.25;
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = "sine";
      osc.frequency.value = freq;
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(0.4, start + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.5);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start(start);
      osc.stop(start + 0.55);
    });
  }

  function vibrate() {
    if (navigator.vibrate) navigator.vibrate([300, 150, 300]);   // not supported on iPhone
  }

  function check() {
    const called = !!document.querySelector(".pg-ticket--called");

    if (wasCalled === null) {            // first look: set the baseline, no alert
      wasCalled = called;
      if (called) document.title = titleText;
      return;
    }
    if (called === wasCalled) return;    // no change: do nothing
    wasCalled = called;

    if (called) {
      document.title = titleText;
      chime();
      vibrate();
    } else {
      document.title = originalTitle;
    }
  }

  if (btn) {
    btn.addEventListener("click", function () {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return;
      audioCtx = audioCtx || new AC();
      audioCtx.resume();                 // this tap unlocks sound
      chime();                           // plays once so the resident hears it works
      vibrate();
      btn.textContent = document.getElementById("sound-done").textContent;
      btn.disabled = true;
    });
  }

  document.body.addEventListener("htmx:load", check);   // runs after every refresh
  document.addEventListener("DOMContentLoaded", check);
})();
