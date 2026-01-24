document.addEventListener("DOMContentLoaded", () => {
  const badges = document.querySelectorAll(".inbox-badge");
  let currentCount = 0;

  window.INBOX.listeners.push((data) => {
    if (typeof data.unread_count !== "number") return;

    currentCount = data.unread_count;
    render();
  });

  function render() {
    badges.forEach(badge => {
      if (currentCount > 0) {
        badge.textContent = currentCount;
        badge.classList.add("is-active");
      } else {
        badge.textContent = "0";
        badge.classList.remove("is-active");
      }
    });
  }
});
