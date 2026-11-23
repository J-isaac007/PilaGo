// TV board: flash and announce each newly called number, once.
(function () {
  let lastNumber = null;
  let firstCheck = true;   // first look at the page = don't announce
  let soundOn = false;

  const btn = document.getElementById("sound-btn");

  function speak(text) {
    if (!("speechSynthesis" in window)) return;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "en-US";
    u.rate = 0.9;
    window.speechSynthesis.speak(u);
  }

  // "C-003" -> "C 0 0 3" so the voice reads it character by character
  function spellOut(number) {
    return number.replace(/[^A-Za-z0-9]/g, "").split("").join(" ");
  }

  function check() {
    const panel = document.querySelector(".tv-serving");
    if (!panel) return;

    const hero = panel.querySelector(".tv-hero:not(.tv-hero--empty)");
    const number = hero ? hero.textContent.trim() : null;
    const counterEl = panel.querySelector(".tv-counter-no");
    const counter = counterEl ? counterEl.textContent.trim() : "";
    
    if (firstCheck) { firstCheck = false; lastNumber = number; return; }
    if (number === lastNumber) return;
    lastNumber = number;
    if (!number) return;                              // board went empty

    hero.classList.add("tv-hero--flash");
    if (soundOn) speak("Now serving " + spellOut(number) + ", Counter " + counter);
  }

  btn.addEventListener("click", function () {
    soundOn = true;
    btn.textContent = document.getElementById("sound-done").textContent;
    btn.classList.add("tv-sound--on");
    speak(" ");   // this click is the user gesture that unlocks speech
  });

  document.body.addEventListener("htmx:load", check);   // runs after every swap
  document.addEventListener("DOMContentLoaded", check);
})();
