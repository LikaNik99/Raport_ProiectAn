/**
 * ═══════════════════════════════════════════════════════════════════════════
 *                         📦 DATABASE MODULE
 *                    Chat Application - SQLite Database
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Acest modul conține toată logica bazei de date pentru aplicația de chat.
 * Baza de date folosește SQLite cu better-sqlite3 pentru performanță.
 * 
 * 📊 STRUCTURA TABELELOR:
 * ├── users                  - Utilizatorii aplicației
 * ├── conversations          - Conversațiile (directe/grupuri)
 * ├── conversation_members   - Membrii fiecărei conversații
 * ├── messages              - Mesajele trimise
 * ├── message_reactions     - Reacții emoji la mesaje
 * ├── cleared_conversations - Conversații curățate per utilizator
 * ├── hidden_conversations  - Conversații ascunse per utilizator
 * └── message_hidden        - Mesaje ascunse individual
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */

import Database from 'better-sqlite3';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ═══════════════════════════════════════════════════════════════════════════
//                          🔌 CONEXIUNE DATABASE
// ═══════════════════════════════════════════════════════════════════════════

// Data directory for SQLite database (supports Docker volumes)
import fs from 'fs';
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '..');
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

const db = new Database(path.join(DATA_DIR, 'chat.db'));

// Optimizări pentru performanță
db.pragma('journal_mode = WAL');      // Write-Ahead Logging pentru scrieri rapide
db.pragma('foreign_keys = ON');       // Activează cheile externe

// ═══════════════════════════════════════════════════════════════════════════
//                          📋 SCHEMA TABELELOR
// ═══════════════════════════════════════════════════════════════════════════

db.exec(`

-- ═══════════════════════════════════════════════════════════════════════════
-- 👤 USERS - Utilizatorii aplicației
-- ═══════════════════════════════════════════════════════════════════════════
-- Stochează informațiile despre fiecare utilizator înregistrat.
-- Parola este stocată ca hash bcrypt pentru securitate.
-- Rolul poate fi 'user' sau 'admin'.

CREATE TABLE IF NOT EXISTS users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,  -- ID unic auto-incrementat
  username      TEXT UNIQUE NOT NULL,               -- Numele de utilizator (unic)
  password_hash TEXT NOT NULL,                      -- Hash-ul parolei (bcrypt)
  avatar_url    TEXT,                               -- URL avatar (opțional)
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP, -- Data creării contului
  role          TEXT DEFAULT 'user'                 -- Rolul: 'user' sau 'admin'
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 💬 CONVERSATIONS - Conversațiile
-- ═══════════════════════════════════════════════════════════════════════════
-- Reprezintă o conversație între utilizatori.
-- Tipuri: 'direct' (1-la-1) sau 'group' (grup/canal).
-- is_announcement: 1 pentru canale de anunțuri (doar adminii pot posta).

CREATE TABLE IF NOT EXISTS conversations (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,         -- ID unic
  type            TEXT CHECK(type IN ('direct', 'group')) NOT NULL, -- Tipul conversației
  name            TEXT,                                      -- Numele (doar pentru grupuri)
  created_by      INTEGER,                                   -- ID-ul creatorului
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,        -- Data creării
  is_announcement INTEGER DEFAULT 0                          -- 1 = canal de anunțuri
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 👥 CONVERSATION_MEMBERS - Membrii conversațiilor
-- ═══════════════════════════════════════════════════════════════════════════
-- Leagă utilizatorii de conversații (relație many-to-many).
-- Rolul în conversație: 'member' sau 'admin'.

CREATE TABLE IF NOT EXISTS conversation_members (
  conversation_id INTEGER NOT NULL,                  -- ID conversație
  user_id         INTEGER NOT NULL,                  -- ID utilizator
  role            TEXT DEFAULT 'member',             -- Rolul în grup
  joined_at       DATETIME DEFAULT CURRENT_TIMESTAMP,-- Data adăugării
  PRIMARY KEY (conversation_id, user_id)             -- Cheie compusă
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 📝 MESSAGES - Mesajele
-- ═══════════════════════════════════════════════════════════════════════════
-- Stochează toate mesajele trimise în conversații.
-- Tipuri: 'text', 'file', 'image', etc.
-- is_deleted: soft delete (mesajul nu dispare din DB, doar se marchează).

CREATE TABLE IF NOT EXISTS messages (
  id              INTEGER PRIMARY KEY AUTOINCREMENT, -- ID mesaj
  conversation_id INTEGER NOT NULL,                  -- ID conversație
  sender_id       INTEGER NOT NULL,                  -- ID expeditor
  content         TEXT,                              -- Conținutul mesajului
  type            TEXT DEFAULT 'text',               -- Tipul: text/file/image
  file_url        TEXT,                              -- URL fișier atașat
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,-- Data trimiterii
  edited_at       DATETIME,                          -- Data ultimei editări
  is_deleted      INTEGER DEFAULT 0,                 -- 1 = mesaj șters
  deleted_at      DATETIME                           -- Data ștergerii
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 😀 MESSAGE_REACTIONS - Reacții la mesaje
-- ═══════════════════════════════════════════════════════════════════════════
-- Stochează reacțiile emoji de la utilizatori pentru fiecare mesaj.
-- Un utilizator poate adăuga mai multe emoji-uri diferite la același mesaj.

CREATE TABLE IF NOT EXISTS message_reactions (
  message_id INTEGER NOT NULL,                       -- ID mesaj
  user_id    INTEGER NOT NULL,                       -- ID utilizator
  emoji      TEXT NOT NULL,                          -- Emoji-ul folosit
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,     -- Data reacției
  PRIMARY KEY (message_id, user_id, emoji)           -- Cheie compusă
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 🧹 CLEARED_CONVERSATIONS - Conversații curățate
-- ═══════════════════════════════════════════════════════════════════════════
-- Când un utilizator "curăță" o conversație, mesajele vechi nu-i mai apar.
-- Mesajele rămân în DB pentru ceilalți utilizatori.

CREATE TABLE IF NOT EXISTS cleared_conversations (
  conversation_id INTEGER NOT NULL,       -- ID conversație
  user_id         INTEGER NOT NULL,       -- ID utilizator
  cleared_at      DATETIME NOT NULL,      -- Momentul curățării
  PRIMARY KEY (conversation_id, user_id)  -- Cheie compusă
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 👁️ HIDDEN_CONVERSATIONS - Conversații ascunse
-- ═══════════════════════════════════════════════════════════════════════════
-- Permite utilizatorilor să ascundă anumite conversații din lista lor.

CREATE TABLE IF NOT EXISTS hidden_conversations (
  conversation_id INTEGER NOT NULL,                  -- ID conversație
  user_id         INTEGER NOT NULL,                  -- ID utilizator
  hidden_at       DATETIME DEFAULT CURRENT_TIMESTAMP,-- Momentul ascunderii
  PRIMARY KEY (conversation_id, user_id)             -- Cheie compusă
);


-- ═══════════════════════════════════════════════════════════════════════════
-- 🙈 MESSAGE_HIDDEN - Mesaje ascunse individual
-- ═══════════════════════════════════════════════════════════════════════════
-- Permite ascunderea unui mesaj specific doar pentru un utilizator.

CREATE TABLE IF NOT EXISTS message_hidden (
  message_id INTEGER NOT NULL,                       -- ID mesaj
  user_id    INTEGER NOT NULL,                       -- ID utilizator
  hidden_at  DATETIME DEFAULT CURRENT_TIMESTAMP,     -- Momentul ascunderii
  PRIMARY KEY (message_id, user_id)                  -- Cheie compusă
);

`);

// ═══════════════════════════════════════════════════════════════════════════
//                          📊 INDEXURI PENTRU PERFORMANȚĂ
// ═══════════════════════════════════════════════════════════════════════════
// Indexurile accelerează căutările frecvente în baza de date.

db.exec(`
-- Index pentru căutarea mesajelor în ordine cronologică
CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id, created_at DESC);

-- Indexuri pentru relația conversații-membri
CREATE INDEX IF NOT EXISTS idx_cm_user ON conversation_members(user_id);
CREATE INDEX IF NOT EXISTS idx_cm_conv ON conversation_members(conversation_id);

-- Indexuri pentru conversații/mesaje ascunse
CREATE INDEX IF NOT EXISTS idx_hidden_conv_user ON hidden_conversations(conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_cleared_conv_user ON cleared_conversations(conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_hidden_msg_user ON message_hidden(message_id, user_id);

-- Indexuri pentru reacții (căutări rapide)
CREATE INDEX IF NOT EXISTS idx_react_msg ON message_reactions(message_id);
CREATE INDEX IF NOT EXISTS idx_react_msg_user ON message_reactions(message_id, user_id);
CREATE INDEX IF NOT EXISTS idx_react_msg_emoji ON message_reactions(message_id, emoji);
`);

// ═══════════════════════════════════════════════════════════════════════════
//                          📈 FUNCȚII STATISTICE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Obține statistici generale despre baza de date
 * @returns {Object} Statistici: nr. utilizatori, conversații, mesaje, etc.
 */
export function getDbStats() {
  const stats = {
    users: db.prepare('SELECT COUNT(*) as count FROM users').get().count,
    conversations: db.prepare('SELECT COUNT(*) as count FROM conversations').get().count,
    messages: db.prepare('SELECT COUNT(*) as count FROM messages').get().count,
    reactions: db.prepare('SELECT COUNT(*) as count FROM message_reactions').get().count,
    groups: db.prepare("SELECT COUNT(*) as count FROM conversations WHERE type = 'group'").get().count,
    directChats: db.prepare("SELECT COUNT(*) as count FROM conversations WHERE type = 'direct'").get().count,
    announcements: db.prepare("SELECT COUNT(*) as count FROM conversations WHERE is_announcement = 1").get().count,
  };
  return stats;
}

/**
 * Obține toți utilizatorii pentru vizualizare admin
 * @returns {Array} Lista utilizatorilor
 */
export function getAllUsers() {
  return db.prepare(`
    SELECT id, username, avatar_url, role, created_at 
    FROM users 
    ORDER BY created_at DESC
  `).all();
}

/**
 * Obține toate conversațiile pentru vizualizare admin
 * @returns {Array} Lista conversațiilor cu numărul de membri
 */
export function getAllConversations() {
  return db.prepare(`
    SELECT 
      c.id, 
      c.type, 
      c.name, 
      c.is_announcement,
      c.created_at,
      (SELECT COUNT(*) FROM conversation_members WHERE conversation_id = c.id) as member_count,
      (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id) as message_count
    FROM conversations c
    ORDER BY c.created_at DESC
  `).all();
}

/**
 * Obține ultimele mesaje pentru vizualizare admin
 * @param {number} limit - Numărul maxim de mesaje
 * @returns {Array} Lista mesajelor recente
 */
export function getRecentMessages(limit = 50) {
  return db.prepare(`
    SELECT 
      m.id,
      m.conversation_id,
      m.content,
      m.type,
      m.created_at,
      m.is_deleted,
      u.username as sender_name
    FROM messages m
    LEFT JOIN users u ON u.id = m.sender_id
    ORDER BY m.created_at DESC
    LIMIT ?
  `).all(limit);
}

/**
 * Obține membrii unei conversații
 * @param {number} convId - ID-ul conversației
 * @returns {Array} Lista membrilor
 */
export function getConversationMembersAdmin(convId) {
  return db.prepare(`
    SELECT u.id, u.username, u.avatar_url, cm.role, cm.joined_at
    FROM conversation_members cm
    JOIN users u ON u.id = cm.user_id
    WHERE cm.conversation_id = ?
    ORDER BY cm.joined_at
  `).all(convId);
}

// ═══════════════════════════════════════════════════════════════════════════
//                          📤 EXPORT
// ═══════════════════════════════════════════════════════════════════════════

export default db;
