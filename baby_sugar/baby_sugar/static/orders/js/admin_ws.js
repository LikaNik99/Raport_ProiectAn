document.addEventListener("DOMContentLoaded", () => {
  console.log("🟢 ADMIN WS JS LOADED");


  const modal = document.getElementById("admin-modal");
  const titleEl = document.getElementById("admin-modal-title");
  const messageEl = document.getElementById("admin-modal-message");
  const okBtn = document.getElementById("admin-modal-ok");

  // 🔒 siguranță DOM (admin base)
  if (!modal || !titleEl || !messageEl || !okBtn) {
    console.warn("⚠️ Admin modal elements lipsesc");
    return;
  }

  let ws;

  try {
    ws = new WebSocket(
      (location.protocol === "https:" ? "wss://" : "ws://") +
      location.host +
      "/ws/admin/"
    );
  } catch (err) {
    console.warn("⚠️ Admin WS nu poate fi inițializat");
    return;
  }

  ws.onopen = () => {
    console.log("🟢 Admin WS conectat");
  };

  ws.onmessage = (e) => {
    const data = JSON.parse(e.data);
    console.log("📩 Admin WS:", data);

    notifyUserSound({
    direction: "receive",
    is_new: true,
    inboxOpen: false
    });

    titleEl.textContent = "Comandă nouă";
    messageEl.innerHTML = `
      <strong>Comandă #${data.id}</strong><br>
      Client: ${data.user}<br>
      Status: ${data.status}
    `;

    if (!modal.open) {
      modal.showModal();
    }
  };

  okBtn.addEventListener("click", (e) => {
    e.preventDefault();
    modal.close();
    window.location.href = "/admin/orders/order/";
  });

  const adminForm = document.querySelector("form#order_form");

  if (adminForm) {
    adminForm.addEventListener("submit", (e) => {
      e.preventDefault(); // ⛔ stop submit imediat

      // 🔔 1. sunet SEND
      notifyUserSound({ direction: "send" });

      // ⏱️ 2. mic delay ca browserul să redea sunetul
      setTimeout(() => {
        adminForm.submit(); // 📤 trimitem formularul
      }, 200); // 100–150ms este perfect
    });
  }



  // ⚠️ NU arătăm panică – doar informativ
  ws.onerror = () => {
    console.warn("⚠️ Admin WS indisponibil (neautorizat sau offline)");
  };

  ws.onclose = (e) => {
    if (e.code === 1000 || e.code === 1006) {
      console.info("ℹ️ Admin WS închis normal");
    } else {
      console.warn("⚠️ Admin WS închis:", e.code);
    }
  };
});
