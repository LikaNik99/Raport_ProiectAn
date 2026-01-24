(() => {
  "use strict";

  const keys = { ENTER: 13, ESC: 27, SPACE: 32 };
  let isOpen = false;
  let queue = [];
  let activeElement = null;

  const labels = {
    ok: "OK",
    cancel: "Cancel",
  };

  let delay = 5000;

  /* ===========================
     UTILS
     =========================== */

  const $ = (id) => document.getElementById(id);

  const supportsTransition = () => {
    const el = document.createElement("div");
    return "transition" in el.style;
  };

  /* ===========================
     INIT DOM
     =========================== */

  function init() {
    if (!$("alertify-cover")) {
      const cover = document.createElement("div");
      cover.id = "alertify-cover";
      cover.className = "alertify-cover alertify-cover-hidden";
      document.body.appendChild(cover);
    }

    if (!$("alertify")) {
      const dialog = document.createElement("section");
      dialog.id = "alertify";
      dialog.className = "alertify alertify-hidden";
      document.body.appendChild(dialog);
    }

    if (!$("alertify-logs")) {
      const logs = document.createElement("section");
      logs.id = "alertify-logs";
      logs.className = "alertify-logs alertify-logs-hidden";
      document.body.appendChild(logs);
    }

    document.body.tabIndex = 0;
  }

  /* ===========================
     DIALOG BUILDER
     =========================== */

  function buildDialog({ type, message }) {
    const buttons =
      type === "alert"
        ? `<button id="alertify-ok" class="alertify-button alertify-button-ok">${labels.ok}</button>`
        : `
          <button id="alertify-cancel" class="alertify-button alertify-button-cancel">${labels.cancel}</button>
          <button id="alertify-ok" class="alertify-button alertify-button-ok">${labels.ok}</button>
        `;

    return `
      <div class="alertify-dialog">
        <article class="alertify-inner">
          <p class="alertify-message">${message}</p>
          <nav class="alertify-buttons">${buttons}</nav>
        </article>
      </div>
    `;
  }

  /* ===========================
     SHOW / HIDE
     =========================== */

  function show() {
    if (queue.length === 0) return;

    const item = queue[0];
    const dialog = $("alertify");
    const cover = $("alertify-cover");

    activeElement = document.activeElement;

    dialog.innerHTML = buildDialog(item);
    dialog.className = "alertify";
    cover.className = "alertify-cover";

    isOpen = true;

    const ok = $("alertify-ok");
    const cancel = $("alertify-cancel");

    ok?.focus();

    ok?.addEventListener("click", () => close(true));
    cancel?.addEventListener("click", () => close(false));

    document.addEventListener("keyup", keyHandler);
  }

  function close(result) {
    const dialog = $("alertify");
    const cover = $("alertify-cover");

    queue.shift();

    dialog.className = "alertify alertify-hidden";
    cover.className = "alertify-cover alertify-cover-hidden";

    document.removeEventListener("keyup", keyHandler);

    activeElement?.focus();
    isOpen = false;

    if (queue.length > 0) show();
  }

  function keyHandler(e) {
    if (e.keyCode === keys.ESC) close(false);
    if (e.keyCode === keys.ENTER) close(true);
  }

  /* ===========================
     LOGS
     =========================== */

  function notify(message, type = "", wait = delay) {
    init();

    const logs = $("alertify-logs");
    logs.className = "alertify-logs";

    const log = document.createElement("article");
    log.className = `alertify-log ${type ? "alertify-log-" + type : ""}`;
    log.innerHTML = message;

    logs.appendChild(log);

    setTimeout(() => log.classList.add("alertify-log-show"), 20);

    if (wait !== 0) {
      setTimeout(() => {
        log.classList.add("alertify-log-hide");
        setTimeout(() => log.remove(), 500);
      }, wait);
    }
  }

  /* ===========================
     PUBLIC API
     =========================== */

  window.alertify = {
    alert(message) {
      init();
      queue.push({ type: "alert", message });
      if (!isOpen) show();
      return this;
    },

    confirm(message, fn) {
      init();
      queue.push({ type: "confirm", message, fn });
      if (!isOpen) show();
      return this;
    },

    success(message, wait) {
      notify(message, "success", wait);
      return this;
    },

    error(message, wait) {
      notify(message, "error", wait);
      return this;
    },

    info(message, wait) {
      notify(message, "info", wait);
      return this;
    },
    set(options) {
      if (typeof options.delay === "number") delay = options.delay;
    },
  };
})();
