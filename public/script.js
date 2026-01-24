// Conectare la server WebSocket
const socket = io();

// Add task button
document.getElementById("addTaskBtn").addEventListener("click", () => {
  const task = { title: "New task", status: "todo" };
  socket.emit("task:create", task);
});

// Ascultăm dacă serverul trimite task nou
socket.on("task:created", (task) => {
  renderTask(task);
  updateCounts();
});

function renderTask(task) {
  const taskDiv = document.createElement("div");
  taskDiv.classList.add("task");
  taskDiv.textContent = task.title;
  document.getElementById(task.status).appendChild(taskDiv);
}

// Actualizează dashboard simplu
function updateCounts() {
  document.getElementById("todoCount").textContent =
    document.getElementById("todo").childNodes.length;
  document.getElementById("progressCount").textContent =
    document.getElementById("inprogress").childNodes.length;
  document.getElementById("doneCount").textContent =
    document.getElementById("done").childNodes.length;
}

// Chat simplu
const chatInput = document.getElementById("chatInput");
chatInput.addEventListener("keypress", (e) => {
  if (e.key === "Enter" && chatInput.value.trim() !== "") {
    socket.emit("chat:message", chatInput.value);
    chatInput.value = "";
  }
});
socket.on("chat:message", (msg) => {
  const div = document.createElement("div");
  div.textContent = msg;
  document.getElementById("messages").appendChild(div);
});