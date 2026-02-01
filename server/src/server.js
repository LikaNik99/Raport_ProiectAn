import 'dotenv/config';
import express from 'express';
import http from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import multer from 'multer';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import Database from 'better-sqlite3';
import fs from 'fs';
import OpenAI from 'openai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'secret-dev-change-me';
const MAX_UPLOAD_MB = Number(process.env.MAX_UPLOAD_MB || 30);

const AI_PROVIDER = process.env.AI_PROVIDER || 'openrouter';
const OPENROUTER_API_KEY = process.env.OPENROUTER_API_KEY || '';
const AI_MODEL = process.env.AI_MODEL || 'deepseek/deepseek-chat-v3.1:free';
const AI_FALLBACK_MODEL = process.env.AI_FALLBACK_MODEL || 'deepseek/deepseek-chat';
const AI_SYSTEM_PROMPT = process.env.AI_SYSTEM_PROMPT || 'Ești un asistent prietenos care răspunde concis în limba română.';
const AI_ENABLED = (AI_PROVIDER === 'openrouter' && !!OPENROUTER_API_KEY);

// Admin env
const ADMIN_USERNAME = process.env.ADMIN_USERNAME || '';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || '';
const ADMIN_FORCE_RESET = (process.env.ADMIN_FORCE_RESET || '0') === '1';

// Global "General" conversation
const GENERAL_CONV_NAME = process.env.GENERAL_CONV_NAME || 'General';

const openai = AI_ENABLED ? new OpenAI({
  apiKey: OPENROUTER_API_KEY,
  baseURL: 'https://openrouter.ai/api/v1',
  defaultHeaders: {
    'HTTP-Referer': process.env.OPENROUTER_SITE_URL || 'http://localhost:3000',
    'X-Title': process.env.OPENROUTER_APP_NAME || 'Chat App'
  }
}) : null;

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: process.env.CORS_ORIGIN || '*' } });

app.use(cors());
app.use(express.json({ limit: '10mb' }));

const UPLOAD_DIR = path.join(__dirname, '..', 'uploads');
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });

// Data directory for SQLite database (supports Docker volumes)
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '..');
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

app.use('/uploads', express.static(UPLOAD_DIR, { maxAge: '7d', etag: true }));
app.use(express.static(path.join(__dirname, '../../client')));

const db = new Database(path.join(DATA_DIR, 'chat.db'));
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  avatar_url TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  role TEXT DEFAULT 'user'
);
CREATE TABLE IF NOT EXISTS conversations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  type TEXT CHECK(type IN ('direct', 'group')) NOT NULL,
  name TEXT,
  created_by INTEGER,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_announcement INTEGER DEFAULT 0
);
CREATE TABLE IF NOT EXISTS conversation_members (
  conversation_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  role TEXT DEFAULT 'member',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (conversation_id, user_id)
);
CREATE TABLE IF NOT EXISTS messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  conversation_id INTEGER NOT NULL,
  sender_id INTEGER NOT NULL,
  content TEXT,
  type TEXT DEFAULT 'text',
  file_url TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  edited_at DATETIME,
  is_deleted INTEGER DEFAULT 0,
  deleted_at DATETIME
);
CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id, created_at DESC);

CREATE TABLE IF NOT EXISTS message_reactions (
  message_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  emoji TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id, user_id, emoji)
);
CREATE TABLE IF NOT EXISTS cleared_conversations (
  conversation_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  cleared_at DATETIME NOT NULL,
  PRIMARY KEY (conversation_id, user_id)
);
CREATE TABLE IF NOT EXISTS hidden_conversations (
  conversation_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  hidden_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (conversation_id, user_id)
);
CREATE TABLE IF NOT EXISTS message_hidden (
  message_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  hidden_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id, user_id)
);
`);

// Indexuri suplimentare
db.exec(`
CREATE INDEX IF NOT EXISTS idx_cm_user ON conversation_members(user_id);
CREATE INDEX IF NOT EXISTS idx_cm_conv ON conversation_members(conversation_id);
CREATE INDEX IF NOT EXISTS idx_hidden_conv_user ON hidden_conversations(conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_cleared_conv_user ON cleared_conversations(conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_hidden_msg_user ON message_hidden(message_id, user_id);
CREATE INDEX IF NOT EXISTS idx_react_msg ON message_reactions(message_id);
CREATE INDEX IF NOT EXISTS idx_react_msg_user ON message_reactions(message_id, user_id);
CREATE INDEX IF NOT EXISTS idx_react_msg_emoji ON message_reactions(message_id, emoji);
`);

// Migration: adaugă coloana last_seen dacă nu există
try {
  db.exec(`ALTER TABLE users ADD COLUMN last_seen DATETIME`);
  console.log('Added last_seen column to users table.');
} catch (e) {
  // Coloana există deja - ignorăm eroarea
}

// ═══════════════════════════════════════════════════════════════════════════
//                          🟢 ONLINE USERS TRACKING
// ═══════════════════════════════════════════════════════════════════════════
// Map: userId -> Set of socket IDs (un user poate avea mai multe taburi deschise)
const onlineUsers = new Map();

// AUTO-FIX: Corectăm tipul conversațiilor existente la pornire
try {
  const result = db.prepare(`
    UPDATE conversations 
    SET type = 'group' 
    WHERE (type != 'group' OR type IS NULL)
      AND is_announcement = 0 
      AND id IN (
        SELECT conversation_id 
        FROM conversation_members 
        GROUP BY conversation_id 
        HAVING COUNT(*) > 2
      )
  `).run();
  if (result.changes > 0) {
    console.log(`Auto-fixed ${result.changes} conversations to GROUP type.`);
  }
} catch (err) {
  console.error("Auto-fix error:", err);
}

const toUserDTO = (u, includeOnline = false) => ({
  id: u.id,
  username: u.username,
  avatarUrl: u.avatar_url || null,
  createdAt: u.created_at,
  role: u.role || 'user',
  lastSeen: u.last_seen || null,
  isOnline: includeOnline ? onlineUsers.has(u.id) : undefined
});
const toMessageDTO = (m, sender) => ({
  id: m.id,
  conversationId: m.conversation_id,
  senderId: m.sender_id,
  content: m.content,
  type: m.type,
  fileUrl: m.file_url,
  createdAt: m.created_at,
  editedAt: m.edited_at || null,
  isDeleted: !!m.is_deleted,
  sender: sender ? toUserDTO(sender) : undefined,
  reactions: m.reactions || []
});

function signToken(user) {
  return jwt.sign({ sub: user.id, username: user.username, role: user.role || 'user' }, JWT_SECRET, { expiresIn: '7d' });
}
function authMiddleware(req, res, next) {
  try {
    const hdr = req.headers.authorization || '';
    const token = hdr.startsWith('Bearer ') ? hdr.slice(7) : null;
    if (!token) return res.status(401).json({ error: 'Neautorizat' });
    const payload = jwt.verify(token, JWT_SECRET);
    req.user = { id: payload.sub, username: payload.username, role: payload.role || 'user' };
    next();
  } catch {
    return res.status(401).json({ error: 'Token invalid' });
  }
}
function adminOnly(req, res, next) {
  if (req.user?.role !== 'admin') return res.status(403).json({ error: 'Doar administratorul are acces' });
  next();
}

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, UPLOAD_DIR),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname);
    const base = path.basename(file.originalname, ext).slice(0, 50).replace(/\s+/g, '_');
    cb(null, `${Date.now()}_${Math.random().toString(36).slice(2, 8)}_${base}${ext}`);
  }
});
const upload = multer({ storage, limits: { fileSize: MAX_UPLOAD_MB * 1024 * 1024 } });

/* ============ AI helpers ============ */
function ensureAIUser() {
  let ai = db.prepare('SELECT * FROM users WHERE username = ?').get('ai_assistant');
  if (!ai) {
    const hash = bcrypt.hashSync('ai:' + Math.random().toString(36).slice(2), 10);
    const avatar = 'https://api.dicebear.com/7.x/bottts-neutral/svg?seed=AI';
    const info = db.prepare('INSERT INTO users (username, password_hash, avatar_url, role) VALUES (?, ?, ?, ?)').run('ai_assistant', hash, avatar, 'user');
    ai = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  }
  return ai;
}
function ensureAdminUser() {
  if (!ADMIN_USERNAME || !ADMIN_PASSWORD) return null;
  let admin = db.prepare('SELECT * FROM users WHERE username = ?').get(ADMIN_USERNAME);
  const newHash = bcrypt.hashSync(ADMIN_PASSWORD, 10);
  const avatar = 'https://api.dicebear.com/7.x/identicon/svg?seed=' + encodeURIComponent('admin');
  if (!admin) {
    const info = db.prepare('INSERT INTO users (username, password_hash, avatar_url, role) VALUES (?, ?, ?, ?)').run(ADMIN_USERNAME, newHash, avatar, 'admin');
    admin = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  } else {
    if (admin.role !== 'admin') db.prepare('UPDATE users SET role = ? WHERE id = ?').run('admin', admin.id);
    if (ADMIN_FORCE_RESET) db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(newHash, admin.id);
  }
  return admin;
}
function getOrCreateAIConversationForUser(userId) {
  const ai = ensureAIUser();
  let convId = findDirectConversation(userId, ai.id);
  if (!convId) convId = createDirectConversation(userId, ai.id);
  return convId;
}
function isAIConversation(convId) {
  const ai = ensureAIUser();
  return !!db.prepare('SELECT 1 FROM conversation_members WHERE conversation_id = ? AND user_id = ?').get(convId, ai.id);
}
function getAIHistory(convId, limit = 20) {
  const rows = db.prepare(`
    SELECT * FROM messages WHERE conversation_id = ? AND is_deleted = 0
    ORDER BY created_at DESC LIMIT ?
  `).all(convId, limit).reverse();
  const ai = ensureAIUser();
  const messages = [{ role: 'system', content: AI_SYSTEM_PROMPT }];
  for (const m of rows) {
    const role = (m.sender_id === ai.id) ? 'assistant' : 'user';
    const content = m.type === 'text' ? (m.content || '') :
      (m.type === 'file' ? `Utilizatorul a trimis un fișier: ${m.file_url || m.content || ''}` : (m.content || ''));
    messages.push({ role, content });
  }
  return messages;
}
async function chatOnce(model, messages) {
  const completion = await openai.chat.completions.create({ model, messages, temperature: 0.7 });
  const text = completion?.choices?.[0]?.message?.content?.trim() || '';
  if (!text) throw new Error('Răspuns AI gol');
  return text;
}
async function aiComplete(history) {
  if (!AI_ENABLED || !openai) throw new Error('AI dezactivat (OPENROUTER_API_KEY lipsă)');
  const candidates = Array.from(new Set([
    AI_MODEL,
    'deepseek/deepseek-chat-v3.1:free',
    'deepseek/deepseek-chat',
    AI_FALLBACK_MODEL,
    'deepseek/deepseek-r1:free'
  ])).filter(Boolean);
  const messages = history.map(h => ({ role: h.role, content: h.content }));
  let lastErr = null;
  for (const model of candidates) {
    try { return await chatOnce(model, messages); }
    catch (e) {
      lastErr = e;
      const msg = String(e?.message || '').toLowerCase();
      const retriable = /not\s*found|invalid model|unsupported|permission|forbidden|404|403/.test(msg);
      if (!retriable) break;
    }
  }
  throw lastErr || new Error('AI necunoscut error');
}

/* ============ Helpers conversații ============ */
function getConversationMembers(convId) {
  const rows = db.prepare(`
    SELECT u.* FROM conversation_members cm
    JOIN users u ON u.id = cm.user_id
    WHERE cm.conversation_id = ?
    ORDER BY u.username
  `).all(convId);
  return rows.map(toUserDTO);
}
function getConversationById(convId) { return db.prepare('SELECT * FROM conversations WHERE id = ?').get(convId); }
function userIsMember(convId, userId) { return !!db.prepare('SELECT 1 FROM conversation_members WHERE conversation_id = ? AND user_id = ?').get(convId, userId); }
function findDirectConversation(a, b) {
  const rows = db.prepare(`
    SELECT c.id FROM conversations c
    WHERE c.type = 'direct' AND EXISTS (SELECT 1 FROM conversation_members cm WHERE cm.conversation_id = c.id AND cm.user_id = ?)
      AND EXISTS (SELECT 1 FROM conversation_members cm2 WHERE cm2.conversation_id = c.id AND cm2.user_id = ?)
  `).all(a, b);
  return rows.length ? rows[0].id : null;
}
function createDirectConversation(a, b) {
  const info = db.prepare(`INSERT INTO conversations (type, is_announcement) VALUES ('direct', 0)`).run();
  const convId = info.lastInsertRowid;
  const ins = db.prepare('INSERT INTO conversation_members (conversation_id, user_id, role) VALUES (?, ?, ?)');
  ins.run(convId, a, 'member'); ins.run(convId, b, 'member');
  return convId;
}
function createGroupConversation(name, creatorId, memberIds) {
  const info = db.prepare(`INSERT INTO conversations (type, name, created_by, is_announcement) VALUES ('group', ?, ?, 0)`).run(name, creatorId);
  const convId = info.lastInsertRowid;
  const insert = db.prepare('INSERT INTO conversation_members (conversation_id, user_id, role) VALUES (?, ?, ?)');
  const all = Array.from(new Set([creatorId, ...memberIds]));
  for (const uid of all) insert.run(convId, uid, uid === creatorId ? 'admin' : 'member');
  return convId;
}
function createAnnouncementConversation(name, creatorId, memberIds) {
  const info = db.prepare(`INSERT INTO conversations (type, name, created_by, is_announcement) VALUES ('group', ?, ?, 1)`).run(name, creatorId);
  const convId = info.lastInsertRowid;
  const insert = db.prepare('INSERT INTO conversation_members (conversation_id, user_id, role) VALUES (?, ?, ?)');
  const all = Array.from(new Set([creatorId, ...(memberIds || [])]));
  for (const uid of all) insert.run(convId, uid, uid === creatorId ? 'admin' : 'member');
  return convId;
}

/* Global "General" conversation helpers */
function ensureGeneralConversation() {
  const creator = ensureAdminUser() || ensureAIUser();
  let conv = db.prepare(`
    SELECT * FROM conversations
    WHERE type = 'group'
      AND is_announcement = 0
      AND LOWER(name) = LOWER(?)
  `).get(GENERAL_CONV_NAME);

  if (!conv) {
    const info = db.prepare(`
      INSERT INTO conversations (type, name, created_by, is_announcement)
      VALUES ('group', ?, ?, 0)
    `).run(GENERAL_CONV_NAME, creator?.id || null);

    conv = db.prepare('SELECT * FROM conversations WHERE id = ?').get(info.lastInsertRowid);

    if (creator) {
      db.prepare(`
        INSERT OR IGNORE INTO conversation_members (conversation_id, user_id, role)
        VALUES (?, ?, ?)
      `).run(conv.id, creator.id, 'admin');
    }
  }
  return conv;
}
function ensureGeneralMembership(userId) {
  const conv = ensureGeneralConversation();
  const exists = db.prepare(`
    SELECT 1 FROM conversation_members
    WHERE conversation_id = ? AND user_id = ?
  `).get(conv.id, userId);

  if (!exists) {
    db.prepare(`
      INSERT INTO conversation_members (conversation_id, user_id, role)
      VALUES (?, ?, 'member')
    `).run(conv.id, userId);
  }
  return conv.id;
}

function buildConversationPayload(convId) {
  const conv = getConversationById(convId);
  if (!conv) return null;
  const last = db.prepare(`SELECT * FROM messages WHERE conversation_id = ? AND is_deleted = 0 ORDER BY created_at DESC LIMIT 1`).get(convId);
  const lastDto = last ? toMessageDTO(last) : null;
  return {
    id: conv.id,
    type: conv.type,
    name: conv.name,
    createdAt: conv.created_at,
    createdBy: conv.created_by || null,
    isAnnouncement: !!conv.is_announcement,
    members: getConversationMembers(conv.id),
    lastMessage: lastDto
  };
}
function getUserConversations(userId, userRole = 'user') {
  // Asigură-te că fiecare utilizator este membru în "General"
  ensureGeneralMembership(userId);

  const isAdminUser = userRole === 'admin';

  let convs;
  if (isAdminUser) {
    // Admin vede TOATE conversațiile din sistem
    convs = db.prepare(`SELECT c.* FROM conversations c`).all();
  } else {
    // Utilizatorii normali văd doar conversațiile în care sunt membri
    convs = db.prepare(`
      SELECT c.* FROM conversations c
      JOIN conversation_members cm ON cm.conversation_id = c.id AND cm.user_id = ?
      WHERE NOT EXISTS (SELECT 1 FROM hidden_conversations h WHERE h.conversation_id = c.id AND h.user_id = ?)
    `).all(userId, userId);
  }

  const lastMsgStmt = db.prepare(`
    SELECT * FROM messages
    WHERE conversation_id = ? AND is_deleted = 0
    ORDER BY created_at DESC
    LIMIT 1
  `);
  const mapped = convs.map(c => {
    const members = getConversationMembers(c.id);
    const lastMessage = lastMsgStmt.get(c.id);
    return {
      id: c.id, type: c.type, name: c.name, createdAt: c.created_at,
      createdBy: c.created_by || null, isAnnouncement: !!c.is_announcement,
      members, lastMessage: lastMessage ? toMessageDTO(lastMessage) : null,
      // Pentru admin: marchează conversațiile în care NU e membru (doar observă)
      isObserving: isAdminUser && !members.some(m => m.id === userId)
    };
  });

  const generalNameLc = GENERAL_CONV_NAME.toLowerCase();

  mapped.sort((a, b) => {
    const aIsGeneral = a.type === 'group'
      && !a.isAnnouncement
      && (a.name || '').toLowerCase() === generalNameLc;
    const bIsGeneral = b.type === 'group'
      && !b.isAnnouncement
      && (b.name || '').toLowerCase() === generalNameLc;

    if (aIsGeneral && !bIsGeneral) return -1;
    if (!aIsGeneral && bIsGeneral) return 1;

    const ta = new Date(a.lastMessage?.createdAt || a.createdAt).getTime();
    const tb = new Date(b.lastMessage?.createdAt || b.createdAt).getTime();
    return tb - ta;
  });
  return mapped;
}

// Hard delete conversație: acum curățăm și message_hidden (în plus față de mesaje/reacții/membri)
function hardDeleteConversation(convId) {
  const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(convId);
  db.prepare('DELETE FROM message_reactions WHERE message_id IN (SELECT id FROM messages WHERE conversation_id = ?)').run(convId);
  db.prepare('DELETE FROM message_hidden WHERE message_id IN (SELECT id FROM messages WHERE conversation_id = ?)').run(convId);
  db.prepare('DELETE FROM messages WHERE conversation_id = ?').run(convId);
  db.prepare('DELETE FROM conversation_members WHERE conversation_id = ?').run(convId);
  db.prepare('DELETE FROM hidden_conversations WHERE conversation_id = ?').run(convId);
  db.prepare('DELETE FROM cleared_conversations WHERE conversation_id = ?').run(convId);
  db.prepare('DELETE FROM conversations WHERE id = ?').run(convId);
  for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:deleted', { conversationId: convId });
}

/* ============ Bootstrap AI + Admin + General ============ */
ensureAIUser();
ensureAdminUser();
ensureGeneralConversation();

/* ============ API ============ */
app.post('/api/auth/register', async (req, res) => {
  try {
    const { username, password } = req.body || {};
    if (!username || !password) return res.status(400).json({ error: 'username și password sunt obligatorii' });
    if (username.length < 3) return res.status(400).json({ error: 'username prea scurt' });
    if (password.length < 6) return res.status(400).json({ error: 'parola trebuie să aibă minim 6 caractere' });
    const exists = db.prepare('SELECT id FROM users WHERE username = ?').get(username);
    if (exists) return res.status(409).json({ error: 'username deja folosit' });
    const hash = await bcrypt.hash(password, 10);
    const info = db.prepare('INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)').run(username, hash, 'user');
    const user = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);

    // auto-join în canalul General
    ensureGeneralMembership(user.id);

    const token = signToken(user);
    res.json({ token, user: toUserDTO(user) });
  } catch (e) { console.error(e); res.status(500).json({ error: 'Eroare server' }); }
});
app.post('/api/auth/login', async (req, res) => {
  try {
    const { username, password } = req.body || {};
    const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);
    if (!user) return res.status(401).json({ error: 'Credentiale invalide' });
    const ok = await bcrypt.compare(password, user.password_hash);
    if (!ok) return res.status(401).json({ error: 'Credentiale invalide' });

    // asigură membership în General și pentru utilizatorii vechi
    ensureGeneralMembership(user.id);

    const token = signToken(user);
    res.json({ token, user: toUserDTO(user) });
  } catch (e) { console.error(e); res.status(500).json({ error: 'Eroare server' }); }
});
app.get('/api/me', authMiddleware, (req, res) => {
  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
  res.json({ user: toUserDTO(u) });
});
app.put('/api/me', authMiddleware, (req, res) => {
  const { avatarUrl, username } = req.body || {};
  const userId = req.user.id;

  // Actualizare avatar
  if (avatarUrl !== undefined) {
    db.prepare('UPDATE users SET avatar_url = ? WHERE id = ?').run(avatarUrl, userId);
  }

  // Actualizare username
  if (username !== undefined) {
    const trimmed = (username || '').trim();
    if (trimmed.length < 3) {
      return res.status(400).json({ error: 'Username-ul trebuie să aibă minim 3 caractere' });
    }
    // Verifică dacă username-ul este deja folosit de altcineva
    const exists = db.prepare('SELECT id FROM users WHERE username = ? AND id != ?').get(trimmed, userId);
    if (exists) {
      return res.status(409).json({ error: 'Acest username este deja folosit' });
    }
    db.prepare('UPDATE users SET username = ? WHERE id = ?').run(trimmed, userId);
  }

  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(userId);
  const userDto = toUserDTO(u);

  // Notifică toți utilizatorii despre schimbarea profilului (pentru sincronizare în timp real)
  if (avatarUrl !== undefined || username !== undefined) {
    io.emit('user:updated', userDto);
  }

  res.json({ user: userDto });
});
app.get('/api/users', authMiddleware, (req, res) => {
  const q = (req.query.q || '').toString().trim().toLowerCase();
  let rows;
  if (q) rows = db.prepare('SELECT * FROM users WHERE id != ? AND LOWER(username) LIKE ? ORDER BY username').all(req.user.id, `%${q}%`);
  else rows = db.prepare('SELECT * FROM users WHERE id != ? ORDER BY username').all(req.user.id);
  res.json({ users: rows.map(u => toUserDTO(u, true)) });
});

/* Admin: ștergere utilizator — curăță și DMs rămase cu 1 membru (dispar din sidebar) */
app.delete('/api/admin/users/:id', authMiddleware, adminOnly, (req, res) => {
  const targetId = Number(req.params.id);
  if (!targetId) return res.status(400).json({ error: 'ID invalid' });

  const target = db.prepare('SELECT * FROM users WHERE id = ?').get(targetId);
  if (!target) return res.status(404).json({ error: 'Utilizator inexistent' });
  if (target.username === 'ai_assistant') return res.status(400).json({ error: 'Nu poți șterge contul AI' });
  if ((target.role || 'user') === 'admin') return res.status(403).json({ error: 'Nu poți șterge un administrator' });
  if (targetId === req.user.id) return res.status(400).json({ error: 'Nu îți poți șterge propriul cont' });

  try {
    // Conversațiile la care era membru (salvate înainte să-i ștergem membership-urile)
    const convIdsRows = db.prepare('SELECT conversation_id FROM conversation_members WHERE user_id = ?').all(targetId);
    const convIds = Array.from(new Set(convIdsRows.map(r => r.conversation_id)));

    // Marchează ca șterse toate mesajele lui (pentru toți)
    db.prepare(`UPDATE messages
                SET is_deleted = 1, deleted_at = CURRENT_TIMESTAMP, content = NULL
                WHERE sender_id = ? AND IFNULL(is_deleted,0) = 0`).run(targetId);

    // Reacții/ascunderi/clear
    db.prepare('DELETE FROM message_reactions WHERE user_id = ?').run(targetId);
    db.prepare('DELETE FROM message_hidden WHERE user_id = ?').run(targetId);
    db.prepare('DELETE FROM cleared_conversations WHERE user_id = ?').run(targetId);
    db.prepare('DELETE FROM hidden_conversations WHERE user_id = ?').run(targetId);

    // Scoate din membri
    db.prepare('DELETE FROM conversation_members WHERE user_id = ?').run(targetId);

    // Curățăm conversațiile: 
    // - dacă 0 membri => hard delete
    // - dacă direct și <2 membri => hard delete (fix "Direct" rămas în sidebar)
    // - altfel => conversation:updated (membrii rămași vor vedea schimbarea)
    for (const cid of convIds) {
      const convRow = getConversationById(cid);
      if (!convRow) continue;
      const count = db.prepare('SELECT COUNT(*) AS n FROM conversation_members WHERE conversation_id = ?').get(cid)?.n || 0;

      if (count === 0 || (convRow.type === 'direct' && count < 2)) {
        hardDeleteConversation(cid);
      } else {
        const payload = buildConversationPayload(cid);
        if (payload) {
          const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(cid);
          for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:updated', payload);
        }
      }
    }

    // Șterge utilizatorul
    db.prepare('DELETE FROM users WHERE id = ?').run(targetId);

    // Forțează logout dacă e conectat
    io.to(`user:${targetId}`).emit('force:logout', { reason: 'Contul tău a fost șters de administrator.' });

    res.json({ ok: true });
  } catch (e) {
    console.error('Admin delete user failed:', e);
    res.status(500).json({ error: 'Eroare la ștergerea utilizatorului' });
  }
});

/* ============ Admin Database Viewer API ============ */
import { getDbStats, getAllUsers, getAllConversations, getRecentMessages } from './database.js';

app.get('/api/admin/db-stats', authMiddleware, adminOnly, (req, res) => {
  try {
    const stats = getDbStats();
    res.json(stats);
  } catch (e) {
    console.error('DB stats error:', e);
    res.status(500).json({ error: 'Eroare la citirea statisticilor' });
  }
});

app.get('/api/admin/db-tables/:table', authMiddleware, adminOnly, (req, res) => {
  try {
    const table = req.params.table;
    let data = [];
    switch (table) {
      case 'users':
        data = getAllUsers();
        break;
      case 'conversations':
        data = getAllConversations();
        break;
      case 'messages':
        data = getRecentMessages(50);
        break;
      default:
        return res.status(400).json({ error: 'Tabel necunoscut' });
    }
    res.json(data);
  } catch (e) {
    console.error('DB table error:', e);
    res.status(500).json({ error: 'Eroare la citirea tabelului' });
  }
});

app.get('/api/conversations', authMiddleware, (req, res) => {
  res.json({ conversations: getUserConversations(req.user.id, req.user.role) });
});

app.post('/api/conversations', authMiddleware, (req, res) => {
  const { name, memberIds } = req.body || {};
  if (!name || !Array.isArray(memberIds)) return res.status(400).json({ error: 'name și memberIds sunt necesare' });
  const convId = createGroupConversation(name, req.user.id, memberIds);
  const conv = getConversationById(convId);
  const payload = {
    id: conv.id, type: conv.type, name: conv.name, createdAt: conv.created_at,
    createdBy: conv.created_by || null, isAnnouncement: !!conv.is_announcement,
    members: getConversationMembers(conv.id), lastMessage: null
  };
  const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(convId);
  for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:new', payload);
  res.json({ conversation: payload });
});

app.post('/api/conversations/:id/members', authMiddleware, (req, res) => {
  const convId = Number(req.params.id);
  const { userIds } = req.body || {};
  const isAdminUser = (req.user.role || 'user') === 'admin';
  if (!isAdminUser && !userIsMember(convId, req.user.id)) return res.status(403).json({ error: 'Nu ești membru al conversației' });
  const insert = db.prepare('INSERT OR IGNORE INTO conversation_members (conversation_id, user_id, role) VALUES (?, ?, ?)');
  for (const uid of (userIds || [])) insert.run(convId, uid, 'member');

  // Dacă era privat și acum sunt mai mult de 2 membri => devine grup automat
  const countRes = db.prepare('SELECT COUNT(*) as cnt FROM conversation_members WHERE conversation_id = ?').get(convId);
  const convMeta = db.prepare('SELECT type, is_announcement FROM conversations WHERE id = ?').get(convId);

  // Orice conversație (care nu e anunț) cu > 2 membri devine GRUP
  if (!convMeta.is_announcement && countRes.cnt > 2 && convMeta.type !== 'group') {
    db.prepare("UPDATE conversations SET type = 'group' WHERE id = ?").run(convId);
  }

  const conv = getConversationById(convId);
  const payload = {
    id: conv.id, type: conv.type, name: conv.name, createdAt: conv.created_at,
    createdBy: conv.created_by || null, isAnnouncement: !!conv.is_announcement,
    members: getConversationMembers(conv.id), lastMessage: null
  };
  const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(convId);
  for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:updated', payload);
  res.json({ conversation: payload });
});

// Admin / owner: scoate un membru din conversație
app.delete('/api/conversations/:id/members/:userId', authMiddleware, (req, res) => {
  const convId = Number(req.params.id);
  const targetId = Number(req.params.userId);
  if (!convId || !targetId) return res.status(400).json({ error: 'ID invalid' });

  const isAdminUser = (req.user.role || 'user') === 'admin';
  // Admin are acces la orice conversație
  if (!isAdminUser && !userIsMember(convId, req.user.id)) {
    return res.status(403).json({ error: 'Nu ești membru al conversației' });
  }

  const conv = getConversationById(convId);
  if (!conv) return res.status(404).json({ error: 'Conversație inexistentă' });

  // nu poți scoate membri dintr-o conversație în care nici măcar nu mai există
  const target = db.prepare(`
    SELECT u.*
    FROM conversation_members cm
    JOIN users u ON u.id = cm.user_id
    WHERE cm.conversation_id = ? AND cm.user_id = ?
  `).get(convId, targetId);
  if (!target) return res.status(404).json({ error: 'Utilizatorul nu este membru în această conversație' });

  const isAdmin = (req.user.role || 'user') === 'admin';
  const isOwner = conv.created_by === req.user.id;

  // doar adminul global sau creatorul conversației pot scoate membri
  if (!isAdmin && !isOwner) {
    return res.status(403).json({ error: 'Doar adminul sau creatorul conversației pot scoate membri' });
  }

  // protecții: nu scoatem alți admini sau AI din conversație
  if ((target.role || 'user') === 'admin') {
    return res.status(403).json({ error: 'Nu poți scoate un administrator din conversație' });
  }
  if (target.username === 'ai_assistant') {
    return res.status(400).json({ error: 'Nu poți scoate contul AI din conversație' });
  }

  // nu are sens să "scoți" pe cineva prin acest endpoint dacă vrea doar să părăsească chatul
  // (pentru asta există DELETE /api/conversations/:id?scope=me)
  if (targetId === req.user.id && !isAdmin) {
    return res.status(400).json({ error: 'Folosește opțiunea de părăsire a conversației' });
  }

  db.prepare('DELETE FROM conversation_members WHERE conversation_id = ? AND user_id = ?').run(convId, targetId);

  // vezi câți membri au mai rămas
  const row = db.prepare('SELECT COUNT(*) AS n FROM conversation_members WHERE conversation_id = ?').get(convId);
  const count = row?.n || 0;

  // dacă nu mai sunt membri, sau e direct cu <2 membri -> ștergem conversația complet
  if (count === 0 || (conv.type === 'direct' && count < 2)) {
    hardDeleteConversation(convId);
    return res.json({ ok: true, deleted: true });
  }

  // altfel: trimitem conversation:updated la membrii rămași
  const payload = buildConversationPayload(convId);
  if (payload) {
    const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(convId);
    for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:updated', payload);
  }

  // userul scos primește "conversation:deleted" pentru acest chat
  io.to(`user:${targetId}`).emit('conversation:deleted', { conversationId: convId });

  res.json({ ok: true, deleted: false });
});

/* DM: notifică ambii utilizatori în timp real (fără refresh) */
app.post('/api/direct/:toUserId', authMiddleware, (req, res) => {
  const a = req.user.id;
  const b = Number(req.params.toUserId);
  if (a === b) return res.status(400).json({ error: 'Nu poți crea conversație cu tine' });
  let convId = findDirectConversation(a, b);
  if (!convId) convId = createDirectConversation(a, b);
  const conv = getConversationById(convId);
  const payload = {
    id: conv.id, type: conv.type, name: null, createdAt: conv.created_at,
    createdBy: conv.created_by || null,
    isAnnouncement: !!conv.is_announcement,
    members: getConversationMembers(conv.id), lastMessage: null
  };
  // Trimitem update pe socket la toți membrii (inclusiv cei noi)
  const allMembers = getConversationMembers(conv.id); // membrii noi sunt deja în DB
  allMembers.forEach(m => {
    io.to(`user:${m.id}`).emit('conversation:updated', payload);
  });

  res.json({ success: true, conversation: payload });
});

app.post('/api/ai/start', authMiddleware, (req, res) => {
  try {
    if (!AI_ENABLED) return res.status(500).json({ error: 'AI nu este configurat pe server' });
    const convId = getOrCreateAIConversationForUser(req.user.id);
    const conv = getConversationById(convId);
    res.json({
      conversation: {
        id: conv.id, type: conv.type, name: null, createdAt: conv.created_at,
        createdBy: conv.created_by || null, isAnnouncement: !!conv.is_announcement,
        members: getConversationMembers(conv.id), lastMessage: null
      }
    });
  } catch (e) { console.error('AI start error:', e); res.status(500).json({ error: 'Eroare la inițierea conversației AI' }); }
});

app.get('/api/conversations/:id/messages', authMiddleware, (req, res) => {
  const convId = Number(req.params.id);
  const isAdminUser = (req.user.role || 'user') === 'admin';

  // Admin poate citi orice conversație, utilizatorii normali doar cele în care sunt membri
  if (!isAdminUser && !userIsMember(convId, req.user.id)) {
    return res.status(403).json({ error: 'Nu ești membru al conversației' });
  }

  const limit = Math.min(Number(req.query.limit || 50), 100);
  const offset = Number(req.query.offset || 0);
  const cleared = db.prepare(`SELECT cleared_at FROM cleared_conversations WHERE conversation_id = ? AND user_id = ?`).get(convId, req.user.id);

  let sql = `
    SELECT m.*, u.id as u_id, u.username as u_username, u.avatar_url as u_avatar_url, u.created_at as u_created_at
    FROM messages m
    JOIN users u ON u.id = m.sender_id
    WHERE m.conversation_id = ?
      AND NOT EXISTS (SELECT 1 FROM message_hidden mh WHERE mh.message_id = m.id AND mh.user_id = ?)
  `;
  const params = [convId, req.user.id];
  if (cleared?.cleared_at) { sql += ` AND m.created_at > ?`; params.push(cleared.cleared_at); }
  sql += ` ORDER BY m.created_at DESC LIMIT ? OFFSET ?`; params.push(limit, offset);

  const rows = db.prepare(sql).all(...params);
  const raw = rows.map(r => toMessageDTO(r, { id: r.u_id, username: r.u_username, avatar_url: r.u_avatar_url, created_at: r.u_created_at })).reverse();

  const ids = raw.map(m => m.id);
  const reactMap = getReactionsMapForMessages(ids, req.user.id);
  const messages = raw.map(m => ({ ...m, reactions: reactMap.get(m.id) || [] }));
  res.json({ messages });
});

app.post('/api/upload', authMiddleware, upload.single('file'), (req, res) => {
  const fileUrl = `/uploads/${req.file.filename}`;
  res.json({ fileUrl });
});

function insertMessage(conversationId, senderId, { content, type = 'text', fileUrl = null }) {
  const info = db.prepare(`
    INSERT INTO messages (conversation_id, sender_id, content, type, file_url)
    VALUES (?, ?, ?, ?, ?)
  `).run(conversationId, senderId, content || null, type, fileUrl || null);
  return db.prepare('SELECT * FROM messages WHERE id = ?').get(info.lastInsertRowid);
}

/* Anunțuri */
app.post('/api/announcements', authMiddleware, (req, res) => {
  try {
    let { title, content, recipientIds, fileUrl } = req.body || {};
    title = (title || '').toString().trim();
    content = (content || '').toString();
    if (!title) return res.status(400).json({ error: 'Titlul anunțului este necesar' });
    if (!content && !fileUrl) return res.status(400).json({ error: 'Anunțul trebuie să conțină text sau fișier' });

    let recipients = [];
    if (recipientIds === 'all') {
      recipients = db.prepare('SELECT id FROM users WHERE id != ?').all(req.user.id).map(r => r.id);
    } else if (Array.isArray(recipientIds) && recipientIds.length > 0) {
      recipients = Array.from(new Set(recipientIds.map(Number).filter(id => id && id !== req.user.id)));
    } else {
      return res.status(400).json({ error: 'Alege destinatari sau selectează "toți"' });
    }

    const convId = createAnnouncementConversation(title, req.user.id, recipients);
    const type = fileUrl ? 'file' : 'text';
    const msgRow = insertMessage(convId, req.user.id, { content, type, fileUrl });
    const sender = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
    const msgPayload = toMessageDTO(msgRow, sender);

    const conv = getConversationById(convId);
    const convPayload = {
      id: conv.id, type: conv.type, name: conv.name, createdAt: conv.created_at,
      createdBy: conv.created_by || null, isAnnouncement: !!conv.is_announcement,
      members: getConversationMembers(conv.id), lastMessage: msgPayload
    };

    const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(convId);
    for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:new', convPayload);
    io.to(`conv:${convId}`).emit('message:new', msgPayload);
    res.json({ conversation: convPayload, message: msgPayload });
  } catch (e) { console.error(e); res.status(500).json({ error: 'Eroare la crearea anunțului' }); }
});
/* Reacții */
function getReactionsMapForMessages(messageIds, currentUserId) {
  if (!messageIds.length) return new Map();
  const placeholders = messageIds.map(() => '?').join(',');
  const rows = db.prepare(`
    SELECT message_id, emoji, COUNT(*) as count,
           SUM(CASE WHEN user_id = ? THEN 1 ELSE 0 END) as reactedByMe
    FROM message_reactions
    WHERE message_id IN (${placeholders})
    GROUP BY message_id, emoji
  `).all(currentUserId, ...messageIds);
  const map = new Map();
  for (const r of rows) {
    if (!map.has(r.message_id)) map.set(r.message_id, []);
    map.get(r.message_id).push({ emoji: r.emoji, count: r.count, reactedByMe: !!r.reactedByMe });
  }
  return map;
}
app.post('/api/messages/:id/reactions', authMiddleware, (req, res) => {
  const msgId = Number(req.params.id);
  const { emoji } = req.body || {};
  if (!emoji || typeof emoji !== 'string' || emoji.length > 16) return res.status(400).json({ error: 'Emoji invalid' });

  const msg = db.prepare('SELECT * FROM messages WHERE id = ?').get(msgId);
  if (!msg) return res.status(404).json({ error: 'Mesaj inexistent' });
  if (!userIsMember(msg.conversation_id, req.user.id)) return res.status(403).json({ error: 'Nu ai acces' });
  if (msg.is_deleted) return res.status(400).json({ error: 'Mesaj șters' });

  const ins = db.prepare('INSERT OR IGNORE INTO message_reactions (message_id, user_id, emoji) VALUES (?, ?, ?)');
  const del = db.prepare('DELETE FROM message_reactions WHERE message_id = ? AND user_id = ? AND emoji = ?');

  const info = ins.run(msgId, req.user.id, emoji);
  if (info.changes === 0) del.run(msgId, req.user.id, emoji);

  const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(msg.conversation_id);
  for (const m of members) {
    const reactions = getReactionsMapForMessages([msgId], m.user_id).get(msgId) || [];
    io.to(`user:${m.user_id}`).emit('reaction:updated', { messageId: msgId, conversationId: msg.conversation_id, reactions });
  }
  const myReactions = getReactionsMapForMessages([msgId], req.user.id).get(msgId) || [];
  res.json({ reactions: myReactions });
});
/* ===== Socket.IO auth robust ===== */
io.use((socket, next) => {
  try {
    const tokenFromAuth = socket.handshake?.auth?.token;
    const tokenFromQuery = typeof socket.handshake?.query?.token === 'string'
      ? socket.handshake.query.token
      : null;
    const authHdr = socket.handshake?.headers?.authorization || socket.request?.headers?.authorization || '';
    const tokenFromHeader = authHdr.startsWith('Bearer ') ? authHdr.slice(7) : null;

    const token = tokenFromAuth || tokenFromQuery || tokenFromHeader;
    if (!token) {
      console.warn('Socket auth: lipsă token', { ip: socket.handshake?.address, ua: socket.handshake?.headers?.['user-agent'] });
      return next(new Error('Neautorizat'));
    }
    const payload = jwt.verify(token, JWT_SECRET);
    socket.user = { id: payload.sub, username: payload.username, role: payload.role || 'user' };
    next();
  } catch (e) {
    console.warn('Socket auth error:', e?.message);
    next(new Error('Token invalid'));
  }
});

io.on('connection', (socket) => {
  const userId = socket.user?.id;
  if (!userId) {
    try { socket.disconnect(true); } catch { }
    return;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //                          🟢 ONLINE STATUS TRACKING
  // ═══════════════════════════════════════════════════════════════════════════

  // Adaugă socket-ul la lista de conexiuni ale user-ului
  if (!onlineUsers.has(userId)) {
    onlineUsers.set(userId, new Set());
    // Primul socket pentru acest user - emit online status
    io.emit('user:online', { userId });
  }
  onlineUsers.get(userId).add(socket.id);

  // Trimite lista completă de useri online la noul conectat
  socket.emit('users:online:list', { userIds: Array.from(onlineUsers.keys()) });

  // Camere personale și camerele conversațiilor la care e membru
  socket.join(`user:${userId}`);
  const convIds = db.prepare('SELECT conversation_id FROM conversation_members WHERE user_id = ?').all(userId).map(r => r.conversation_id);
  for (const id of convIds) socket.join(`conv:${id}`);

  // Handle disconnect
  socket.on('disconnect', () => {
    const userSockets = onlineUsers.get(userId);
    if (userSockets) {
      userSockets.delete(socket.id);
      if (userSockets.size === 0) {
        // Ultimul socket s-a deconectat - user e offline
        onlineUsers.delete(userId);
        // Actualizează last_seen în DB
        try {
          db.prepare('UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE id = ?').run(userId);
        } catch (e) {
          console.warn('Failed to update last_seen:', e?.message);
        }
        // Emit offline status
        io.emit('user:offline', { userId });
      }
    }
  });

  socket.on('join', ({ conversationId }) => {
    if (!conversationId) return;
    if (!userIsMember(conversationId, userId)) return;
    socket.join(`conv:${conversationId}`);
  });

  socket.on('leave', ({ conversationId }) => {
    if (!conversationId) return;
    socket.leave(`conv:${conversationId}`);
  });

  // Trimitere mesaj (fan-out + conv:updated)
  socket.on('message:send', async ({ conversationId, content, type, fileUrl }) => {
    if (!conversationId) return;
    if (!userIsMember(conversationId, userId)) return;

    const conv = getConversationById(conversationId);
    if (conv?.is_announcement && conv.created_by !== userId) return;

    // Dacă era ascunsă conversația pentru MINE, re-apare (doar pentru mine)
    db.prepare('DELETE FROM hidden_conversations WHERE conversation_id = ? AND user_id = ?').run(conversationId, userId);

    const msgRow = insertMessage(conversationId, userId, { content, type, fileUrl });
    const sender = db.prepare('SELECT * FROM users WHERE id = ?').get(userId);
    const payload = toMessageDTO(msgRow, sender);

    // 1) realtime către toți din cameră
    io.to(`conv:${conversationId}`).emit('message:new', payload);

    // 2) sincronizează lista conv la toți membrii
    try {
      const convRow = getConversationById(conversationId);
      const convPayload = {
        id: convRow.id, type: convRow.type, name: convRow.name, createdAt: convRow.created_at,
        createdBy: convRow.created_by || null, isAnnouncement: !!convRow.is_announcement,
        members: getConversationMembers(conversationId), lastMessage: payload
      };
      const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(conversationId);
      for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:updated', convPayload);
    } catch (e) {
      console.warn('conv:updated fanout fail', e?.message);
    }

    // 3) AI reply (dacă e conv cu AI) + sync listă conv
    try {
      if (type === 'text' && isAIConversation(conversationId) && AI_ENABLED) {
        const history = getAIHistory(conversationId, 20);
        const aiText = await aiComplete(history);
        if (aiText) {
          const ai = ensureAIUser();
          const aiMsgRow = insertMessage(conversationId, ai.id, { content: aiText, type: 'text' });
          const aiPayload = toMessageDTO(aiMsgRow, ai);
          io.to(`conv:${conversationId}`).emit('message:new', aiPayload);

          try {
            const convRow2 = getConversationById(conversationId);
            const convPayload2 = {
              id: convRow2.id, type: convRow2.type, name: convRow2.name, createdAt: convRow2.created_at,
              createdBy: convRow2.created_by || null, isAnnouncement: !!convRow2.is_announcement,
              members: getConversationMembers(conversationId), lastMessage: aiPayload
            };
            const members2 = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(conversationId);
            for (const m of members2) io.to(`user:${m.user_id}`).emit('conversation:updated', convPayload2);
          } catch (e2) { console.warn('conv:updated fanout (AI) fail', e2?.message); }
        }
      }
    } catch (err) {
      console.error('AI reply failed:', err?.message || err);
      const ai = ensureAIUser();
      const aiMsgRow = insertMessage(conversationId, ai.id, {
        content: 'Îmi pare rău, a apărut o eroare la generarea răspunsului 🤖.',
        type: 'text'
      });
      const aiPayload = toMessageDTO(aiMsgRow, ai);
      io.to(`conv:${conversationId}`).emit('message:new', aiPayload);

      try {
        const convRow3 = getConversationById(conversationId);
        const convPayload3 = {
          id: convRow3.id, type: convRow3.type, name: convRow3.name, createdAt: convRow3.created_at,
          createdBy: convRow3.created_by || null, isAnnouncement: !!convRow3.is_announcement,
          members: getConversationMembers(conversationId), lastMessage: aiPayload
        };
        const members3 = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(conversationId);
        for (const m of members3) io.to(`user:${m.user_id}`).emit('conversation:updated', convPayload3);
      } catch (e3) { console.warn('conv:updated fanout (AI fallback) fail', e3?.message); }
    }
  });

  // Mesaj direct rapid (compat)
  socket.on('message:private', ({ toUserId, content }) => {
    const a = userId;
    const b = Number(toUserId);
    if (!b || a === b) return;
    let convId = findDirectConversation(a, b);
    if (!convId) convId = createDirectConversation(a, b);
    const msgRow = insertMessage(convId, a, { content, type: 'text' });
    const sender = db.prepare('SELECT * FROM users WHERE id = ?').get(a);
    const payload = toMessageDTO(msgRow, sender);

    // Unhide doar pentru mine
    db.prepare('DELETE FROM hidden_conversations WHERE conversation_id = ? AND user_id = ?').run(convId, a);
    io.to(`conv:${convId}`).emit('message:new', payload);

    const conv = getConversationById(convId);
    const convPayload = {
      id: conv.id, type: conv.type, name: null, createdAt: conv.created_at,
      createdBy: conv.created_by || null, isAnnouncement: !!conv.is_announcement,
      members: getConversationMembers(conv.id), lastMessage: payload
    };
    io.to(`user:${a}`).emit('conversation:new', convPayload);
    io.to(`user:${b}`).emit('conversation:new', convPayload);
  });

  socket.on('conversation:create:group', ({ name, memberIds }) => {
    const convId = createGroupConversation(name, userId, memberIds || []);
    const conv = getConversationById(convId);
    const payload = {
      id: conv.id, type: conv.type, name: conv.name, createdAt: conv.created_at,
      createdBy: conv.created_by || null, isAnnouncement: !!conv.is_announcement,
      members: getConversationMembers(conv.id), lastMessage: null
    };
    const members = db.prepare('SELECT user_id FROM conversation_members WHERE conversation_id = ?').all(convId);
    for (const m of members) io.to(`user:${m.user_id}`).emit('conversation:new', payload);
  });

  socket.on('typing', ({ conversationId, isTyping }) => {
    if (!userIsMember(conversationId, userId)) return;
    const conv = getConversationById(conversationId);
    if (conv?.is_announcement && conv.created_by !== userId) return;
    socket.to(`conv:${conversationId}`).emit('typing', { conversationId, userId, isTyping: !!isTyping });
  });
});

/* Editare mesaj */
app.put('/api/messages/:id', authMiddleware, (req, res) => {
  const msgId = Number(req.params.id);
  const { content } = req.body || {};
  const msg = db.prepare('SELECT * FROM messages WHERE id = ?').get(msgId);
  if (!msg) return res.status(404).json({ error: 'Mesaj inexistent' });
  if (!userIsMember(msg.conversation_id, req.user.id)) return res.status(403).json({ error: 'Nu ai acces' });
  if (msg.sender_id !== req.user.id) return res.status(403).json({ error: 'Poți edita doar mesajele tale' });
  if (msg.type !== 'text') return res.status(400).json({ error: 'Se pot edita doar mesajele text' });
  if (!content || !content.trim()) return res.status(400).json({ error: 'Conținut gol' });
  if (msg.is_deleted) return res.status(400).json({ error: 'Mesaj șters' });

  db.prepare('UPDATE messages SET content = ?, edited_at = CURRENT_TIMESTAMP WHERE id = ?').run(content, msgId);
  const updated = db.prepare('SELECT * FROM messages WHERE id = ?').get(msgId);
  const sender = db.prepare('SELECT * FROM users WHERE id = ?').get(updated.sender_id);
  const dto = toMessageDTO(updated, sender);
  io.to(`conv:${msg.conversation_id}`).emit('message:updated', dto);
  res.json({ message: dto });
});

/* Ștergere mesaj */
app.delete('/api/messages/:id', authMiddleware, (req, res) => {
  try {
    const msgId = Number(req.params.id);
    const scope = (req.query.scope || 'all').toString();
    const msg = db.prepare('SELECT * FROM messages WHERE id = ?').get(msgId);
    if (!msg) return res.status(404).json({ error: 'Mesaj inexistent' });
    if (!userIsMember(msg.conversation_id, req.user.id)) return res.status(403).json({ error: 'Nu ai acces' });

    if (scope === 'me') {
      db.prepare('INSERT OR IGNORE INTO message_hidden (message_id, user_id, hidden_at) VALUES (?, ?, CURRENT_TIMESTAMP)').run(msgId, req.user.id);
      io.to(`user:${req.user.id}`).emit('message:hidden', { id: msgId, conversationId: msg.conversation_id });
      return res.json({ ok: true });
    }

    const conv = getConversationById(msg.conversation_id);
    const canDelete = (msg.sender_id === req.user.id) || (!!conv && conv.created_by === req.user.id);
    if (!canDelete) return res.status(403).json({ error: 'Nu ai voie să ștergi acest mesaj' });
    if (msg.is_deleted) return res.json({ ok: true });

    try { db.prepare('DELETE FROM message_reactions WHERE message_id = ?').run(msgId); } catch { }

    db.prepare('UPDATE messages SET is_deleted = 1, deleted_at = CURRENT_TIMESTAMP, content = NULL WHERE id = ?').run(msgId);

    io.to(`conv:${msg.conversation_id}`).emit('message:deleted', { id: msgId, conversationId: msg.conversation_id });
    res.json({ ok: true });
  } catch (err) {
    console.error('DELETE /api/messages/:id failed', { err: err?.message, stack: err?.stack });
    res.status(500).json({ error: 'Eroare internă la ștergerea mesajului' });
  }
});

/* Curățare conversație (me/all) */
app.post('/api/conversations/:id/clear', authMiddleware, (req, res) => {
  const convId = Number(req.params.id);
  const { scope = 'me' } = req.body || {};
  const isAdminUser = (req.user.role || 'user') === 'admin';
  if (!isAdminUser && !userIsMember(convId, req.user.id)) return res.status(403).json({ error: 'Nu ești membru' });

  if (scope === 'me') {
    db.prepare('INSERT OR REPLACE INTO cleared_conversations (conversation_id, user_id, cleared_at) VALUES (?, ?, CURRENT_TIMESTAMP)').run(convId, req.user.id);
    return res.json({ ok: true });
  } else if (scope === 'all') {
    const conv = getConversationById(convId);
    // Admin poate curăța orice conversație
    const canAll = isAdminUser || conv.type === 'direct' || conv.created_by === req.user.id;
    if (!canAll) return res.status(403).json({ error: 'Doar creatorul grupului poate curăța pentru toți' });
    db.prepare('DELETE FROM message_reactions WHERE message_id IN (SELECT id FROM messages WHERE conversation_id = ?)').run(convId);
    db.prepare('DELETE FROM message_hidden WHERE message_id IN (SELECT id FROM messages WHERE conversation_id = ?)').run(convId);
    db.prepare('DELETE FROM messages WHERE conversation_id = ?').run(convId);
    io.to(`conv:${convId}`).emit('conversation:cleared', { conversationId: convId, scope: 'all' });
    return res.json({ ok: true });
  } else return res.status(400).json({ error: 'scope invalid' });
});

/* Ștergere conversație (me/all) */
app.delete('/api/conversations/:id', authMiddleware, (req, res) => {
  const convId = Number(req.params.id);
  const scope = (req.query.scope || 'me').toString();
  const isAdminUser = (req.user.role || 'user') === 'admin';
  if (!isAdminUser && !userIsMember(convId, req.user.id)) return res.status(403).json({ error: 'Nu ești membru' });

  if (scope === 'me') {
    db.prepare('INSERT OR REPLACE INTO hidden_conversations (conversation_id, user_id, hidden_at) VALUES (?, ?, CURRENT_TIMESTAMP)').run(convId, req.user.id);
    db.prepare('INSERT OR REPLACE INTO cleared_conversations (conversation_id, user_id, cleared_at) VALUES (?, ?, CURRENT_TIMESTAMP)').run(convId, req.user.id);
    return res.json({ ok: true });
  } else if (scope === 'all') {
    const conv = getConversationById(convId);
    // Admin poate șterge orice conversație
    const canAll = isAdminUser || conv.type === 'direct' || conv.created_by === req.user.id;
    if (!canAll) return res.status(403).json({ error: 'Nu ai drepturi să ștergi pentru toți' });

    hardDeleteConversation(convId);
    return res.json({ ok: true });
  } else return res.status(400).json({ error: 'scope invalid' });
});

/* Diag AI + Health */
app.get('/api/ai/diag', authMiddleware, async (_req, res) => {
  if (!AI_ENABLED) return res.status(500).json({ ok: false, error: 'AI disabled' });
  try {
    const out = await chatOnce(AI_MODEL, [{ role: 'user', content: 'ping' }]);
    res.json({ ok: true, model: AI_MODEL, output: out });
  } catch (e) {
    const primaryErr = e?.message || String(e);
    try {
      const out2 = await chatOnce(AI_FALLBACK_MODEL, [{ role: 'user', content: 'ping' }]);
      res.json({ ok: false, error: primaryErr, fallback: { model: AI_FALLBACK_MODEL, output: out2 } });
    } catch (e2) {
      res.status(500).json({ ok: false, error: primaryErr, fallbackError: e2?.message || String(e2) });
    }
  }
});
app.get('/api/ai/diag-dev', async (req, res) => {
  const key = (req.query.key || '').toString();
  if (!process.env.ADMIN_API_KEY || key !== process.env.ADMIN_API_KEY) return res.status(401).json({ error: 'Neautorizat' });
  if (!AI_ENABLED) return res.status(500).json({ ok: false, error: 'AI disabled' });
  try {
    const out = await chatOnce(AI_MODEL, [{ role: 'user', content: 'ping' }]);
    res.json({ ok: true, model: AI_MODEL, output: out });
  } catch (e) {
    const primaryErr = e?.message || String(e);
    try {
      const out2 = await chatOnce(AI_FALLBACK_MODEL, [{ role: 'user', content: 'ping' }]);
      res.json({ ok: false, error: primaryErr, fallback: { model: AI_FALLBACK_MODEL, output: out2 } });
    } catch (e2) {
      res.status(500).json({ ok: false, error: primaryErr, fallbackError: e2?.message || String(e2) });
    }
  }
});

app.get('/api/health', (_req, res) => res.json({ ok: true, time: new Date().toISOString(), ai: AI_ENABLED, model: AI_MODEL }));

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Server pornit pe http://localhost:${PORT}`);
});