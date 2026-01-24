/* =========================
   🔊 SUNETE GLOBALE
   ========================= */
window.sounds = {
  receive: new Audio("/static/sounds/receive.mp3"),
  send: new Audio("/static/sounds/send.mp3"),
};

window.sounds.receive.volume = 1.0;
window.sounds.send.volume = 1.0;


/* =========================
   🔓 UNLOCK AUDIO
   ========================= */
let audioUnlocked = false;

function unlockAudioOnce() {
  if (audioUnlocked) return;

  Object.values(window.sounds).forEach(sound => {
    sound.play().then(() => {
      sound.pause();
      sound.currentTime = 0;
    }).catch(() => {});
  });

  audioUnlocked = true;
  console.log("🔓 Audio unlocked");
}

document.addEventListener("click", unlockAudioOnce, { once: true });


/* =========================
   🔎 HELPERS
   ========================= */
function isSoundEnabled() {
  return localStorage.getItem("notif_sound") === "on";
}

function isVibrateEnabled() {
  return localStorage.getItem("notif_vibrate") === "on";
}


/* =========================
   🔔 FUNCȚIA CENTRALĂ
   ========================= */
function notifyUserSound({ direction = "receive", is_new = false, inboxOpen = false }) {
  if (!window.sounds) return;

  if (direction === "receive") {
    if (!is_new) return;
    if (inboxOpen && !document.hidden) return;

    if (isVibrateEnabled()) {
      navigator.vibrate?.(150);
    }

    if (isSoundEnabled()) {
      window.sounds.receive.currentTime = 0;
      window.sounds.receive.play().catch(() => {});
    }
  }

  if (direction === "send") {
    if (isSoundEnabled()) {
      window.sounds.send.currentTime = 0;
      window.sounds.send.play().catch(() => {});
    }
  }
}



window.addEventListener("load", () => {
  const consent = document.getElementById("notif-consent");
  if (!consent) return;

  const sound = localStorage.getItem("notif_sound");

  console.log("🔍 notif_sound =", sound);

  if (sound === null) {
    consent.style.display = "block";
    console.log("🔔 Consent banner shown");
  } else {
    consent.style.display = "none";
    console.log("✅ Consent already chosen, banner hidden");
  }

  document.getElementById("notif-yes")?.addEventListener("click", () => {
    localStorage.setItem("notif_sound", "on");
    localStorage.setItem("notif_vibrate", "on");
    consent.style.display = "none";
    unlockAudioOnce();
  });

  document.getElementById("notif-no")?.addEventListener("click", () => {
    localStorage.setItem("notif_sound", "off");
    localStorage.setItem("notif_vibrate", "off");
    consent.style.display = "none";
  });
});
