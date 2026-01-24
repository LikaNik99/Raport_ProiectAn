const express = require("express");
const http = require("http");
const { Server } = require("socket.io");
const fs = require("fs");
const path = require("path");
const cookieParser = require("cookie-parser");
const crypto = require("crypto");

// === NEW: MongoDB (users in Mongo) ===
require('dotenv').config();
const mongoose = require('mongoose');

// Conectare Mongo
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/taskdb';
mongoose.set('strictQuery', true);
mongoose.connect(MONGODB_URI)
  .then(() => console.log('✅ MongoDB connected'))
  .catch((e) => { console.error('Mongo error:', e); process.exit(1); });

// Model User (parole în clar pentru simplitate; poți trece ulterior la bcrypt)
const UserSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true },
  email: { type: String, unique: true, required: true, lowercase: true, trim: true, index: true },
  password: { type: String, default: '' }, // DEV: parole în clar (exact ca în user.json)
  role: { type: String, enum: ['admin', 'user'], default: 'user' },
  notifications: { type: [mongoose.Schema.Types.Mixed], default: [] },
  avatar: { type: String }
}, { timestamps: true });
const User = mongoose.model('User', UserSchema);

// Seed admin dacă lipsește
async function ensureAdmin() {
  const anyAdmin = await User.findOne({ role: 'admin' }).lean();
  if (!anyAdmin) {
    await User.create({
      name: "admin",
      email: "admin@admin.com",
      password: "admin123",
      role: "admin",
      notifications: []
    });
    console.log('👑 Admin seed: admin@admin.com / admin123');
  }
}
ensureAdmin().catch(console.error);

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

app.use(express.json());
app.use(cookieParser());

// ===============================
// Sesiuni simple (cookie "sid")
// ===============================
const SESSIONS = new Map(); // sid -> { user: {email,name,role}, expiresAt }
const SESSION_TTL = 7 * 24 * 60 * 60 * 1000; // 7 zile

function createSession(user) {
  const sid = crypto.randomBytes(24).toString("hex");
  const expiresAt = Date.now() + SESSION_TTL;
  SESSIONS.set(sid, { user: { email: user.email, name: user.name, role: user.role }, expiresAt });
  return sid;
}
function getSession(sid) {
  if (!sid) return null;
  const s = SESSIONS.get(sid);
  if (!s) return null;
  if (s.expiresAt < Date.now()) { SESSIONS.delete(sid); return null; }
  return s;
}
function parseCookies(header) {
  const out = {};
  if (!header) return out;
  header.split(";").forEach((p) => {
    const [k, ...v] = p.trim().split("=");
    out[k] = decodeURIComponent(v.join("="));
  });
  return out;
}

// === Helpers users (Mongo) ===
async function getUserByEmail(email) {
  if (!email) return null;
  return await User.findOne({ email: String(email).toLowerCase() });
}
async function getAdmins() {
  return await User.find({ role: 'admin' });
}

// ===============================
// BOARD (board.json) — tasks + projects
// ===============================
let board = { tasks: [], projects: [] };
const TASKS_FILE = path.join(__dirname, "board.json");

if (fs.existsSync(TASKS_FILE)) {
  try {
    board = JSON.parse(fs.readFileSync(TASKS_FILE, "utf8"));
    if (!Array.isArray(board.tasks)) board.tasks = [];
    if (!Array.isArray(board.projects)) board.projects = [];
  } catch {
    board = { tasks: [], projects: [] };
  }
} else {
  fs.writeFileSync(TASKS_FILE, JSON.stringify(board, null, 2));
}

// Helpers pentru status
function deriveStatus(t) {
  if (!t) return "todo";
  if (t.completed) return "done";
  if (t.status && ["todo","in_progress","done"].includes(String(t.status))) {
    if (t.status === "done" && !t.completed) return "in_progress";
    return t.status === "done" ? "done" : t.status;
  }
  if (t.claimedByEmail || t.claimedBy) return "in_progress";
  return "todo";
}
function ensureTaskStatus(t) {
  if (!t) return false;
  const prev = t.status;
  const next = deriveStatus(t);
  if (prev !== next) {
    t.status = next;
    return true;
  }
  return false;
}

// MIGRARE status
(function migrateStatuses(){
  let changed = false;
  (board.tasks || []).forEach(t => {
    if (t && t.completed) {
      if (t.status !== "done") { t.status = "done"; changed = true; }
    } else {
      const ok = ensureTaskStatus(t);
      if (ok) changed = true;
    }
  });
  if (changed) saveBoard();
})();
function saveBoard() {
  try { fs.writeFileSync(TASKS_FILE, JSON.stringify(board, null, 2)); }
  catch (e) { console.error("⚠️ Eroare la salvarea board.json:", e); }
}

// ===============================
// STATIC + Pagini (ORDINEA CONTEAZĂ)
// ===============================
app.use("/formular", express.static(path.join(__dirname, "Formular de inregistrare")));

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "Formular de inregistrare", "register.html"));
});

app.get("/index.html", (req, res) => {
  const sid = req.cookies?.sid;
  const s = getSession(sid);
  if (!s) return res.redirect("/");
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.use(express.static(path.join(__dirname, "public"), { index: false }));

// ===============================
// AUTH (register/login/logout) — users din Mongo
// ===============================
app.post("/register", async (req, res) => {
  try {
    const { name, email, password } = req.body || {};
    if (!name || !email || !password) {
      return res.status(400).json({ success: false, message: "Completează toate câmpurile!" });
    }
    const emailNorm = String(email).toLowerCase();
    const exists = await User.findOne({ email: emailNorm });
    if (exists) {
      return res.status(400).json({ success: false, message: "Email deja folosit!" });
    }
    await User.create({ name, email: emailNorm, password, role: "user", notifications: [] });
    res.json({ success: true, message: "Înregistrare reușită!" });
  } catch (e) {
    if (e?.code === 11000) {
      return res.status(400).json({ success: false, message: "Email deja folosit!" });
    }
    console.error(e);
    res.status(500).json({ success: false, message: "Eroare server" });
  }
});

app.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body || {};
    const u = await User.findOne({ email: String(email || '').toLowerCase() });
    if (!u || (u.password || '') !== password) {
      return res.status(401).json({ success: false, message: "Date incorecte!" });
    }
    const sid = createSession({ email: u.email, name: u.name, role: u.role });
    res.cookie("sid", sid, { httpOnly: true, sameSite: "lax", secure: false, maxAge: SESSION_TTL });
    res.json({ success: true, redirect: "/index.html", username: u.name, role: u.role, email: u.email });
  } catch (e) {
    console.error(e);
    res.status(500).json({ success: false, message: "Eroare server" });
  }
});

app.post("/logout", (req, res) => {
  const sid = req.cookies?.sid;
  if (sid) {
    SESSIONS.delete(sid);
    res.clearCookie("sid", { httpOnly: true, sameSite: "lax", secure: false });
  }
  res.json({ success: true });
});

// ===============================
// Socket.IO — autentificare + mapare socket-uri per user
// ===============================
const USER_SOCKETS = new Map(); // emailLower -> Set(socketId)
function attachSocket(email, socketId) {
  const key = String(email || "").toLowerCase();
  if (!key) return;
  if (!USER_SOCKETS.has(key)) USER_SOCKETS.set(key, new Set());
  USER_SOCKETS.get(key).add(socketId);
}
function detachSocket(email, socketId) {
  const key = String(email || "").toLowerCase();
  const set = USER_SOCKETS.get(key);
  if (!set) return;
  set.delete(socketId);
  if (set.size === 0) USER_SOCKETS.delete(key);
}
function sendToUser(email, event, payload) {
  const key = String(email || "").toLowerCase();
  const set = USER_SOCKETS.get(key);
  if (!set) return;
  for (const sid of set) io.to(sid).emit(event, payload);
}

io.use((socket, next) => {
  const cookies = parseCookies(socket.handshake.headers.cookie || "");
  const sid = cookies.sid;
  const session = getSession(sid);
  if (!session) return next(new Error("unauthorized"));
  socket.data.user = session.user; // { email, name, role }
  next();
});

// ===============================
// SOCKET.IO — tasks + projects + rating + notificări + chat
// ===============================
io.on("connection", (socket) => {
  const me = socket.data.user;
  attachSocket(me?.email, socket.id);
  console.log("✅ Conectat:", socket.id, me?.email);

  socket.emit("me:info", me);

  // ===== NOTIFICĂRI: listă, readAll, clearAll =====
  socket.on("notify:list:request", async () => {
    const u = await getUserByEmail(me?.email);
    const list = Array.isArray(u?.notifications) ? u.notifications : [];
    socket.emit("notify:bulk", list);
  });
  socket.on("notify:readAll", async () => {
    const u = await getUserByEmail(me?.email);
    if (u) {
      u.notifications = (Array.isArray(u.notifications) ? u.notifications : []).map(n => ({ ...n, read: true }));
      await u.save();
      socket.emit("notify:bulk", u.notifications);
    }
  });
  socket.on("notify:clearAll", async () => {
    const u = await getUserByEmail(me?.email);
    if (u) {
      u.notifications = [];
      await u.save();
      socket.emit("notify:bulk", u.notifications);
    }
  });

  // ===== TASKS =====
  socket.on("tasks:request", () => {
    let changed = false;
    board.tasks.forEach(t => { if (ensureTaskStatus(t)) changed = true; });
    if (changed) saveBoard();
    socket.emit("tasks:init", board.tasks);
  });

  // Creează task — doar admin
  socket.on("task:create", (payload) => {
    if (me?.role !== "admin") {
      return socket.emit("error:permission", "Doar adminul poate adăuga taskuri!");
    }
    const task = payload?.task ?? payload ?? {};
    if (!task.id) task.id = Date.now().toString();
    if (typeof task.completed === "undefined") task.completed = false;
    if (!task.hasOwnProperty("claimedBy")) task.claimedBy = null;
    task.status = task.completed ? "done" : "todo";
    board.tasks.push(task);
    saveBoard();
    io.emit("task:created", task);
  });

  // Închide task (Done)
  socket.on("task:close", async (id) => {
    const t = board.tasks.find(tt => tt.id === id);
    if (!t) return socket.emit("error:validation", "Task inexistent");
    if (t.completed) return;

    if (t.claimedByEmail && me.role !== "admin" && t.claimedByEmail !== me.email) {
      return socket.emit("error:permission", "Doar persoana care a preluat taskul (sau adminul) îl poate închide.");
    }

    t.completed = true;
    t.status = "done";
    t.closedBy = me?.email || "unknown";
    t.closedAt = new Date().toISOString();

    saveBoard();
    io.emit("task:updated", t);
    io.emit("task:status", { id: t.id, status: t.status, claimedBy: t.claimedBy, claimedByEmail: t.claimedByEmail });

    // Notifică TOȚI ADMINII
    if (me?.role !== "admin") {
      const notif = {
        id: Date.now().toString(),
        type: "closed",
        taskId: t.id,
        taskTitle: t.title || "Task",
        closedBy: t.closedBy,
        createdAt: new Date().toISOString(),
        read: false
      };
      const admins = await getAdmins();
      for (const u of admins) {
        u.notifications = Array.isArray(u.notifications) ? u.notifications : [];
        u.notifications.push(notif);
        await u.save();
        sendToUser(u.email, "notify:new", notif);
      }
    }
  });

  // Update task (conținut) + protecții
  socket.on("task:update", (updatedTask) => {
    const idx = board.tasks.findIndex(t => t.id === updatedTask?.id);
    if (idx === -1) return socket.emit("error:validation", "Task inexistent");
    const current = board.tasks[idx];

    if (current.completed === true && updatedTask?.completed === false) {
      const { completed, ...rest } = updatedTask || {};
      board.tasks[idx] = { ...current, ...rest };
      ensureTaskStatus(board.tasks[idx]);
      saveBoard();
      return io.emit("task:updated", board.tasks[idx]);
    }

    if (me?.role === "admin") {
      board.tasks[idx] = { ...current, ...updatedTask };
      if (board.tasks[idx].completed) board.tasks[idx].status = "done";
      else ensureTaskStatus(board.tasks[idx]);
      saveBoard();
      return io.emit("task:updated", board.tasks[idx]);
    }

    if (updatedTask?.completed === true && current.completed !== true) {
      if (current.claimedByEmail && current.claimedByEmail !== me.email) {
        return socket.emit("error:permission", "Doar persoana care a preluat taskul îl poate închide.");
      }
      current.completed = true;
      current.status = "done";
      current.closedBy = me?.email || "unknown";
      current.closedAt = new Date().toISOString();
      saveBoard();
      io.emit("task:updated", current);
      io.emit("task:status", { id: current.id, status: current.status, claimedBy: current.claimedBy, claimedByEmail: current.claimedByEmail });
      return;
    }

    return socket.emit("error:permission", "Doar adminul poate edita taskuri (poți doar să le închizi).");
  });

  // NEW: Status switch (To Do / In Progress) — realtime sync
  socket.on("task:status", (payload) => {
    try {
      const { id, status, claimedBy, claimedByEmail } = payload || {};
      if (!id || !status) return;
      const idx = board.tasks.findIndex(t => t.id === id);
      if (idx === -1) return socket.emit("error:validation", "Task inexistent");
      const t = board.tasks[idx];

      if (status === "done") {
        return;
      }
      if (t.completed) {
        io.to(socket.id).emit("task:status", { id: t.id, status: "done", claimedBy: t.claimedBy, claimedByEmail: t.claimedByEmail });
        return;
      }

      const isClaimedByOther = t.claimedByEmail && t.claimedByEmail !== me.email && me.role !== "admin";
      if (isClaimedByOther) {
        io.to(socket.id).emit("task:status", { id: t.id, status: deriveStatus(t), claimedBy: t.claimedBy, claimedByEmail: t.claimedByEmail });
        return;
      }

      if (status === "in_progress") {
        t.status = "in_progress";
        t.completed = false;
        if (!t.claimedByEmail) {
          t.claimedBy = { name: me?.name || me?.email || "User" };
          t.claimedByEmail = me?.email || null;
        } else {
          if (typeof claimedBy !== "undefined") t.claimedBy = claimedBy;
          if (typeof claimedByEmail !== "undefined") t.claimedByEmail = claimedByEmail;
        }
      } else if (status === "todo") {
        t.status = "todo";
        t.completed = false;
        if (typeof claimedBy !== "undefined") t.claimedBy = claimedBy;
        if (typeof claimedByEmail !== "undefined") t.claimedByEmail = claimedByEmail;
      } else {
        return;
      }

      saveBoard();
      io.emit("task:status", { id: t.id, status: t.status, claimedBy: t.claimedBy, claimedByEmail: t.claimedByEmail });
      io.emit("task:updated", t);
    } catch (e) {
      console.error("task:status error", e);
    }
  });

  // Claim / Release
  socket.on("task:claim", ({ id, claimedBy }) => {
    const idx = board.tasks.findIndex(t => t.id === id);
    if (idx === -1) return;
    const t = board.tasks[idx];

    if (!claimedBy) {
      if (t.claimedByEmail && t.claimedByEmail !== me.email && me.role !== "admin") {
        return socket.emit("error:permission", "Nu poți elibera un task preluat de altcineva.");
      }
      t.claimedBy = null;
      t.claimedByEmail = null;
      if (!t.completed) t.status = "todo";
      saveBoard();
      io.emit("task:updated", t);
      io.emit("task:status", { id: t.id, status: t.status, claimedBy: t.claimedBy, claimedByEmail: t.claimedByEmail });
      return;
    }

    if (t.claimedByEmail && t.claimedByEmail !== me.email && me.role !== "admin") {
      return socket.emit("error:permission", "Taskul a fost deja preluat de altcineva.");
    }
    t.claimedBy = { name: me?.name || claimedBy?.name || me?.email || "User" };
    t.claimedByEmail = me?.email || null;
    if (!t.completed) t.status = "in_progress";
    saveBoard();
    io.emit("task:updated", t);
    io.emit("task:status", { id: t.id, status: t.status, claimedBy: t.claimedBy, claimedByEmail: t.claimedByEmail });
  });

  // Ștergere task — doar admin
  socket.on("task:delete", (idOrPayload) => {
    if (me?.role !== "admin") {
      return socket.emit("error:permission", "Doar adminul poate șterge taskuri!");
    }
    const id = typeof idOrPayload === "object" ? idOrPayload.id : idOrPayload;
    board.tasks = board.tasks.filter(t => t.id !== id);
    saveBoard();
    io.emit("task:deleted", id);
  });

  // ===== PROJECTS =====
  socket.on("projects:request", () => {
    socket.emit("projects:init", board.projects);
  });
  socket.on("project:create", ({ name, color }) => {
    if (me?.role !== "admin") {
      return socket.emit("error:permission", "Doar adminul poate adăuga proiecte!");
    }
    const project = {
      id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
      name: String(name || "New Project").trim(),
      color: color || "#4e73df"
    };
    board.projects.push(project);
    saveBoard();
    io.emit("project:created", project);
  });
  socket.on("project:delete", (id) => {
    if (me?.role !== "admin") {
      return socket.emit("error:permission", "Doar adminul poate șterge proiecte!");
    }
    const before = board.projects.length;
    board.projects = board.projects.filter(p => p.id !== id);
    if (board.projects.length !== before) {
      board.tasks.forEach(t => { if (t.project === id) t.project = null; });
      saveBoard();
      io.emit("project:deleted", id);
    }
  });

  // ===== RATING (admin) + notificare către userul care a închis =====
  socket.on("task:rate", async ({ id, stars, comment }) => {
    if (me?.role !== "admin") {
      return socket.emit("error:permission", "Doar adminul poate da rating.");
    }
    const idx = board.tasks.findIndex(t => t.id === id);
    if (idx === -1) return socket.emit("error:validation", "Task inexistent");
    const t = board.tasks[idx];
    if (!t.completed) return socket.emit("error:validation", "Poți nota doar task-uri închise.");

    if (typeof t.rating === "number") {
      return socket.emit("error:validation", "Acest task a fost deja notat.");
    }

    const s = Math.max(1, Math.min(5, parseInt(stars || 0, 10)));
    const fb = String(comment || "").slice(0, 1000);

    t.rating = s;
    t.feedback = fb;
    t.ratedBy = me?.email || "admin";
    t.ratedByName = me?.name || "Admin";
    t.ratedByRole = me?.role || "admin";
    t.ratedAt = new Date().toISOString();

    saveBoard();
    io.emit("task:updated", t);

    // 1) Curăță notificările "closed" la toți adminii
    const admins = await getAdmins();
    for (const u of admins) {
      u.notifications = Array.isArray(u.notifications) ? u.notifications : [];
      const before = u.notifications.length;
      u.notifications = u.notifications.filter(n => !(n && n.type === "closed" && n.taskId === t.id));
      if (u.notifications.length !== before) {
        await u.save();
        sendToUser(u.email, "notify:bulk", u.notifications);
      }
    }

    // 2) Notifică userul care a închis taskul
    const recipient = await getUserByEmail(t.closedBy);
    if (recipient) {
      const notif = {
        id: Date.now().toString(),
        type: "rating",
        taskId: t.id,
        taskTitle: t.title || "Task",
        stars: s,
        comment: fb,
        ratedBy: t.ratedBy,
        ratedByName: t.ratedByName,
        ratedByRole: t.ratedByRole,
        createdAt: new Date().toISOString(),
        read: false
      };
      recipient.notifications = Array.isArray(recipient.notifications) ? recipient.notifications : [];
      recipient.notifications.push(notif);
      await recipient.save();
      sendToUser(recipient.email, "notify:new", notif);
    }
  });

  // ===== CHAT =====
  socket.on("chat:message", (msg) => {
    const time = new Date().toLocaleTimeString("ro-RO", { hour: "2-digit", minute: "2-digit" });
    const message = {
      id: Date.now(),
      user: msg.user || me?.name || "Anonim",
      text: msg.text,
      time
    };
    io.emit("chat:message", message);
  });

  socket.on("disconnect", () => {
    detachSocket(me?.email, socket.id);
    console.log("❌ Deconectat:", socket.id);
  });
});

// ===============================
// PORNIRE SERVER
// ===============================
const PORT = process.env.PORT || 3000;
server.listen(PORT, "0.0.0.0", () => {
  console.log(`🚀 Server pornit pe portul ${PORT}`);
  console.log(`➡️  Login page: http://localhost:${PORT}/`);
});