window.INBOX = {
  socket: null,
  listeners: []
};

document.addEventListener("DOMContentLoaded", () => {

  const socket = new WebSocket(
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/inbox/"
  );

  window.INBOX.socket = socket;

  const inboxContainer = document.querySelector('[data-inbox="true"]');
  const inboxOpen = !!inboxContainer;

  socket.onopen = () => {
    console.log("🟢 Inbox WS conectat");
  };

  socket.onmessage = (e) => {
    const data = JSON.parse(e.data);
    console.log("📨 WS NOTIFICATION:", data);

    /* =========================
       🔑 1. INIT (DB → badge)
       ========================= */
    if (data.type === "init") {
      window.INBOX.listeners.forEach(fn =>
        fn({ unread_count: data.unread_count })
      );
      return;
    }

      // 🔔 Redă sunet DOAR la mesaj nou
    if (data.type === "notification") {
      notifyUserSound(
        {
          direction:"receive",
          is_new:true,
          inboxOpen:inboxOpen
        }
      );
    }

    /* =========================
       🔔 2. LIVE → badge
       ========================= */
    if (typeof data.unread_count === "number") {
      window.INBOX.listeners.forEach(fn => fn(data));
    }
    console.log("UNREAD?", data.unread_count);
    // if (!list) return;
    // if (!data.id) return;
    if (data.type !== "notification") return;
    if (!inboxOpen) return;
    /* =========================
       📥 3. INBOX DOM (codul tău)
       ========================= */

    if (document.querySelector(`[data-id="${data.id}"]`)) {
      console.log("⚠️ Notificare deja existentă, ignorată");
      return;
    }

    const item = document.createElement("div");
    item.className = "list-group-item";
    item.dataset.id = data.id;

    const time = data.created_at
      ? new Date(data.created_at).toLocaleString("ro-RO", {
          day: "2-digit",
          month: "2-digit",
          year: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        })
      : "acum";

    item.innerHTML = `
      <div class="d-flex justify-content-between align-items-start">
        <div class="d-flex gap-3">
          <input type="checkbox" name="ids" value="${data.id}">
          <div>
            <a href="/user/inbox/notifications/${data.id}/" class="text-decoration-none">
              <span class="fw-bold">${data.title}</span>
            </a>
            <p class="mb-1 text-muted">${data.message}</p>
            <small class="text-secondary">
              ${time} · de la ${data.sender || "sistem"}
            </small>
          </div>
        </div>
      </div>
    `;

    inboxContainer.prepend(item);
  };

  socket.onclose = () => {
    console.warn("🔴 Inbox WS închis");
    setTimeout(() => location.reload(), 2000);
  };
});




