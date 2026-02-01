/* =========================================================
   app.js — Modern Messenger UI + Admin (extins)
   ========================================================= */

/* ========= Config locale/timezone ========= */
const LOCALE = 'ro-RO';
const DEFAULT_TZ = 'Europe/Chisinau';
function getTZ() { return localStorage.getItem('tz') || DEFAULT_TZ; }
function setTZ(tz) { localStorage.setItem('tz', tz); document.dispatchEvent(new Event('tz:changed')); }

/* ========= Theme Management (Dark/Light) ========= */
const THEME_KEY = 'app-theme';

function getTheme() {
  return localStorage.getItem(THEME_KEY) || 'light';
}

function setTheme(theme) {
  localStorage.setItem(THEME_KEY, theme);
  document.documentElement.setAttribute('data-theme', theme);
  document.dispatchEvent(new CustomEvent('theme:changed', { detail: { theme } }));
}

function toggleTheme() {
  const current = getTheme();
  const next = current === 'dark' ? 'light' : 'dark';
  setTheme(next);
}

// Initialize theme on page load
(function initTheme() {
  const saved = getTheme();
  document.documentElement.setAttribute('data-theme', saved);
})();

/* ========= State ========= */
const state = {
  token: null,
  user: null,            // { id, username, avatarUrl, role }
  conversations: [],
  currentConvId: null,
  users: [],
  socket: null,
  unread: {},             // convId -> count (local)
  messages: new Map(),    // convId -> [messages]
  onlineUsers: new Set(), // Set of online user IDs
  ui: {
    filter: 'all',        // all | direct | group
    rightOpen: false,
    msgSearchOpen: false,
    msgSearchQuery: '',
    sections: { announcements: false, groups: false, direct: false } // true=colapsat
  }
};

/* ========= DOM utils ========= */
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

/* ========= Helpers vizuale ========= */
function userAvatarUrl(username, avatarUrl) {
  const lower = (username || '').toLowerCase();

  if (lower === 'ai_assistant' || lower === 'ai assistant') return '/assets/avatars/default_ai.png?v=4';
  // FORȚĂM avatarul pentru username 'admin' să ignore orice setare custom anterioară
  if (lower === 'admin') return '/assets/avatars/default_admin.png?v=4';

  if (avatarUrl) return avatarUrl;

  // Verifică dacă e admin (self sau din lista cache) după rol
  const isMeAdmin = state.user && state.user.username === username && (state.user.role === 'admin' || lower === 'admin');
  const cachedUser = Array.isArray(state.users) ? state.users.find(u => u.username === username) : null;
  if (isMeAdmin || (cachedUser && (cachedUser.role === 'admin' || (cachedUser.username || '').toLowerCase() === 'admin'))) {
    return '/assets/avatars/default_admin.png?v=5';
  }

  return '/assets/avatars/default_user.png?v=3';
}

function getConversationAvatar(conv) {
  if (isAIConv(conv)) return '/assets/avatars/default_ai.png?v=3';
  if (conv.isAnnouncement) return '/assets/avatars/default_announcement.png?v=3';
  if (conv.type === 'group') return '/assets/avatars/default_group.png?v=3';

  // Direct: afișăm avatarul partenerului
  const other = conv.members?.find(m => m.id !== state.user.id);
  // Dacă nu găsim partener (ex: ești singur), fallback la user default
  return userAvatarUrl(other?.username || 'User', other?.avatarUrl);
}

function escapeHtml(s) {
  return (s || '')
    .toString()
    .replace(/[&<>"']/g, (c) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    }[c]));
}

/* ========= RegExp utils (fix highlight) ========= */
function escapeRegExp(str = '') {
  let out = '';
  for (let i = 0; i < str.length; i++) {
    const ch = str[i];
    if ('\\^$.*+?()[]{}|'.indexOf(ch) !== -1) out += '\\' + ch;
    else out += ch;
  }
  return out;
}

/* ========= File type utils (IMPROVED image detection) ========= */
function isImageUrl(url = '') {
  try {
    const u = new URL(url, location.origin);
    const p = (u.pathname || '').toLowerCase();
    return /\.(png|jpe?g|jpe|gif|webp|avif|svg|bmp|bpg|tiff?|ico|cur|jfif|heic|heif)$/.test(p);
  } catch {
    const s = (url || '').toString().toLowerCase().split('?')[0];
    return /\.(png|jpe?g|jpe|gif|webp|avif|svg|bmp|bpg|tiff?|ico|cur|jfif|heic|heif)$/.test(s);
  }
}

/* ========= Text caret util ========= */
function insertAtCursor(input, text) {
  if (!input) return;
  const start = input.selectionStart ?? input.value.length;
  const end = input.selectionEnd ?? input.value.length;
  const val = input.value || '';
  input.value = val.slice(0, start) + text + val.slice(end);
  const pos = start + text.length;
  input.selectionStart = input.selectionEnd = pos;
  input.focus();
}

/* ========= UI state persist: secțiuni (collapsed) ========= */
const SECTIONS_KEY = 'conv:sections:collapsed';
function loadSectionsUI() {
  try {
    const s = JSON.parse(localStorage.getItem(SECTIONS_KEY) || '{}');
    state.ui.sections = {
      announcements: !!s.announcements,
      groups: !!s.groups,
      direct: !!s.direct
    };
  } catch {
    state.ui.sections = { announcements: false, groups: false, direct: false };
  }
}
function saveSectionsUI() {
  try { localStorage.setItem(SECTIONS_KEY, JSON.stringify(state.ui.sections || {})); } catch { }
}

/* ========= Time utils (parse corect UTC din SQLite) ========= */
function parseDateUTC(value) {
  if (!value) return new Date();
  if (value instanceof Date) return value;
  if (typeof value === 'number') return new Date(value);
  if (typeof value === 'string') {
    if (value.endsWith('Z') || /[T ]\d{2}:\d{2}:\d{2}(?:\.\d+)?([+-]\d{2}:?\d{2})$/.test(value)) return new Date(value);
    const m = value.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?$/);
    if (m) {
      const [, Y, Mo, D, h, mi, s] = m;
      return new Date(Date.UTC(+Y, +Mo - 1, +D, +h, +mi, +(s || '0')));
    }
  }
  return new Date(value);
}
function tzDayKey(d) {
  const dt = parseDateUTC(d);
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: getTZ(), year: 'numeric', month: '2-digit', day: '2-digit'
  }).format(dt);
}
function isSameDay(a, b) { return tzDayKey(a) === tzDayKey(b); }
function isYesterday(d) {
  const now = new Date();
  const y = new Date(now);
  y.setUTCDate(now.getUTCDate() - 1);
  return tzDayKey(d) === tzDayKey(y);
}
function dayLabel(d) {
  const dk = tzDayKey(d), todayKey = tzDayKey(new Date());
  if (dk === todayKey) return 'Astăzi';
  if (isYesterday(d)) return 'Ieri';
  return new Intl.DateTimeFormat(LOCALE, {
    timeZone: getTZ(), day: '2-digit', month: 'short', year: 'numeric'
  }).format(parseDateUTC(d));
}
function fmtTime(d) {
  return new Intl.DateTimeFormat(LOCALE, {
    timeZone: getTZ(), hour: '2-digit', minute: '2-digit', hour12: false
  }).format(parseDateUTC(d));
}

/* ========= 🟢 Online Status Helpers ========= */
function formatLastSeen(lastSeenDate) {
  if (!lastSeenDate) return 'offline';

  const now = new Date();
  const lastSeen = parseDateUTC(lastSeenDate);
  const diffMs = now - lastSeen;
  const diffMin = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMin < 1) return 'văzut acum';
  if (diffMin === 1) return 'văzut acum 1 min';
  if (diffMin < 60) return `văzut acum ${diffMin} min`;
  if (diffHours === 1) return 'văzut acum 1 oră';
  if (diffHours < 24) return `văzut acum ${diffHours} ore`;
  if (diffDays === 1) return 'văzut ieri';
  if (diffDays < 7) return `văzut acum ${diffDays} zile`;

  return 'văzut ' + new Intl.DateTimeFormat(LOCALE, {
    timeZone: getTZ(), day: '2-digit', month: 'short'
  }).format(lastSeen);
}

/* ========= Reacții (cache opțional) ========= */
const reactionCache = new Map(); // messageId -> ...

/* ========= Unread helpers ========= */
function getUnread(convId) { return state.unread[convId] || 0; }
function bumpUnread(convId, n = 1) { state.unread[convId] = (state.unread[convId] || 0) + n; }
function clearUnread(convId) { if (state.unread[convId]) delete state.unread[convId]; }

/* ========= Sender helpers ========= */
function resolveSender(msg) {
  if (msg.sender && msg.sender.id) return msg.sender;
  const conv = state.conversations.find(c => c.id === msg.conversationId);
  const fromConv = conv?.members?.find(m => m.id === msg.senderId);
  if (fromConv) return fromConv;
  if (msg.senderId === state.user?.id) return state.user;
  return { id: msg.senderId, username: `user-${msg.senderId}`, avatarUrl: null };
}

/* ========= Token helpers ========= */
function saveToken(token) { localStorage.setItem('token', token); state.token = token; }
function loadToken() { const t = localStorage.getItem('token'); if (t) state.token = t; }
function clearToken() { localStorage.removeItem('token'); state.token = null; }

/* ========= API helpers ========= */
async function api(path, opts = {}) {
  const res = await fetch(path, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
      ...(opts.headers || {})
    }
  });
  if (!res.ok) {
    let err = null;
    try { err = await res.json(); } catch { }
    throw new Error(err?.error || `HTTP ${res.status}`);
  }
  try { return await res.json(); } catch { return { ok: true }; }
}
function authFetch(path, opts = {}) {
  return fetch(path, {
    ...opts,
    headers: {
      ...(opts.headers || {}),
      ...(state.token ? { Authorization: `Bearer ${state.token}` } : {})
    }
  });
}
function fileUpload(path, file) {
  const form = new FormData();
  form.append('file', file);
  return fetch(path, {
    method: 'POST',
    headers: { ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}) },
    body: form
  }).then(async (r) => {
    if (!r.ok) {
      let e = null; try { e = await r.json(); } catch { }
      throw new Error(e?.error || `Upload HTTP ${r.status}`);
    }
    return r.json();
  });
}

/* ========= Business helpers ========= */
const AI_USERNAME = 'ai_assistant';
const AI_LABEL = 'AI Assistant';
function isAdmin() {
  if (!state.user) return false;
  return (state.user.role || 'user') === 'admin' || (state.user.username || '').toLowerCase() === 'admin';
}

/* helper pentru breakpoint mobil (folosit la panoul din dreapta) */
function isMobile() {
  return window.matchMedia && window.matchMedia('(max-width: 700px)').matches;
}

function isAIConv(conv) {
  return conv?.type === 'direct' && Array.isArray(conv?.members) && conv.members.some(u => u.username === AI_USERNAME);
}
function formatConvTitle(conv) {
  if (conv.type === 'group') {
    if (conv.name) return conv.name;
    // Dacă e grup fără nume, afișăm membrii (fără mine)
    const others = (conv.members || []).filter(m => m.id !== state.user.id).map(m => m.username);
    if (others.length === 0) return 'Grup';
    return others.join(', ');
  }

  // Pentru conversații directe
  const members = conv.members || [];

  // Verifică dacă adminul observă o conversație în care NU e membru
  if (isAdmin() && conv.isObserving && members.length >= 2) {
    // Afișează ambii utilizatori ca @user1 & @user2
    const names = members.map(m => '@' + m.username).join(' & ');
    return names;
  }

  // Comportamentul normal: arată celălalt utilizator
  const other = members.find(m => m.id !== state.user.id);
  if (!other) return 'Direct';
  return other.username === AI_USERNAME ? AI_LABEL : other.username;
}

/* ========= UI shell ========= */
function setAuthVisible(vis) {
  $('#auth').classList.toggle('hidden', !vis);
  $('#app').classList.toggle('hidden', vis);
  $('#auth')?.setAttribute('aria-hidden', vis ? 'false' : 'true');
  $('#app')?.setAttribute('aria-hidden', vis ? 'true' : 'false');
}

/* ========= Auth UI ========= */
function setupAuth() {
  $('#tab-login').addEventListener('click', () => {
    $('#tab-login').classList.add('active');
    $('#tab-register').classList.remove('active');
    $('#tab-login').setAttribute('aria-selected', 'true');
    $('#tab-register').setAttribute('aria-selected', 'false');
    $('#login-form').classList.remove('hidden');
    $('#register-form').classList.add('hidden');
  });
  $('#tab-register').addEventListener('click', () => {
    $('#tab-register').classList.add('active');
    $('#tab-login').classList.remove('active');
    $('#tab-register').setAttribute('aria-selected', 'true');
    $('#tab-login').setAttribute('aria-selected', 'false');
    $('#register-form').classList.remove('hidden');
    $('#login-form').classList.add('hidden');
  });

  $('#btn-login').addEventListener('click', async () => {
    const username = $('#login-username').value.trim();
    const password = $('#login-password').value;
    $('#login-error').textContent = '';
    try {
      const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) });
      saveToken(data.token); await initApp();
    } catch (e) { $('#login-error').textContent = e.message; }
  });

  $('#btn-register').addEventListener('click', async () => {
    const username = $('#register-username').value.trim();
    const password = $('#register-password').value;
    $('#register-error').textContent = '';
    try {
      const data = await api('/api/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) });
      saveToken(data.token); await initApp();
    } catch (e) { $('#register-error').textContent = e.message; }
  });

  // === NOU: Enter pentru submit ===

  // Login: Enter în oricare input -> click pe "Intră"
  $$('#login-form input').forEach(inp => {
    inp.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        $('#btn-login').click();
      }
    });
  });

  // Register: Enter în oricare input -> click pe "Creează cont"
  $$('#register-form input').forEach(inp => {
    inp.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        $('#btn-register').click();
      }
    });
  });
}

/* ========= Socket ========= */
function connectSocket() {
  if (state.socket) state.socket.disconnect();
  state.socket = io({ auth: { token: state.token } });

  state.socket.on('connect', () => { });

  // Mesaj nou — cache + UI + UNREAD (doar când conv nu e deschisă)
  state.socket.on('message:new', (msg) => {
    touchConversationFromMessage(msg);

    if (msg.conversationId === state.currentConvId) {
      if (document.querySelector(`[data-msg="${msg.id}"]`)) return; // dedup
      cacheMessage(msg.conversationId, msg);
      const wasAtBottom = isAtBottom();
      appendMessageWithSeparator(msg);
      if (wasAtBottom || msg.senderId === state.user.id) scrollMessagesToEnd(true);
      if (state.ui.msgSearchOpen && state.ui.msgSearchQuery) applyMessageSearchHighlight(state.ui.msgSearchQuery);
      if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
    } else {
      cacheMessage(msg.conversationId, msg);
      if (msg.senderId !== state.user.id) {
        bumpUnread(msg.conversationId);
        renderConversations();
      }
    }
  });

  state.socket.on('message:updated', (msg) => {
    touchConversationFromMessage(msg, /*bumpFirst=*/false);
    renderConversations();

    if (msg.conversationId !== state.currentConvId) return;
    const el = document.querySelector(`[data-msg="${msg.id}"]`);
    if (el) {
      const contentEl = el.querySelector('.content');
      contentEl.innerHTML = buildMessageContentHTML(msg);
      // dataset.raw și pentru file (caption)
      contentEl.dataset.raw = (!msg.isDeleted && (msg.type === 'text' || msg.type === 'file')) ? (msg.content || '') : '';

      // Re-atașează fallbackul de imagine dacă e cazul
      attachImageFallback(el, msg);

      const meta = el.querySelector('.meta');
      meta.textContent = `${fmtTime(msg.createdAt)}${msg.editedAt ? ' • editat' : ''}`;
      if (state.ui.msgSearchOpen && state.ui.msgSearchQuery) applyMessageSearchHighlight(state.ui.msgSearchQuery);
      updateCachedMessage(msg);
      if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
    }
  });

  // Ștergere (toți)
  state.socket.on('message:deleted', ({ id, conversationId }) => {
    if (conversationId !== state.currentConvId) {
      const conv = state.conversations.find(c => c.id === conversationId);
      if (conv?.lastMessage?.id === id) fetchConversations();
      return;
    }
    const row = document.querySelector(`[data-msg="${id}"]`)?.closest('.msg-row');
    if (row) row.remove();
    removeFromCache(id, conversationId);
    normalizeSeparators();

    const conv = state.conversations.find(c => c.id === conversationId);
    if (conv?.lastMessage?.id === id) fetchConversations();

    if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
  });

  // Ascundere locală
  state.socket.on('message:hidden', ({ id, conversationId }) => {
    if (conversationId !== state.currentConvId) return;
    document.querySelector(`[data-msg="${id}"]`)?.closest('.msg-row')?.remove();
    removeFromCache(id, conversationId);
    normalizeSeparators();
    if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
  });

  // Reacții
  state.socket.on('reaction:updated', ({ messageId, conversationId, reactions }) => {
    const el = document.querySelector(`[data-msg="${messageId}"]`);
    if (!el) return;
    const reactDiv = el.querySelector('.reactions');
    reactDiv.innerHTML = reactions.map(r => `<span class="reaction ${r.reactedByMe ? 'me' : ''}" data-emoji="${r.emoji}">${r.emoji} ${r.count}</span>`).join('');
    reactDiv.querySelectorAll('.reaction').forEach(btn => {
      btn.addEventListener('click', () => toggleReaction(messageId, btn.getAttribute('data-emoji')));
    });
    reactionCache.delete(messageId);
  });

  // Convorbiri noi
  state.socket.on('conversation:new', (conv) => {
    upsertConversationToTop(conv);
    if (conv?.id) state.socket.emit('join', { conversationId: conv.id });
    if (conv.lastMessage && conv.lastMessage.senderId && conv.lastMessage.senderId !== state.user.id) {
      bumpUnread(conv.id);
    }
    renderConversations();
  });

  // Fallback conv:updated
  state.socket.on('conversation:updated', (conv) => {
    upsertConversationToTop(conv);
    if (conv?.id) state.socket.emit('join', { conversationId: conv.id });

    if (conv.id === state.currentConvId && conv.lastMessage && !conv.lastMessage.isDeleted) {
      const existsInDom = document.querySelector(`[data-msg="${conv.lastMessage.id}"]`);
      const existsInCache = (state.messages.get(conv.id) || []).some(m => m.id === conv.lastMessage.id);
      if (!existsInDom && !existsInCache) {
        cacheMessage(conv.id, conv.lastMessage);
        const wasAtBottom = isAtBottom();
        appendMessageWithSeparator(conv.lastMessage);
        if (wasAtBottom || conv.lastMessage.senderId === state.user.id) scrollMessagesToEnd(true);
        if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
      }
    }
    renderConversations();
  });

  // Typing
  state.socket.on('typing', ({ conversationId, userId, isTyping }) => {
    if (conversationId === state.currentConvId && userId !== state.user.id) {
      const hdr = $('#chat-header');
      hdr.querySelector('.typing')?.remove();
      if (isTyping) {
        const span = document.createElement('span');
        span.className = 'typing badge';
        span.textContent = 'scrie...';
        hdr.appendChild(span);
      }
    }
  });

  // Clear & Delete conversație
  state.socket.on('conversation:cleared', ({ conversationId }) => {
    if (conversationId === state.currentConvId) {
      $('#messages').innerHTML = '';
      state.messages.set(conversationId, []);
      if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
    }
  });
  state.socket.on('conversation:deleted', ({ conversationId }) => {
    const idx = state.conversations.findIndex(c => c.id === conversationId);
    if (idx >= 0) state.conversations.splice(idx, 1);
    delete state.unread[conversationId];
    renderConversations();
    if (state.currentConvId === conversationId) {
      state.currentConvId = null;
      $('#chat-header').innerHTML = '';
      $('#messages').innerHTML = '';
      toggleInfoPanel(false);
    }
  });

  // Admin: forțează delogarea
  state.socket.on('force:logout', ({ reason }) => {
    try { alert(reason || 'Sesiunea a fost închisă.'); } catch { }
    clearToken();
    try { state.socket?.disconnect(); } catch { }
    state.user = null;
    setAuthVisible(true);
    $('#chat-header').innerHTML = '';
    $('#messages').innerHTML = '';
    $('#conversations').innerHTML = '';
  });

  // ═══════════════════════════════════════════════════════════════════════════
  //                          🟢 ONLINE STATUS TRACKING
  // ═══════════════════════════════════════════════════════════════════════════

  // Primită la conectare: lista completă de useri online
  state.socket.on('users:online:list', ({ userIds }) => {
    state.onlineUsers = new Set(userIds || []);
    renderConversations();
    // Update chat header if DM is open
    if (state.currentConvId) {
      const conv = state.conversations.find(c => c.id === state.currentConvId);
      if (conv?.type === 'direct') renderChatHeader(conv);
    }
  });

  // Un user a intrat online
  state.socket.on('user:online', ({ userId }) => {
    state.onlineUsers.add(userId);
    renderConversations();
    // Update chat header if this user is in the current DM
    if (state.currentConvId) {
      const conv = state.conversations.find(c => c.id === state.currentConvId);
      if (conv?.type === 'direct') {
        const other = conv.members?.find(m => m.id !== state.user.id);
        if (other?.id === userId) renderChatHeader(conv);
      }
    }
  });

  // Un user a ieșit offline
  state.socket.on('user:offline', ({ userId }) => {
    state.onlineUsers.delete(userId);
    renderConversations();
    // Update chat header if this user is in the current DM
    if (state.currentConvId) {
      const conv = state.conversations.find(c => c.id === state.currentConvId);
      if (conv?.type === 'direct') {
        const other = conv.members?.find(m => m.id !== state.user.id);
        if (other?.id === userId) renderChatHeader(conv);
      }
    }
  });

  // Sincronizare profil utilizator în timp real
  state.socket.on('user:updated', (updatedUser) => {
    if (!updatedUser?.id) return;

    // Actualizează în lista de utilizatori
    const userIdx = state.users.findIndex(u => u.id === updatedUser.id);
    if (userIdx >= 0) {
      state.users[userIdx] = { ...state.users[userIdx], ...updatedUser };
    }

    // Actualizează membrii în conversații
    for (const conv of state.conversations) {
      if (!conv.members) continue;
      const memberIdx = conv.members.findIndex(m => m.id === updatedUser.id);
      if (memberIdx >= 0) {
        conv.members[memberIdx] = { ...conv.members[memberIdx], ...updatedUser };
      }
    }

    // Re-render conversații (pentru a reflecta noul username/avatar în DMs)
    renderConversations();

    // Actualizează mesajele afișate dacă sunt de la acest utilizator
    if (state.currentConvId) {
      const msgs = state.messages.get(state.currentConvId) || [];
      let needsRerender = false;
      for (const msg of msgs) {
        if (msg.sender?.id === updatedUser.id || msg.senderId === updatedUser.id) {
          if (msg.sender) {
            msg.sender = { ...msg.sender, ...updatedUser };
          }
          needsRerender = true;
        }
      }
      if (needsRerender) {
        renderMessagesList(msgs);
      }

      // Actualizează header-ul chat-ului dacă e DM cu acest utilizator
      const conv = state.conversations.find(c => c.id === state.currentConvId);
      if (conv?.type === 'direct') {
        const other = conv.members?.find(m => m.id !== state.user.id);
        if (other?.id === updatedUser.id) {
          renderChatHeader(conv);
        }
      }
    }

    // Actualizează listele din modale
    renderUsersList();
    renderGroupUsersList();
    renderAnnounceUsersList();

    // Actualizează panoul de info dacă e deschis
    if (state.ui.rightOpen && state.currentConvId) {
      renderInfoPanel(state.currentConvId, true);
    }
  });
}
/* ========= Conversations helpers ========= */
function touchConversationFromMessage(msg, bumpFirst = true) {
  const idx = state.conversations.findIndex(c => c.id === msg.conversationId);
  if (idx >= 0) {
    const conv = state.conversations[idx];
    conv.lastMessage = {
      id: msg.id,
      conversationId: msg.conversationId,
      senderId: msg.senderId,
      content: msg.content,
      type: msg.type,
      isDeleted: !!msg.isDeleted,
      createdAt: msg.createdAt
    };
    if (bumpFirst) {
      const [it] = state.conversations.splice(idx, 1);
      state.conversations.unshift(it);
    }
  } else {
    fetchConversations();
  }
  renderConversations();
}
function upsertConversationToTop(conv) {
  const idx = state.conversations.findIndex(c => c.id === conv.id);
  if (idx >= 0) {
    const [it] = state.conversations.splice(idx, 1);
    state.conversations.unshift({ ...it, ...conv });
  } else {
    state.conversations.unshift(conv);
  }
}
function upsertConversation(conv) {
  const idx = state.conversations.findIndex(c => c.id === conv.id);
  if (idx >= 0) state.conversations[idx] = conv;
  else state.conversations.unshift(conv);
}

/* ========= App init ========= */
async function initApp() {
  try {
    const me = await api('/api/me');
    state.user = me.user;
    $('#me-name').textContent = state.user.username + (isAdmin() ? ' (admin)' : '');
    $('#avatar').src = userAvatarUrl(state.user.username, state.user.avatarUrl);

    // Show DB viewer button only for admins
    const dbBtn = $('#btn-db-viewer');
    if (dbBtn) {
      if (isAdmin()) {
        dbBtn.classList.remove('hidden');
      } else {
        dbBtn.classList.add('hidden');
      }
    }

    setAuthVisible(false);
    loadSectionsUI();

    connectSocket();
    await Promise.all([fetchConversations(), fetchUsers()]);
    renderConversations();
    bindUI();
    ensureComposerBindings();

    // Deschide automat conversația "General" (sau prima conversație disponibilă)
    const generalConv = state.conversations.find(
      c => c.type === 'group'
        && !c.isAnnouncement
        && (c.name || '').toLowerCase() === 'general'
    );
    const firstConv = generalConv || state.conversations[0];
    if (firstConv) {
      await openConversation(firstConv.id);
    }
  } catch (e) {
    console.error(e);
    clearToken();
    setAuthVisible(true);
  }
}

async function fetchConversations() {
  const data = await api('/api/conversations');
  state.conversations = data.conversations || [];
  renderConversations();
}
async function fetchUsers() {
  const data = await api('/api/users');
  state.users = data.users || [];
  renderUsersList();
  renderGroupUsersList();
  renderAnnounceUsersList();
}

/* ========= AI front helpers ========= */
async function startAIConversation() {
  try {
    const data = await api('/api/ai/start', { method: 'POST' });
    upsertConversationToTop(data.conversation);
    renderConversations();
    await openConversation(data.conversation.id);
  } catch (e) {
    alert(e.message || 'AI indisponibil momentan.');
  }
}
function injectAIButton() {
  const actions = document.querySelector('header .actions');
  if (!actions || document.getElementById('btn-ai')) return;
  const btn = document.createElement('button');
  btn.id = 'btn-ai';
  btn.title = 'AI Assistant';
  btn.textContent = '🤖 AI';
  btn.addEventListener('click', startAIConversation);
  actions.insertBefore(btn, actions.firstChild);
}

/* ========= Helpers: modale Fișiere și Setări ========= */
const TZ_OPTS = [
  'Europe/Chisinau', 'Europe/Bucharest', 'Europe/Kyiv', 'Europe/Moscow', 'Europe/London', 'Europe/Berlin', 'Europe/Paris',
  'Europe/Madrid', 'Europe/Rome', 'Europe/Athens', 'Africa/Cairo',
  'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles', 'America/Sao_Paulo',
  'Asia/Dubai', 'Asia/Almaty', 'Asia/Kolkata', 'Asia/Shanghai', 'Asia/Tokyo', 'Australia/Sydney', 'UTC'
];
function ensureFilesModal() {
  if ($('#modal-files')) return;
  const wrap = document.createElement('div');
  wrap.id = 'modal-files';
  wrap.className = 'modal hidden';
  wrap.setAttribute('role', 'dialog');
  wrap.setAttribute('aria-modal', 'true');
  wrap.setAttribute('aria-labelledby', 'modal-files-title');
  wrap.innerHTML = `
    <div class="modal-content" style="display:flex; flex-direction:column; max-height:80vh; overflow:hidden;">
      <h3 id="modal-files-title" style="flex-shrink:0;">Fișiere recente</h3>
      <div id="files-list" class="list" style="flex:1; overflow-y:auto; padding-right:4px; margin-bottom:0;"></div>
      <div class="modal-actions" style="flex-shrink:0; border-top:1px solid var(--glass-stroke); padding-top:12px; margin-top:0; justify-content:flex-end;">
        <button class="secondary" data-close="#modal-files" style="width:100%;">Închide</button>
      </div>
    </div>
  `;
  document.body.appendChild(wrap);
  wrap.querySelector('[data-close]')?.addEventListener('click', () => closeModal('#modal-files'));
}
function ensureSettingsModal() {
  if ($('#modal-settings')) return;
  const wrap = document.createElement('div');
  wrap.id = 'modal-settings';
  wrap.className = 'modal hidden';
  wrap.setAttribute('role', 'dialog');
  wrap.setAttribute('aria-modal', 'true');
  wrap.setAttribute('aria-labelledby', 'modal-settings-title');
  const browserTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  const currentTz = getTZ();
  const options = [`<option value="${browserTz}">Detectează automat (${browserTz})</option>`]
    .concat(TZ_OPTS.map(tz => `<option value="${tz}">${tz}</option>`)).join('');
  wrap.innerHTML = `
    <div class="modal-content">
      <h3 id="modal-settings-title">Setări</h3>
      <div class="list">
        <div class="row" style="gap:12px;align-items:center;">
          <div style="min-width:120px;">Fus orar</div>
          <select id="tz-select" style="flex:1;padding:10px;border-radius:12px;border:1px solid var(--glass-stroke);background:#0b1220;color:var(--text);">
            ${options}
          </select>
        </div>
      </div>
      <div class="modal-actions">
        <button class="secondary" data-close="#modal-settings">Închide</button>
      </div>
    </div>
  `;
  document.body.appendChild(wrap);
  const select = wrap.querySelector('#tz-select');
  if (select) select.value = currentTz;
  select?.addEventListener('change', () => {
    const tz = select.value;
    setTZ(tz);
    renderConversations();
    if (state.currentConvId) {
      const cached = state.messages.get(state.currentConvId) || [];
      renderMessagesList(cached);
    }
  });
  wrap.querySelector('[data-close]')?.addEventListener('click', () => closeModal('#modal-settings'));
}

/* ========= Emoji picker (composer principal, clamped în <main>) ========= */
function setupEmojiPicker() {
  const btn = document.getElementById('emoji-btn');
  let panel = document.getElementById('emoji-panel');
  if (!btn || !panel) return;

  if (panel.parentElement !== document.body) document.body.appendChild(panel);

  const EMOJIS = ['😀', '😁', '😂', '🤣', '😊', '😍', '😎', '🤔', '😅', '🙌', '👍', '👏', '🙏', '🔥', '🎉', '💯', '💡', '🤝', '🫶', '✨', '📎', '🖼️'];

  function build() {
    if (panel.dataset.ready === '1') return;
    panel.innerHTML = EMOJIS.map(e => `<button type="button" data-emo="${e}" aria-label="${e}">${e}</button>`).join('');
    panel.dataset.ready = '1';
    panel.querySelectorAll('button').forEach(b => {
      b.addEventListener('click', () => {
        const input = document.getElementById('msg-input');
        input.value += b.dataset.emo;
        input.focus();
      });
    });
  }

  function position() {
    panel.classList.remove('hidden');
    panel.style.visibility = 'hidden';

    const r = btn.getBoundingClientRect();
    const host = document.querySelector('main') || document.body;
    const cRect = host.getBoundingClientRect ? host.getBoundingClientRect() : { top: 0, left: 0, right: window.innerWidth, bottom: window.innerHeight };

    const pW = panel.offsetWidth || 260;
    const pH = panel.offsetHeight || 180;
    const margin = 10;

    const topAbove = r.top - pH - 8;
    const topBelow = r.bottom + 8;
    const minTop = cRect.top + margin;
    const maxTop = cRect.bottom - pH - margin;
    let top = topAbove >= minTop ? topAbove : Math.min(Math.max(topBelow, minTop), maxTop);

    const minLeft = cRect.left + margin;
    const maxLeft = cRect.right - pW - margin;
    let left = r.left + (r.width / 2) - (pW / 2);
    if (left < minLeft) left = minLeft;
    if (left > maxLeft) left = Math.max(minLeft, maxLeft);

    panel.style.position = 'fixed';
    panel.style.left = `${left}px`;
    panel.style.top = `${top}px`;
    panel.style.zIndex = '4000';
    panel.style.visibility = 'visible';
  }

  let open = false;
  function openPanel() {
    build();
    open = true;
    btn.setAttribute('aria-expanded', 'true');
    panel.classList.remove('hidden');
    position();
    document.addEventListener('click', onDocClick, true);
    window.addEventListener('resize', position);
    window.addEventListener('scroll', position, true);
  }
  function closePanel() {
    open = false;
    btn.setAttribute('aria-expanded', 'false');
    panel.classList.add('hidden');
    document.removeEventListener('click', onDocClick, true);
    window.removeEventListener('resize', position);
    window.removeEventListener('scroll', position, true);
  }
  function onDocClick(e) { if (!e.target.closest('#emoji-panel') && !e.target.closest('#emoji-btn')) closePanel(); }

  btn.addEventListener('click', (e) => { e.preventDefault(); e.stopPropagation(); open ? closePanel() : openPanel(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && open) closePanel(); });
  document.addEventListener('conversation:changed', () => { if (open) closePanel(); });
}

/* ========= Announcement modal: emoji + preview atașament ========= */
function initAnnouncementModal() {
  const modal = $('#modal-announcement');
  if (!modal) return;

  const ta = $('#ann-text');
  if (!ta) return;

  // 1) Asigură wrapper pentru textarea
  let wrap = ta.closest('.ann-input-wrap');
  if (!wrap) {
    wrap = document.createElement('div');
    wrap.className = 'ann-input-wrap';
    ta.parentNode.insertBefore(wrap, ta);
    wrap.appendChild(ta);
  }

  // 2) Asigură butonul emoji
  let emoBtn = $('#ann-emoji-btn');
  if (!emoBtn) {
    emoBtn = document.createElement('button');
    emoBtn.id = 'ann-emoji-btn';
    emoBtn.className = 'btn-icon';
    emoBtn.type = 'button';
    emoBtn.title = 'Emoji';
    emoBtn.setAttribute('aria-label', 'Emoji');
    emoBtn.textContent = '😀';
    emoBtn.style.position = 'absolute';
    emoBtn.style.right = '8px';
    emoBtn.style.bottom = '8px';
    wrap.appendChild(emoBtn);
  }

  // 3) Asigură panoul emoji (și mută-l în body ca să nu creeze scroll în modal)
  let emoPanel = $('#ann-emoji-panel');
  if (!emoPanel) {
    emoPanel = document.createElement('div');
    emoPanel.id = 'ann-emoji-panel';
    emoPanel.className = 'emoji-panel hidden';
    emoPanel.setAttribute('role', 'dialog');
    emoPanel.setAttribute('aria-label', 'Alege emoji pentru anunț');
  }
  if (emoPanel.parentElement !== document.body) {
    document.body.appendChild(emoPanel);
  }

  // 4) Leagă picker-ul dacă nu e deja legat (dataset.ready protejează de dublare)
  if (!emoBtn.dataset.ready) {
    setupAnnEmojiPicker(emoBtn, emoPanel, ta);
    emoBtn.dataset.ready = '1';
  }

  // 5) Asigură containerul de preview (după lista de utilizatori)
  if (!$('#ann-preview')) {
    const prev = document.createElement('div');
    prev.id = 'ann-preview';
    prev.className = 'ann-preview hidden';
    prev.setAttribute('aria-live', 'polite');
    const usersList = $('#ann-users');
    if (usersList) usersList.insertAdjacentElement('afterend', prev);
  }

  // 6) Bind change pe input fișier (o singură dată)
  const fileInput = $('#ann-file');
  if (fileInput && !fileInput.dataset.ready) {
    fileInput.addEventListener('change', (e) => {
      const file = e.target.files && e.target.files[0];
      updateAnnPreview(file);
    });
    fileInput.dataset.ready = '1';
  }
}
function setupAnnEmojiPicker(btn, panel, textarea) {
  const EMOJIS = ['😀', '😁', '😂', '🤣', '😊', '😍', '😎', '🤔', '😅', '🙌', '👍', '👏', '🙏', '🔥', '🎉', '💯', '💡', '🤝', '🫶', '✨', '😢', '😮', '😴'];

  function build() {
    if (panel.dataset.ready === '1') return;
    panel.innerHTML = EMOJIS.map(e => `<button type="button" data-emo="${e}" aria-label="${e}">${e}</button>`).join('');
    panel.dataset.ready = '1';
    panel.querySelectorAll('button').forEach(b => {
      b.addEventListener('click', () => { insertAtCursor(textarea, b.dataset.emo); });
    });
  }

  function position() {
    panel.classList.remove('hidden');
    panel.style.visibility = 'hidden';
    const r = btn.getBoundingClientRect();
    const pW = panel.offsetWidth || 260;
    const pH = panel.offsetHeight || 180;
    const margin = 10;
    const vw = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    const vh = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);

    const topBelow = r.bottom + 8;
    const topAbove = r.top - pH - 8;
    let top = topBelow;
    if (topBelow + pH > vh - margin && topAbove > margin) top = topAbove;
    let left = r.left + (r.width / 2) - (pW / 2);
    left = Math.max(margin, Math.min(left, vw - pW - margin));

    panel.style.position = 'fixed';
    panel.style.left = `${left}px`;
    panel.style.top = `${top}px`;
    panel.style.zIndex = '9001';
    panel.style.visibility = 'visible';
  }

  let open = false;
  function openPanel() {
    build(); open = true;
    btn.setAttribute('aria-expanded', 'true');
    panel.classList.remove('hidden');
    position();
    document.addEventListener('click', onDocClick, true);
    window.addEventListener('resize', position);
    window.addEventListener('scroll', position, true);
  }
  function closePanel() {
    open = false;
    btn.setAttribute('aria-expanded', 'false');
    panel.classList.add('hidden');
    document.removeEventListener('click', onDocClick, true);
    window.removeEventListener('resize', position);
    window.removeEventListener('scroll', position, true);
  }
  function onDocClick(e) {
    if (panel.contains(e.target) || btn.contains(e.target)) return;
    closePanel();
  }

  btn.addEventListener('click', (e) => {
    e.preventDefault(); e.stopPropagation();
    open ? closePanel() : openPanel();
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && open) closePanel(); });
}

/* ========= Preview atașament (anunț) ========= */
function updateAnnPreview(file) {
  const prev = $('#ann-preview');
  if (!prev) return;
  prev.innerHTML = '';
  if (!file) { prev.classList.add('hidden'); return; }

  const isImg = /^image\//i.test(file.type) || /\.(png|jpe?g|jpe|gif|webp|avif|svg|bmp|tiff?|jfif|heic|heif)$/i.test(file.name);
  const url = isImg ? URL.createObjectURL(file) : null;

  const thumb = isImg ? `<img src="${url}" alt="">` : `<div class="file-chip">📎</div>`;
  const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
  const meta = `
    <div class="meta">
      <div class="name">${escapeHtml(file.name)}</div>
      <div class="hint">${sizeMB} MB</div>
    </div>`;

  prev.innerHTML = thumb + meta;
  const remove = document.createElement('button');
  remove.type = 'button';
  remove.className = 'ann-remove';
  remove.textContent = 'Șterge';
  remove.addEventListener('click', () => {
    const input = $('#ann-file');
    if (input) input.value = '';
    if (isImg && url) URL.revokeObjectURL(url);
    prev.classList.add('hidden');
    prev.innerHTML = '';
  });
  prev.appendChild(remove);
  prev.classList.remove('hidden');
}
function resetAnnouncementForm() {
  const title = $('#ann-title'); if (title) title.value = '';
  const text = $('#ann-text'); if (text) text.value = '';
  const all = $('#an n-select-all'); if (all) all.checked = false;
  const file = $('#ann-file'); if (file) file.value = '';
  const prev = $('#ann-preview'); if (prev) { prev.classList.add('hidden'); prev.innerHTML = ''; }
  $$('#ann-users input[type=checkbox]').forEach(cb => { cb.checked = false; cb.disabled = false; });
}

/* ========= UI bindings ========= */
function bindUI() {
  // logout
  $('#btn-logout').addEventListener('click', () => {
    clearToken();
    state.socket?.disconnect();
    setAuthVisible(true);
  });

  // modale existente
  $('#btn-users').addEventListener('click', () => openModal('#modal-users'));
  $('#btn-new-group').addEventListener('click', () => openModal('#modal-group'));
  $('#btn-new-announcement').addEventListener('click', () => { openModal('#modal-announcement'); renderAnnounceUsersList(); initAnnouncementModal(); });
  // NOU: butoane din drawer (mobil) care fac același lucru
  const drawerToggle = $('#drawer');

  $('#drawer-users-btn')?.addEventListener('click', () => {
    openModal('#modal-users');
    if (drawerToggle) drawerToggle.checked = false; // închide meniul burger
  });

  $('#drawer-group-btn')?.addEventListener('click', () => {
    openModal('#modal-group');
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#drawer-ann-btn')?.addEventListener('click', () => {
    openModal('#modal-announcement');
    renderAnnounceUsersList();
    initAnnouncementModal();
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#drawer-files-btn')?.addEventListener('click', () => {
    openFilesModal();
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#drawer-settings-btn')?.addEventListener('click', () => {
    ensureSettingsModal();
    openModal('#modal-settings');
    if (drawerToggle) drawerToggle.checked = false;
  });

  // NOU: butoane din tablet menu (același comportament ca drawer)
  $('#tablet-users-btn')?.addEventListener('click', () => {
    openModal('#modal-users');
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#tablet-group-btn')?.addEventListener('click', () => {
    openModal('#modal-group');
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#tablet-ann-btn')?.addEventListener('click', () => {
    openModal('#modal-announcement');
    renderAnnounceUsersList();
    initAnnouncementModal();
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#tablet-files-btn')?.addEventListener('click', () => {
    openFilesModal();
    if (drawerToggle) drawerToggle.checked = false;
  });

  $('#tablet-settings-btn')?.addEventListener('click', () => {
    ensureSettingsModal();
    openModal('#modal-settings');
    if (drawerToggle) drawerToggle.checked = false;
  });

  document.querySelector('.tablet-close-btn')?.addEventListener('click', () => {
    if (drawerToggle) drawerToggle.checked = false;
  });
  $('#users-search').addEventListener('input', async (e) => {
    const q = e.target.value.trim();
    const data = await api('/api/users?q=' + encodeURIComponent(q));
    state.users = data.users;
    renderUsersList(); renderGroupUsersList(); renderAnnounceUsersList();
  });

  $('#create-group-btn').addEventListener('click', async () => {
    const name = $('#group-name').value.trim();
    const selected = $$('#modal-group input[type=checkbox]:checked').map(cb => Number(cb.value));
    if (!name || selected.length === 0) { alert('Alege un nume și cel puțin un membru'); return; }
    try {
      const data = await api('/api/conversations', { method: 'POST', body: JSON.stringify({ name, memberIds: selected }) });
      upsertConversationToTop(data.conversation); renderConversations(); closeModal('#modal-group');
    } catch (e) { openAlertModal(e.message); }
  });

  // Handler pentru click pe textul "Trimite către toți"
  $('#ann-select-all-wrap')?.addEventListener('click', (e) => {
    if (e.target.tagName === 'INPUT') return;
    const chk = $('#ann-select-all');
    if (chk) {
      chk.checked = !chk.checked;
      chk.dispatchEvent(new Event('change'));
    }
  });

  $('#ann-select-all').addEventListener('change', (e) => {
    const checked = e.target.checked;
    $('#ann-users').style.opacity = checked ? 0.5 : 1;
    $$('#ann-users input[type=checkbox]').forEach(cb => { cb.disabled = checked; if (checked) cb.checked = false; });
  });

  $('#create-announcement-btn').addEventListener('click', async () => {
    const title = $('#ann-title').value.trim();
    const text = $('#ann-text').value.trim();
    const fileInput = $('#ann-file');
    const file = fileInput.files[0];
    const sendToAll = $('#ann-select-all').checked;

    if (!title) { openAlertModal('Titlul este obligatoriu.'); return; }
    if (!text && !file) { openAlertModal('Te rog adaugă un text sau un fișier.'); return; }

    const userIds = [];
    try {
      let fileUrl = null;
      if (file) { const up = await fileUpload('/api/upload', file); fileUrl = up.fileUrl; }
      const recipientIds = sendToAll ? 'all' : $$('#ann-users input[type=checkbox]:checked').map(cb => Number(cb.value));
      if (!sendToAll && recipientIds.length === 0) { openAlertModal('Alege cel puțin un destinatar sau bifează "toți".'); return; }
      const data = await api('/api/announcements', { method: 'POST', body: JSON.stringify({ title, content: text, recipientIds, fileUrl }) });
      upsertConversationToTop(data.conversation); renderConversations();
      resetAnnouncementForm();
      closeModal('#modal-announcement');
      openConversation(data.conversation.id);
    } catch (e) { openAlertModal(e.message); }
  });

  // Rail
  $('#search-conv').addEventListener('input', renderConversations);
  const railBtns = $$('#rail .icon-btn');
  railBtns[2]?.removeAttribute('disabled'); railBtns[3]?.removeAttribute('disabled');
  railBtns[2]?.classList.remove('disabled'); railBtns[3]?.classList.remove('disabled');
  railBtns.forEach((btn, idx) => {
    btn.addEventListener('click', async () => {
      railBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      if (idx === 0) { state.ui.filter = 'all'; renderConversations(); }
      else if (idx === 1) { state.ui.filter = 'group'; renderConversations(); }
      else if (idx === 2) { await openFilesModal(); }
      else if (idx === 3) { ensureSettingsModal(); openModal('#modal-settings'); }
    });
  });

  // Composer
  $('#send-btn').addEventListener('click', async () => { await sendMessage(); scrollMessagesToEnd(true); });
  $('#msg-input').addEventListener('keydown', async (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); await sendMessage(); scrollMessagesToEnd(true); } else { sendTyping(true); } });
  $('#msg-input').addEventListener('blur', () => sendTyping(false));
  $('#file-btn').addEventListener('click', () => $('#file-input').click());
  $('#file-input').addEventListener('change', handleFileInput);

  // Drag & drop fișiere în zona mesajelor
  const msgArea = $('#messages');
  msgArea.addEventListener('dragover', (e) => { e.preventDefault(); });
  msgArea.addEventListener('drop', async (e) => { e.preventDefault(); const file = e.dataTransfer?.files?.[0]; if (!file) return; await uploadAndSendFile(file); });

  // Emoji picker principal
  setupEmojiPicker();

  // Profile cabinet modal
  $('#profile').addEventListener('click', () => openProfileModal());

  // Avatar input în modal profil
  $('#profile-avatar-input')?.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    // Preview local al imaginii
    const reader = new FileReader();
    reader.onload = (evt) => {
      $('#profile-avatar-preview').src = evt.target.result;
      $('#profile-avatar-preview').dataset.pendingFile = 'true';
    };
    reader.readAsDataURL(file);
  });

  // Salvare profil
  $('#btn-save-profile')?.addEventListener('click', saveProfile);

  // Close modal buttons generice (reset anunț la închidere)
  $$('[data-close]').forEach(btn => btn.addEventListener('click', () => {
    const target = btn.getAttribute('data-close');
    if (target === '#modal-announcement') resetAnnouncementForm();
    closeModal(target);
  }));
  $('#modal-backdrop').addEventListener('click', () => {
    ['#modal-users', '#modal-group', '#modal-announcement', '#modal-files', '#modal-settings', '#modal-profile'].forEach(id => {
      if (id === '#modal-announcement') resetAnnouncementForm();
      closeModal(id);
    });
  });

  // Scurtături
  window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); $('#search-conv').focus(); }
    if (e.key === 'Escape') {
      if (state.ui.msgSearchOpen) toggleChatSearch(false);
      closeMsgMenu();
      document.getElementById('emoji-panel')?.classList.add('hidden');
      document.getElementById('emoji-btn')?.setAttribute('aria-expanded', 'false');
    }
  });

  // Reactualizează timpii când se schimbă TZ
  document.addEventListener('tz:changed', () => {
    renderConversations();
    if (state.currentConvId) renderMessagesList(state.messages.get(state.currentConvId) || []);
  });

  injectAIButton();
}

// Închide panoul de informații din butonul X
$('#info-close')?.addEventListener('click', () => toggleInfoPanel(false));

/* ========= Files modal (PREFETCH ALL with limited concurrency) ========= */
async function openFilesModal() {
  ensureFilesModal();
  openModal('#modal-files');
  const container = $('#files-list');
  container.innerHTML = '<div class="subtitle">Se încarcă...</div>';

  async function prefetchAll(limit = 5) {
    const convs = state.conversations.slice();
    let idx = 0;
    async function worker() {
      while (idx < convs.length) {
        const i = idx++;
        const conv = convs[i];
        if (!state.messages.get(conv.id)) {
          try {
            const data = await api(`/api/conversations/${conv.id}/messages?limit=100`);
            state.messages.set(conv.id, data.messages || []);
          } catch (e) {
            console.warn('Nu pot încărca mesaje pentru conv', conv.id, e);
          }
        }
      }
    }
    const workers = Array.from({ length: Math.min(limit, convs.length || 1) }, () => worker());
    await Promise.all(workers);
  }

  // Încarcă toate conversațiile cu concurență limitată
  await prefetchAll(5);

  // Colectează fișierele
  const files = [];
  for (const conv of state.conversations) {
    const title = formatConvTitle(conv);
    const msgs = state.messages.get(conv.id) || [];
    for (const m of msgs) {
      if (m.type === 'file' && m.fileUrl) {
        files.push({
          convId: conv.id, title,
          id: m.id, name: m.content || 'fișier', url: m.fileUrl, at: m.createdAt
        });
      }
    }
  }
  files.sort((a, b) => new Date(parseDateUTC(b.at)) - new Date(parseDateUTC(a.at)));

  if (!files.length) {
    container.innerHTML = '<div class="subtitle">Nu există fișiere recente.</div>';
    return;
  }

  // Afișare listă
  container.innerHTML = '';
  files.slice(0, 200).forEach(f => {
    const row = document.createElement('div');
    row.className = 'row';
    const isImg = isImageUrl(f.url);
    row.innerHTML = `
      <div style="display:flex;align-items:center;gap:10px;min-width:0;">
        ${isImg ? `<img src="${f.url}" alt="" style="width:36px;height:36px;object-fit:cover;border-radius:8px;border:1px solid var(--glass-stroke);" />`
        : `<div style="width:36px;height:36px;border-radius:8px;border:1px solid var(--glass-stroke);display:flex;align-items:center;justify-content:center;background:rgba(255,255,255,.06)">📎</div>`}
        <div style="min-width:0;">
          <div style="font-weight:700;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:52vw;">${escapeHtml(f.name)}</div>
          <div class="subtitle" style="font-size:12px;">${escapeHtml(f.title)} • ${fmtTime(f.at)}</div>
        </div>
      </div>
      <div style="display:flex;gap:8px;">
        <a href="${f.url}" target="_blank" rel="noopener" class="btn" style="background:var(--primary-600); color:white; padding:6px 16px; border-radius:99px; font-size:13px; text-decoration:none; border:none;">Deschide</a>
      </div>
    `;
    container.appendChild(row);
  });
}

/* ========= File handlers ========= */
async function handleFileInput(e) {
  const file = e.target.files[0];
  if (!file || !state.currentConvId) return;
  await uploadAndSendFile(file);
  e.target.value = '';
}
async function uploadAndSendFile(file) {
  try {
    const up = await fileUpload('/api/upload', file);
    // Caption din input, dacă există; altfel numele fișierului
    const caption = ($('#msg-input')?.value || '').trim();
    const isImage = file.type.startsWith('image/');
    // Dacă e imagine și nu avem caption, trimitem content gol (să nu apară numele fișierului sub poză)
    state.socket.emit('message:send', {
      conversationId: state.currentConvId,
      type: 'file',
      fileUrl: up.fileUrl,
      content: caption ? caption : (isImage ? '' : file.name)
    });
    if (caption) { $('#msg-input').value = ''; }
    scrollMessagesToEnd(true);
  } catch (err) {
    alert('Eroare upload: ' + err.message);
  }
}
/* ========= Sidebar conversații (secțiuni) ========= */
function renderConversations() {
  const q = ($('#search-conv')?.value || '').trim().toLowerCase();
  const filter = state.ui.filter;
  const container = $('#conversations');
  container.innerHTML = '';

  // 1) Filtrare
  const base = state.conversations.filter(c => {
    if (filter === 'group' && c.type !== 'group') return false;
    const title = formatConvTitle(c).toLowerCase();
    return title.includes(q);
  });

  // 2) Secțiuni
  const announcements = base.filter(c => !!c.isAnnouncement);
  const groups = base.filter(c => c.type === 'group' && !c.isAnnouncement);
  const directs = base.filter(c => c.type === 'direct');

  // 3) Item conversație
  function createConvItem(conv) {
    const div = document.createElement('div');

    const unreadCount = getUnread(conv.id);
    const isUnread = unreadCount > 0;
    div.className = 'item' + (conv.id === state.currentConvId ? ' active' : '') + (isUnread ? ' unread' : '');

    const title = formatConvTitle(conv);
    const subtitle = conv.lastMessage
      ? (conv.lastMessage.isDeleted ? 'Mesaj șters'
        : (conv.lastMessage.type === 'file' ? 'Fișier' : conv.lastMessage.content || ''))
      : '';

    const avatarSrc = getConversationAvatar(conv);
    const rightHtml = isUnread
      ? `<span class="unread-badge">${unreadCount}</span>`
      : (conv.lastMessage ? `<span class="subtitle">${fmtTime(conv.lastMessage.createdAt)}</span>` : '');

    const badges = [
      conv.type === 'group' ? 'grup' : null,
      isAIConv(conv) ? 'AI' : null,
      conv.isAnnouncement ? 'anunț' : null
    ].filter(Boolean).map(b => `<span class="badge">${b}</span>`).join(' ');

    // 🟢 Online indicator pentru conversații directe
    let onlineIndicator = '';
    if (conv.type === 'direct' && !isAIConv(conv)) {
      const other = conv.members?.find(m => m.id !== state.user.id);
      if (other && state.onlineUsers.has(other.id)) {
        onlineIndicator = '<span class="online-indicator"></span>';
      }
    }

    div.innerHTML = `
      <div class="avatar-wrap">
        <img class="avatar" src="${avatarSrc}" alt="" />
        ${onlineIndicator}
      </div>
      <div class="conv-middle">
        <div class="title">${escapeHtml(title)} ${badges}</div>
        <div class="subtitle">${escapeHtml(subtitle || '')}</div>
      </div>
      <div class="right">${rightHtml}</div>
    `;
    div.addEventListener('click', () => openConversation(conv.id));
    return div;
  }

  // 4) Secțiune (pliabilă)
  function addSection(key, icon, label, items) {
    if (!items.length) return;
    const sec = document.createElement('div');
    sec.className = 'conv-section';
    if (state.ui.sections?.[key]) sec.dataset.collapsed = '1';

    const head = document.createElement('button');
    head.className = 'section-head';
    head.setAttribute('aria-expanded', state.ui.sections?.[key] ? 'false' : 'true');
    head.innerHTML = `
      <span class="caret">▾</span>
      <span class="icon">${icon}</span>
      <span class="label">${label}</span>
      <span class="count">${items.length}</span>
    `;
    head.addEventListener('click', () => {
      const v = !state.ui.sections[key];
      state.ui.sections[key] = v;
      if (v) sec.dataset.collapsed = '1'; else sec.removeAttribute('data-collapsed');
      head.setAttribute('aria-expanded', v ? 'false' : 'true');
      saveSectionsUI();
    });

    const body = document.createElement('div');
    body.className = 'section-body';
    items.forEach(c => body.appendChild(createConvItem(c)));

    sec.appendChild(head);
    sec.appendChild(body);
    container.appendChild(sec);
  }

  // 5) Afișare
  if (filter === 'group') {
    addSection('announcements', '📣', 'Anunțuri', announcements);
    addSection('groups', '👥', 'Grupuri & canale', groups);
  } else {
    addSection('announcements', '📣', 'Anunțuri', announcements);
    addSection('groups', '👥', 'Grupuri & canale', groups);
    addSection('direct', '💬', 'Mesaje directe', directs);
  }

  // 6) Empty
  if (!container.children.length) {
    const empty = document.createElement('div');
    empty.className = 'subtitle';
    empty.style.padding = '8px 12px';
    empty.textContent = 'Nu există conversații';
    container.appendChild(empty);
  }
}

/* ========= Deschidere conversație ========= */
async function openConversation(convId) {
  state.currentConvId = convId;
  state.socket.emit('join', { conversationId: convId });

  const conv = state.conversations.find(c => c.id === convId);
  if (!conv) return;

  document.dispatchEvent(new CustomEvent('conversation:changed', { detail: { convId } }));

  clearUnread(convId);
  renderConversations();

  renderChatHeader(conv);

  const isLocked = conv.isAnnouncement && state.user.id !== (conv.createdBy || -1);
  const composer = $('#composer');
  const input = $('#msg-input');

  if (isLocked) {
    composer.style.display = 'none';
    const note = document.createElement('span');
    note.className = 'badge ann-note';
    note.textContent = 'Anunț — doar autorul poate posta';
    $('#chat-header').appendChild(note);
  } else {
    composer.style.display = 'flex';
    $('#chat-header').querySelector('.ann-note')?.remove();
    ensureComposerBindings();
    input.placeholder = isAIConv(conv) ? 'Întreabă AI…' : 'Scrie un mesaj...';
  }

  // 1) Randare imediată din cache
  const cached = state.messages.get(convId) || [];
  renderMessagesList(cached);
  normalizeSeparators();
  if (state.ui.rightOpen) renderInfoPanel(convId, /*force=*/true);

  // 2) Fetch fresh și re-render dacă s-a schimbat lista
  try {
    const data = await api(`/api/conversations/${convId}/messages`);
    const fresh = data.messages || [];
    const current = state.messages.get(convId) || [];
    const curLast = current[current.length - 1]?.id;
    const freshLast = fresh[fresh.length - 1]?.id;
    if (fresh.length !== current.length || curLast !== freshLast) {
      state.messages.set(convId, fresh);
      renderMessagesList(fresh);
      normalizeSeparators();
      if (state.ui.rightOpen) renderInfoPanel(state.currentConvId, /*force=*/true);
    }
  } catch (e) {
    console.warn('Nu s-au putut încărca mesajele fresh pentru conv', convId, e);
  }
}

/* ========= Header conversație ========= */
function renderChatHeader(conv) {
  const header = $('#chat-header');
  header.innerHTML = '';

  const avatar = document.createElement('img');
  avatar.className = 'avatar';
  avatar.style.width = '32px';
  avatar.style.height = '32px';
  avatar.src = getConversationAvatar(conv);
  avatar.alt = '';

  const titleWrap = document.createElement('div');
  const title = document.createElement('strong');
  title.textContent = formatConvTitle(conv);
  const sub = document.createElement('div');
  sub.className = 'subtitle';

  // 🟢 Status online/offline pentru DMs
  if (conv.type === 'direct' && !isAIConv(conv)) {
    const other = conv.members?.find(m => m.id !== state.user.id);
    if (other) {
      if (state.onlineUsers.has(other.id)) {
        sub.innerHTML = '<span class="status-text online">● Online</span>';
      } else {
        // Caută lastSeen în datele userului
        const cachedUser = state.users.find(u => u.id === other.id);
        const lastSeenText = formatLastSeen(cachedUser?.lastSeen || other.lastSeen);
        sub.innerHTML = `<span class="status-text offline">${lastSeenText}</span>`;
      }
    } else {
      sub.textContent = 'Direct chat';
    }
  } else {
    const membersCount = conv.members?.length || 0;
    sub.textContent = conv.type === 'group' ? `${membersCount} membri` : (isAIConv(conv) ? 'AI chat' : 'Direct chat');
  }

  titleWrap.appendChild(title);
  titleWrap.appendChild(sub);
  header.appendChild(avatar);
  header.appendChild(titleWrap);

  const tools = document.createElement('div');
  tools.className = 'hdr-actions';
  tools.innerHTML = `
    <button class="btn" id="btn-chat-search" title="Caută în chat">🔎</button>
    <button class="btn" id="btn-info" title="Detalii chat">ℹ️</button>
    <button class="btn danger" id="btn-del-me" title="Șterge chat (eu)">Șterge (eu)</button>
    <button class="btn danger" id="btn-del-all" title="Șterge chat (toți)">Șterge (toți)</button>
  `;
  header.appendChild(tools);

  $('#btn-del-me').addEventListener('click', async () => {
    openConfirmModal('Ștergi conversația din istorie doar pentru tine?', async () => {
      try {
        await api(`/api/conversations/${conv.id}?scope=me`, { method: 'DELETE' });
        const idx = state.conversations.findIndex(c => c.id === conv.id);
        if (idx >= 0) state.conversations.splice(idx, 1);
        state.currentConvId = null;
        renderConversations();
        header.innerHTML = '';
        $('#messages').innerHTML = '';
        toggleInfoPanel(false);
      } catch (e) { openAlertModal(e.message); }
    });
  });

  $('#btn-del-all').addEventListener('click', async () => {
    openConfirmModal('Ștergi conversația pentru toți utilizatorii?', async () => {
      try {
        await api(`/api/conversations/${conv.id}?scope=all`, { method: 'DELETE' });
        const idx = state.conversations.findIndex(c => c.id === conv.id);
        if (idx >= 0) state.conversations.splice(idx, 1);
        state.currentConvId = null;
        renderConversations();
        header.innerHTML = '';
        $('#messages').innerHTML = '';
        toggleInfoPanel(false);
      } catch (e) { openAlertModal(e.message); }
    });
  });

  $('#btn-info').addEventListener('click', () => toggleInfoPanel(!state.ui.rightOpen));
  $('#btn-chat-search').addEventListener('click', () => toggleChatSearch(!state.ui.msgSearchOpen));
}

let infoOutsideHandler = null;

/* ========= Right info panel ========= */
function toggleInfoPanel(open) {
  state.ui.rightOpen = !!open;
  const panel = $('#info-panel');
  const layout = $('#layout');
  if (!panel) return;

  const mobile = isMobile();

  // curățăm orice handler vechi de click-în-afara
  if (infoOutsideHandler) {
    document.removeEventListener('click', infoOutsideHandler, true);
    infoOutsideHandler = null;
  }

  if (state.ui.rightOpen) {
    // DESCHIDE
    panel.classList.remove('hidden'); // scoatem display:none
    panel.classList.add('open');      // animăm drawerul pe mobil

    if (!mobile) {
      // Desktop: afișăm ca și coloană în layout
      layout?.classList.add('has-right');
    }

    if (state.currentConvId) {
      renderInfoPanel(state.currentConvId, /*force=*/true);
    }

    if (mobile) {
      // pe mobil: click oriunde în afara panoului -> închide
      infoOutsideHandler = (e) => {
        const btn = $('#btn-info');
        if (!panel.contains(e.target) && !btn?.contains(e.target)) {
          toggleInfoPanel(false);
        }
      };
      // capturăm înaintea altor handlers
      setTimeout(() => document.addEventListener('click', infoOutsideHandler, true), 0);

      // Esc pe mobil (și desktop) închide dacă e deschis
      const escHandler = (ev) => {
        if (ev.key === 'Escape') {
          toggleInfoPanel(false);
          document.removeEventListener('keydown', escHandler, true);
        }
      };
      document.addEventListener('keydown', escHandler, true);
    }

  } else {
    // ÎNCHIDE
    panel.classList.remove('open');

    if (!mobile) {
      panel.classList.add('hidden');
      layout?.classList.remove('has-right');
    }
  }
}

function renderInfoPanel(convId, force = false) {
  const conv = state.conversations.find(c => c.id === convId);
  if (!conv) return;

  $('#info-summary').innerHTML = `
    <div style="display:flex;align-items:center;gap:10px;">
      <img class="avatar" style="width:40px;height:40px;" src="${getConversationAvatar(conv)}" alt="" />
      <div>
        <div style="font-weight:700">${escapeHtml(formatConvTitle(conv))}</div>
        <div class="subtitle">${conv.type === 'group' ? `${conv.members?.length || 0} membri` : (isAIConv(conv) ? 'AI chat' : 'Direct chat')}</div>
      </div>
    </div>
  `;

  const msgs = state.messages.get(convId) || [];
  const imgs = msgs.filter(m => !m.isDeleted && m.type === 'file' && m.fileUrl && isImageUrl(m.fileUrl)).slice(-12);
  const mediaDiv = $('#info-media');
  mediaDiv.innerHTML = imgs.length
    ? imgs.map(m => `<a href="${m.fileUrl}" target="_blank" title="${escapeHtml(m.content || '')}"><img src="${m.fileUrl}" alt="" style="width:64px;height:64px;object-fit:cover;border-radius:12px;border:1px solid rgba(255,255,255,.08);"/></a>`).join('')
    : '<div class="subtitle">Fără media</div>';

  const members = conv.members || [];
  const isUserAdmin = isAdmin();

  // listă membri cu buton "Scoate" pentru admin
  $('#info-members').innerHTML = members.map(u => {
    const label = u.username === AI_USERNAME ? AI_LABEL : '@' + escapeHtml(u.username);
    const canKick =
      isUserAdmin &&
      u.id !== state.user.id &&                      // nu îți vezi buton la tine
      (u.role || 'user') !== 'admin' &&              // nu scoți alți admini
      u.username !== AI_USERNAME;                    // nu scoți AI

    const kickBtn = canKick
      ? `<button class="btn danger btn-kick-member"
                 data-kick="${u.id}"
                 data-name="${escapeHtml(u.username)}"
                 style="margin-left:auto;padding:4px 8px;font-size:11px;line-height:1;">
           Scoate
         </button>`
      : '';

    return `
      <div class="info-member-row" style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
        <img class="avatar" style="width:28px;height:28px;" src="${userAvatarUrl(u.username, u.avatarUrl)}" alt="" />
        <div>${label}</div>
        ${kickBtn}
      </div>
    `;
  }).join('');

  // atașează handler-ele pentru butoanele "Scoate"
  if (isUserAdmin) {
    $$('#info-members .btn-kick-member').forEach(btn => {
      btn.addEventListener('click', async () => {
        const userId = Number(btn.getAttribute('data-kick'));
        const uname = btn.getAttribute('data-name');
        if (!userId) return;
        openConfirmModal(`Scoți utilizatorul @${uname} din acest chat?`, async () => {
          try {
            await api(`/api/conversations/${convId}/members/${userId}`, { method: 'DELETE' });
            // actualizarea UI vine prin socket 'conversation:updated' / 'conversation:deleted'
          } catch (e) {
            alert(e.message || 'Eroare la scoaterea utilizatorului din chat');
          }
        });
      });
    });
  }

  $('#info-leave').onclick = () => $('#btn-del-me')?.click();
}

/* ========= Mesaje: listă + separatoare ========= */
function renderMessagesList(messages) {
  const m = $('#messages'); m.innerHTML = '';
  let lastDate = null;
  (messages || []).forEach(msg => {
    if (msg.isDeleted) return; // NU randăm mesajele șterse (toți)
    if (!lastDate || !isSameDay(lastDate, msg.createdAt)) {
      insertDaySeparator(dayLabel(msg.createdAt));
      lastDate = msg.createdAt;
    }
    renderMessage(msg);
  });
  normalizeSeparators();

  scrollMessagesToEnd(true);
  if (state.ui.msgSearchOpen && state.ui.msgSearchQuery) applyMessageSearchHighlight(state.ui.msgSearchQuery);
}
function insertDaySeparator(label) {
  const sep = document.createElement('div');
  sep.className = 'msg-separator';
  sep.textContent = label;
  $('#messages').appendChild(sep);
}
function appendMessageWithSeparator(msg) {
  const m = $('#messages');
  let needSep = true;
  for (let i = m.children.length - 1; i >= 0; i--) {
    const el = m.children[i];
    if (el && el.classList.contains('msg-row')) {
      const lastBubble = el.querySelector('.message');
      if (lastBubble && lastBubble.dataset.createdAt) needSep = !isSameDay(lastBubble.dataset.createdAt, msg.createdAt);
      break;
    }
  }
  if (needSep) insertDaySeparator(dayLabel(msg.createdAt));
  renderMessage(msg);
}

/* ========= Randare mesaj ========= */
function buildMessageContentHTML(msg) {
  if (msg.isDeleted) return '';

  if (msg.type === 'file') {
    const caption = (msg.content || '').trim();
    const isImg = msg.fileUrl && isImageUrl(msg.fileUrl);

    if (isImg) {
      const imgHtml = `<div class="msg-image"><img src="${msg.fileUrl}" alt="image" loading="lazy" /></div>`;
      // Dacă textul e chiar numele fișierului (găsit în URL) sau un UUID lung, nu îl afișăm
      const isFilename = (caption && msg.fileUrl.includes(caption));

      const capHtml = (caption && !isFilename) ? `<div class="msg-caption" data-raw="${escapeHtml(caption)}">${escapeHtml(caption)}</div>` : '';
      return imgHtml + capHtml;
    } else {
      const name = escapeHtml(caption || (msg.fileUrl.split('/').pop() || 'fișier'));
      return `
        <div class="file-attachment">
          <div class="file-icon">📎</div>
          <div class="file-name">${name}</div>
          <a href="${msg.fileUrl}" target="_blank" rel="noopener">Descarcă fișier</a>
        </div>
      `;
    }
  }

  // text
  return escapeHtml(msg.content || '');
}

function renderMessage(msg) {
  const isMine = msg.senderId === state.user.id;
  const sender = resolveSender(msg);
  const displayName = isMine ? 'Eu' : (sender?.username === AI_USERNAME ? AI_LABEL : (sender?.username || `user-${msg.senderId}`));
  const avatarSrc = userAvatarUrl(sender?.username, sender?.avatarUrl);

  const row = document.createElement('div');
  row.className = 'msg-row ' + (isMine ? 'mine' : 'their');

  const avatar = document.createElement('img');
  avatar.className = 'msg-avatar';
  avatar.src = avatarSrc;
  avatar.alt = displayName;

  const bubble = document.createElement('div');
  bubble.className = 'message ' + (isMine ? 'mine' : 'their');
  bubble.dataset.msg = msg.id;
  bubble.dataset.createdAt = msg.createdAt;

  const contentHtml = buildMessageContentHTML(msg);

  bubble.innerHTML = `
    <span class="msg-actions" title="Acțiuni">⋯</span>
    <div class="author">${escapeHtml(displayName)}</div>
    <div class="content">${contentHtml}</div>
    <div class="reactions">${(msg.reactions || []).map(r =>
    `<span class="reaction ${r.reactedByMe ? 'me' : ''}" data-emoji="${r.emoji}">${r.emoji} ${r.count}</span>`
  ).join('')
    }</div>
    <div class="meta">${fmtTime(msg.createdAt)}${msg.editedAt ? ' • editat' : ''}</div>
  `;

  const contentEl = bubble.querySelector('.content');
  // dataset.raw pentru text și pentru file (caption fiind tratat separat)
  contentEl.dataset.raw = (!msg.isDeleted && (msg.type === 'text' || msg.type === 'file')) ? (msg.content || '') : '';

  bubble.querySelector('.msg-actions').addEventListener('click', (e) => { e.stopPropagation(); openMessageMenu(msg); });
  bubble.querySelectorAll('.reaction').forEach(btn => { btn.addEventListener('click', () => toggleReaction(msg.id, btn.getAttribute('data-emoji'))); });

  row.appendChild(avatar); row.appendChild(bubble);
  $('#messages').appendChild(row);

  // fallback pentru imagini care nu se pot încărca
  attachImageFallback(bubble, msg);
}

/* Fallback automat: dacă <img> nu se încarcă -> transformă în chip "file-attachment" */
function attachImageFallback(bubbleEl, msg) {
  try {
    const img = bubbleEl.querySelector('.msg-image img');
    if (!img) return;
    const handler = () => {
      const content = bubbleEl.querySelector('.content');
      const caption = (msg.content || '').trim();
      const name = escapeHtml(caption || (msg.fileUrl?.split('/').pop() || 'fișier'));
      content.innerHTML = `
        <div class="file-attachment">
          <div class="file-icon">📎</div>
          <div class="file-name">${name}</div>
          <a href="${msg.fileUrl}" target="_blank" rel="noopener">Descarcă fișier</a>
        </div>
      `;
      // păstrăm textul pentru căutare
      content.dataset.raw = msg.content || '';
      img.onerror = null;
    };
    img.onerror = handler;
  } catch { }
}

/* ========= Normalizare separatoare de zi ========= */
function normalizeSeparators() {
  const container = $('#messages');
  if (!container) return;

  // 1) elimină separatorii existenți
  container.querySelectorAll('.msg-separator').forEach(el => el.remove());

  // 2) reconstruiește în funcție de mesajele rămase
  let lastDay = null;
  const rows = Array.from(container.querySelectorAll('.msg-row'));
  rows.forEach(row => {
    const msgEl = row.querySelector('.message');
    const at = msgEl?.dataset?.createdAt;
    if (!at) return;
    const dk = tzDayKey(at);
    if (dk !== lastDay) {
      const sep = document.createElement('div');
      sep.className = 'msg-separator';
      sep.textContent = dayLabel(at);
      container.insertBefore(sep, row);
      lastDay = dk;
    }
  });
}

/* ========= Cache mesaje ========= */
function cacheMessage(convId, msg) {
  const arr = state.messages.get(convId) || [];
  if (!arr.some(m => m.id === msg.id)) arr.push(msg);
  state.messages.set(convId, arr);
}
function updateCachedMessage(msg) {
  const arr = state.messages.get(msg.conversationId);
  if (!arr) return;
  const i = arr.findIndex(m => m.id === msg.id);
  if (i >= 0) arr[i] = { ...arr[i], ...msg };
}
function removeFromCache(msgId, convId) {
  const arr = state.messages.get(convId);
  if (!arr) return;
  const i = arr.findIndex(m => m.id === msgId);
  if (i >= 0) arr.splice(i, 1);
}
function markDeletedInCache(msgId, convId) {
  const arr = state.messages.get(convId);
  if (!arr) return;
  const i = arr.findIndex(m => m.id === msgId);
  if (i >= 0) arr[i].isDeleted = true;
}

/* ========= Reacții ========= */
async function toggleReaction(messageId, emoji) {
  try { await api(`/api/messages/${messageId}/reactions`, { method: 'POST', body: JSON.stringify({ emoji }) }); }
  catch (e) { openAlertModal(e.message); }
}

/* ========= Meniu ⋯ pe mesaj ========= */
let currentEditMsgId = null;
function openEditModal(msgId, currentText) {
  currentEditMsgId = msgId;
  const txt = document.getElementById('edit-msg-text');
  if (txt) txt.value = currentText;
  openModal('#modal-edit-msg');
  setTimeout(() => txt?.focus(), 50);
}

document.getElementById('btn-save-edit')?.addEventListener('click', async () => {
  const text = document.getElementById('edit-msg-text').value.trim();
  if (!currentEditMsgId) return;
  try {
    await api(`/api/messages/${currentEditMsgId}`, { method: 'PUT', body: JSON.stringify({ content: text }) });
    closeModal('#modal-edit-msg');
  } catch (e) { openAlertModal(e.message); }
});

/* ========= Modal Confirmare Generic ========= */
let onConfirmCallback = null;
function openConfirmModal(text, onConfirm) {
  const modal = document.getElementById('modal-confirm');
  const txt = document.getElementById('modal-confirm-text');
  if (modal && txt) {
    txt.textContent = text;
    onConfirmCallback = onConfirm;
    openModal('#modal-confirm');
  }
}

document.getElementById('btn-confirm-yes')?.addEventListener('click', () => {
  if (onConfirmCallback) onConfirmCallback();
  closeModal('#modal-confirm');
  onConfirmCallback = null;
});
document.getElementById('btn-confirm-no')?.addEventListener('click', () => {
  closeModal('#modal-confirm');
  onConfirmCallback = null;
});

/* ========= Modal Alert Generic ========= */
function openAlertModal(text) {
  const modal = document.getElementById('modal-alert');
  const txt = document.getElementById('modal-alert-text');
  if (modal && txt) {
    // Traducere mesaje de eroare tehnice în română
    let displayText = text;
    if (text === 'Failed to fetch' || text === 'NetworkError when attempting to fetch resource.') {
      displayText = 'Conexiunea la server nu este activă.';
    }
    txt.textContent = displayText;
    openModal('#modal-alert');
  }
}
document.getElementById('btn-alert-ok')?.addEventListener('click', () => {
  closeModal('#modal-alert');
});

let currentMsgMenu = null;
let currentMsgMenuAnchor = null;

function closeMsgMenu() { currentMsgMenu?.remove(); currentMsgMenu = null; currentMsgMenuAnchor = null; }

function openMessageMenu(msg) {
  const anchorBtn = document.querySelector(`[data-msg="${msg.id}"] .msg-actions`);
  const bubble = document.querySelector(`[data-msg="${msg.id}"]`);
  if (!anchorBtn || !bubble) return;

  if (currentMsgMenu && currentMsgMenuAnchor === anchorBtn) { closeMsgMenu(); return; }
  closeMsgMenu();

  const canEdit = (msg.senderId === state.user.id) && !msg.isDeleted && (msg.type === 'text');
  const hasCopyable =
    !msg.isDeleted && (
      (msg.type === 'text' && (msg.content || '').trim().length > 0) ||
      (msg.type === 'file' && (msg.content || '').trim().length > 0)
    );

  // Nou: și adminul poate șterge (toți) orice mesaj
  const canDeleteAll = (msg.senderId === state.user.id) || isAdmin();

  const menu = document.createElement('div');
  menu.className = 'msg-menu';
  const quick = ['❤️', '😂', '👍', '👎', '🔥', '😡'];
  const more = ['😀', '😁', '🤣', '😊', '😍', '😎', '🤔', '😅', '🙌', '👏', '🙏', '💯', '💡', '🤝', '🫶', '✨', '😢', '😮', '😴', '🎉'];

  menu.innerHTML = `
    <div class="emoji-row">
      ${quick.map(e => `<button class="emoji-btn" data-emoji="${e}">${e}</button>`).join('')}
      <button class="more-btn" title="Mai multe">•••</button>
    </div>
    <div class="emoji-more">
      ${more.map(e => `<button class="emoji-btn" data-emoji="${e}">${e}</button>`).join('')}
    </div>
    ${canEdit ? `<button class="action" data-act="edit">Editează</button>` : ''}
    ${hasCopyable ? `<button class="action" data-act="copy">Copiază text</button>` : ''}
    <div class="split"></div>
    <button class="action" data-act="del-me">Șterge (eu)</button>
    ${canDeleteAll ? `<button class="action" data-act="del-all">Șterge (toți)</button>` : ''}
  `;

  menu.addEventListener('click', (e) => e.stopPropagation());
  menu.style.visibility = 'hidden';
  document.body.appendChild(menu);

  const anchorRect = anchorBtn.getBoundingClientRect();
  const container = document.getElementById('messages');
  const cRect = container.getBoundingClientRect();
  const mW = menu.offsetWidth;
  const mH = menu.offsetHeight;
  const margin = 8;

  const isMine = bubble.classList.contains('mine');
  let leftWanted = isMine ? (anchorRect.right - mW) : anchorRect.left;
  const minLeft = cRect.left + margin;
  const maxLeft = cRect.right - mW - margin;
  leftWanted = Math.min(Math.max(leftWanted, minLeft), Math.max(minLeft, maxLeft));

  let topWanted = anchorRect.bottom + margin;
  const maxBottom = cRect.bottom - margin;
  if (topWanted + mH > maxBottom) {
    topWanted = anchorRect.top - mH - margin;
    const minTop = cRect.top + margin;
    if (topWanted < minTop) topWanted = minTop;
  }

  menu.style.left = `${leftWanted}px`;
  menu.style.top = `${topWanted}px`;
  menu.style.visibility = 'visible';

  const onDocClick = (e) => {
    if (menu.contains(e.target) || anchorBtn.contains(e.target)) return;
    document.removeEventListener('click', onDocClick, false);
    closeMsgMenu();
  };
  setTimeout(() => document.addEventListener('click', onDocClick, false), 0);

  currentMsgMenu = menu;
  currentMsgMenuAnchor = anchorBtn;

  menu.querySelectorAll('.emoji-btn').forEach(b => {
    b.addEventListener('click', async (e) => { e.preventDefault(); e.stopPropagation(); await toggleReaction(msg.id, b.dataset.emoji); closeMsgMenu(); });
  });

  const moreBtn = menu.querySelector('.more-btn');
  const moreBox = menu.querySelector('.emoji-more');
  moreBtn.addEventListener('click', (e) => { e.preventDefault(); e.stopPropagation(); moreBox.classList.toggle('open'); });

  menu.querySelectorAll('.action').forEach(b => {
    b.addEventListener('click', async (e) => {
      e.preventDefault(); e.stopPropagation();
      const act = b.dataset.act;
      try {
        if (act === 'edit') {
          const cur = document.querySelector(`[data-msg="${msg.id}"] .content`)?.textContent || msg.content || '';
          openEditModal(msg.id, cur);
        } else if (act === 'copy') {
          const cap = document.querySelector(`[data-msg="${msg.id}"] .msg-caption`)?.dataset.raw || '';
          const base = document.querySelector(`[data-msg="${msg.id}"] .content`)?.dataset.raw || '';
          const text = msg.type === 'file' ? (cap || base || msg.content || '') : (base || msg.content || '');
          if (!text) return;
          window.focus(); // Fix Electron clipboard
          await navigator.clipboard.writeText(text);
        } else if (act === 'del-me') {
          await api(`/api/messages/${msg.id}?scope=me`, { method: 'DELETE' });
          document.querySelector(`[data-msg="${msg.id}"]`)?.closest('.msg-row')?.remove();
          removeFromCache(msg.id, msg.conversationId);
          normalizeSeparators();
        } else if (act === 'del-all') {
          openConfirmModal('Ștergi acest mesaj pentru toți?', async () => {
            try {
              await api(`/api/messages/${msg.id}?scope=all`, { method: 'DELETE' });
            } catch (err) { openAlertModal(err.message); }
          });
        }
      } catch (err) {
        openAlertModal(err.message || 'Eroare');
      } finally {
        closeMsgMenu();
      }
    });
  });
}

/* ========= Utils scroll ========= */
function isAtBottom() {
  const m = $('#messages'); if (!m) return true;
  return (m.scrollHeight - m.scrollTop - m.clientHeight) < 4;
}
function scrollMessagesToEnd(force = false) {
  const m = $('#messages'); if (!m) return;
  const nearBottom = (m.scrollHeight - m.scrollTop - m.clientHeight) < 80;
  if (force || nearBottom) m.scrollTop = m.scrollHeight;
}

/* ========= Typing & send ========= */
function sendTyping(isTyping) {
  if (!state.currentConvId || !state.socket) return;
  state.socket.emit('typing', { conversationId: state.currentConvId, isTyping });
}
async function sendMessage() {
  const input = $('#msg-input');
  const text = (input?.value || '').trim();
  if (!state.currentConvId || !text) return;
  if (!state.socket || !state.socket.connected) { openAlertModal('Conexiunea la server nu este activă.'); return; }
  state.socket.emit('message:send', { conversationId: state.currentConvId, content: text, type: 'text' });
  input.value = '';
}

/* ========= Fail-safe composer ========= */
function ensureComposerBindings() {
  const sendBtn = $('#send-btn');
  const input = $('#msg-input');
  if (!sendBtn || !input) return;
  sendBtn.onclick = async () => { await sendMessage(); scrollMessagesToEnd(true); };
  input.onkeydown = async (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); await sendMessage(); scrollMessagesToEnd(true); } else { sendTyping(true); } };
  input.onblur = () => sendTyping(false);
}

/* ========= Users modal (include acțiuni admin) ========= */
function renderUsersList() {
  const container = $('#users-list'); if (!container) return;
  container.innerHTML = '';
  state.users.forEach(u => {
    const allowDelete = isAdmin()
      && u.username !== AI_USERNAME
      && (u.role || 'user') !== 'admin'
      && u.id !== state.user.id;

    const delBtn = allowDelete
      ? `<button class="danger" data-del="${u.id}">Șterge</button>`
      : '';

    const addBtn = state.currentConvId ? `<button data-add="${u.id}">Adaugă în chat</button>` : '';

    const row = document.createElement('div');
    row.className = 'row';
    row.innerHTML = `<div>@${escapeHtml(u.username)}</div>
      <div style="display:flex; gap:6px; flex-wrap:wrap;">
        <button data-dm="${u.id}">Mesaj direct</button>
        ${addBtn}
        ${delBtn}
      </div>`;

    row.querySelector(`[data-dm="${u.id}"]`).addEventListener('click', async () => {
      try {
        const data = await api('/api/direct/' + u.id, { method: 'POST' });
        upsertConversationToTop(data.conversation);
        renderConversations();
        closeModal('#modal-users');
        openConversation(data.conversation.id);
      } catch (e) { openAlertModal(e.message); }
    });

    if (state.currentConvId) {
      row.querySelector(`[data-add="${u.id}"]`)?.addEventListener('click', async () => {
        try {
          await api(`/api/conversations/${state.currentConvId}/members`, { method: 'POST', body: JSON.stringify({ userIds: [u.id] }) });
          openAlertModal(`@${u.username} a fost adăugat în grup`);
        } catch (e) { openAlertModal(e.message || 'Eroare'); }
      });
    }

    if (allowDelete) {
      row.querySelector(`[data-del="${u.id}"]`)?.addEventListener('click', async () => {
        openConfirmModal(`Ștergi utilizatorul @${u.username}? Această acțiune este permanentă.`, async () => {
          try {
            await api(`/api/admin/users/${u.id}`, { method: 'DELETE' });
            await fetchUsers();
            await fetchConversations();
            openAlertModal(`Utilizatorul @${u.username} a fost șters.`);
          } catch (err) {
            openAlertModal(err?.message || 'Eroare la ștergerea utilizatorului');
          }
        });
      });
    }

    container.appendChild(row);
  });
}
function renderGroupUsersList() {
  const container = $('#group-users'); if (!container) return;
  container.innerHTML = '';
  state.users.forEach(u => {
    const row = document.createElement('div');
    row.className = 'row';
    // Folosim DIV în loc de LABEL și gestionăm click-ul manual pe rând
    row.innerHTML = `
      <div style="display:flex; align-items:center; gap:8px; pointer-events:none; width:100%;">
        <input type="checkbox" value="${u.id}" style="pointer-events:auto;" />
        <span>@${escapeHtml(u.username)}</span>
      </div>`;

    // Handler explicit pentru click pe rând (extinde aria de selecție)
    row.addEventListener('click', (e) => {
      if (e.target.tagName === 'INPUT') return;
      const chk = row.querySelector('input');
      if (chk) chk.checked = !chk.checked;
    });

    container.appendChild(row);
  });
}
function renderAnnounceUsersList() {
  const container = $('#ann-users'); if (!container) return;
  container.innerHTML = '';
  state.users.forEach(u => {
    const row = document.createElement('div');
    row.className = 'row';
    // Folosim DIV în loc de LABEL pentru consistență și evitarea problemelor de dublu-click
    row.innerHTML = `
      <div style="display:flex; align-items:center; gap:8px; pointer-events:none; width:100%;">
        <input type="checkbox" value="${u.id}" style="pointer-events:auto;" />
        <span>@${escapeHtml(u.username)}</span>
      </div>`;

    row.addEventListener('click', (e) => {
      if (e.target.tagName === 'INPUT') return;
      const chk = row.querySelector('input');
      if (chk) chk.checked = !chk.checked;
    });

    container.appendChild(row);
  });
}

/* ========= Chat search (highlight) ========= */
function toggleChatSearch(open) {
  state.ui.msgSearchOpen = !!open;
  const header = $('#chat-header');
  header.querySelector('#chat-search-wrap')?.remove();

  if (!state.ui.msgSearchOpen) {
    state.ui.msgSearchQuery = '';
    applyMessageSearchHighlight('');
    return;
  }
  const wrap = document.createElement('div');
  wrap.id = 'chat-search-wrap';
  wrap.style.marginLeft = '12px';
  const width = isMobile() ? '120px' : '220px';
  wrap.innerHTML = `<input id="chat-search" placeholder="Caută în mesaje..." style="padding:6px 10px;border-radius:12px;border:1px solid var(--border);background:var(--bg-card);color:var(--text);width:${width};" />`;
  header.insertBefore(wrap, header.querySelector('.hdr-actions'));

  const input = $('#chat-search');
  input.focus();
  input.addEventListener('input', () => {
    state.ui.msgSearchQuery = input.value.trim();
    applyMessageSearchHighlight(state.ui.msgSearchQuery);
  });
}
function applyMessageSearchHighlight(q) {
  const query = (q || '').trim();
  // 1) Mesaje text (folosesc .content[data-raw])
  $$('#messages .message .content').forEach(el => {
    const hasCaption = !!el.querySelector('.msg-caption'); // e mesaj cu imagine
    if (hasCaption) return; // pt imagini, highlight doar pe caption (mai jos)
    const raw = el.dataset.raw || '';
    if (!raw) return;
    if (!query) { el.innerHTML = escapeHtml(raw); return; }
    const re = new RegExp('(' + escapeRegExp(query) + ')', 'gi');
    const safe = escapeHtml(raw);
    el.innerHTML = safe.replace(re, '<mark>$1</mark>');
  });
  // 2) Captions sub imagini (.msg-caption[data-raw])
  $$('#messages .message .msg-caption').forEach(cap => {
    const raw = cap.dataset.raw || '';
    if (!raw) { cap.textContent = ''; return; }
    if (!query) { cap.innerHTML = escapeHtml(raw); return; }
    const re = new RegExp('(' + escapeRegExp(query) + ')', 'gi');
    const safe = escapeHtml(raw);
    cap.innerHTML = safe.replace(re, '<mark>$1</mark>');
  });
}

/* ========= Profile Cabinet Modal ========= */
function openProfileModal() {
  const preview = $('#profile-avatar-preview');
  const usernameInput = $('#profile-username');
  const errorDiv = $('#profile-error');

  // Populează cu datele curente
  if (preview) {
    preview.src = userAvatarUrl(state.user.username, state.user.avatarUrl);
    delete preview.dataset.pendingFile;
  }
  if (usernameInput) {
    usernameInput.value = state.user.username || '';
  }
  if (errorDiv) {
    errorDiv.textContent = '';
  }

  // Resetează input-ul de fișier
  const fileInput = $('#profile-avatar-input');
  if (fileInput) fileInput.value = '';

  openModal('#modal-profile');
}

async function saveProfile() {
  const usernameInput = $('#profile-username');
  const preview = $('#profile-avatar-preview');
  const fileInput = $('#profile-avatar-input');
  const errorDiv = $('#profile-error');

  const newUsername = usernameInput?.value.trim();
  if (!newUsername) {
    if (errorDiv) errorDiv.textContent = 'Numele de utilizator nu poate fi gol.';
    return;
  }

  try {
    const updates = {};

    // Verifică dacă username-ul s-a schimbat
    if (newUsername !== state.user.username) {
      updates.username = newUsername;
    }

    // Verifică dacă există un fișier nou de încărcat
    const file = fileInput?.files?.[0];
    if (file) {
      const up = await fileUpload('/api/upload', file);
      updates.avatarUrl = up.fileUrl;
    }

    // Fă update doar dacă există modificări
    if (Object.keys(updates).length > 0) {
      const result = await api('/api/me', {
        method: 'PUT',
        body: JSON.stringify(updates)
      });

      // Actualizează state-ul local
      if (updates.username) {
        state.user.username = updates.username;
        $('#me-name').textContent = state.user.username + (isAdmin() ? ' (admin)' : '');
      }
      if (updates.avatarUrl) {
        state.user.avatarUrl = updates.avatarUrl;
        $('#avatar').src = updates.avatarUrl;
      }
    }

    closeModal('#modal-profile');
  } catch (err) {
    if (errorDiv) {
      errorDiv.textContent = err.message || 'Eroare la salvarea profilului.';
    }
  }
}

/* ========= Modals ========= */
function openModal(target) {
  // 1. Închide orice alt modal deschis
  document.querySelectorAll('.modal:not(.hidden)').forEach(m => {
    m.classList.add('hidden');
    m.setAttribute('aria-hidden', 'true');
  });

  // 2. Închide drawer-ul (sidebar) pe mobil, dacă e deschis
  const drawer = document.getElementById('drawer');
  if (drawer && drawer.checked) drawer.checked = false;

  // 3. Afișează backdrop-ul
  const backdrop = document.getElementById('modal-backdrop');
  if (backdrop) backdrop.classList.remove('hidden');

  const el = (typeof target === 'string') ? document.querySelector(target) : target;
  if (!el) return;

  el.classList.remove('hidden');
  el.setAttribute('aria-hidden', 'false');

  // Inițializează picker + preview când se deschide modalul de anunț
  if (el.id === 'modal-announcement') {
    initAnnouncementModal();
  }
}
function closeModal(target) {
  const el = (typeof target === 'string') ? document.querySelector(target) : target;
  if (el) {
    el.classList.add('hidden');
    el.setAttribute('aria-hidden', 'true');
  }
  const anyOpen = Array.from(document.querySelectorAll('.modal')).some(m => !m.classList.contains('hidden'));
  if (!anyOpen) {
    const backdrop = document.getElementById('modal-backdrop');
    backdrop && backdrop.classList.add('hidden');
  }
}

// păstrează utilitarul tău, dar nu-l mai combina cu $:
function el(sel) { return (typeof sel === 'string') ? document.querySelector(sel) : sel; }

/* ========= Add Member Modal logic ========= */
function renderAddMemberList(convId) {
  const container = document.getElementById('add-member-list');
  if (!container) return;
  container.innerHTML = '';

  const conv = state.conversations.find(c => c.id === convId);
  if (!conv) return;

  const existingIds = new Set((conv.members || []).map(m => m.id));

  // Filtrăm utilizatorii care NU sunt în conversație
  const usersToAdd = state.users.filter(u => !existingIds.has(u.id) && u.username !== AI_USERNAME);

  if (usersToAdd.length === 0) {
    container.innerHTML = '<div class="subtitle" style="padding:10px;">Nu există utilizatori noi de adăugat.</div>';
    return;
  }

  usersToAdd.forEach(u => {
    const row = document.createElement('div');
    row.className = 'row';
    row.style.cssText = 'display:flex; justify-content:space-between; align-items:center; padding:12px 20px; border-bottom:1px solid var(--glass-stroke);';
    row.innerHTML = `
      <div style="display:flex;align-items:center;gap:12px;">
        <img src="${userAvatarUrl(u.username, u.avatarUrl)}" class="avatar" style="width:36px;height:36px;border-radius:50%;object-fit:cover;" alt=""/>
        <span style="font-weight:500; font-size:15px;">@${escapeHtml(u.username)}</span>
      </div>
      <button class="btn" style="padding:6px 16px; font-size:13px; background:var(--primary-600); color:white; border:none; border-radius:99px; cursor:pointer;">Adaugă</button>
    `;

    const btn = row.querySelector('button');

    // Hover effect manual pentru buton inline
    btn.onmouseover = () => btn.style.background = 'var(--primary-700)';
    btn.onmouseout = () => btn.style.background = 'var(--primary-600)';
    btn.onclick = async () => {
      try {
        const res = await api(`/api/conversations/${convId}/members`, {
          method: 'POST',
          body: JSON.stringify({ userIds: [u.id] })
        });

        closeModal('#modal-add-member');
        openAlertModal(`@${u.username} a fost adăugat!`);

        // Refresh UI imediat cu datele noi (type: group)
        if (res.conversation) {
          upsertConversationToTop(res.conversation);
          renderConversations();
          if (state.currentConvId === res.conversation.id) {
            renderChatHeader(res.conversation);
            if (state.ui.rightOpen) renderInfoPanel(res.conversation.id, true);
          }
        }
      } catch (e) {
        openAlertModal(e.message || 'Eroare');
      }
    };

    container.appendChild(row);
  });

  // Search filter simple
  const searchInput = document.getElementById('add-member-search');
  if (searchInput) {
    searchInput.value = ''; // reset search
    searchInput.oninput = (e) => {
      const term = e.target.value.toLowerCase();
      Array.from(container.children).forEach(row => {
        if (row.classList.contains('subtitle')) return;
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(term) ? 'flex' : 'none';
      });
    };
    // searchInput.focus(); // eliminat focus automat pe mobil
  }
}


/* ========= Boot ========= */
setupAuth();
loadToken();
if (state.token) { initApp(); } else { setAuthVisible(true); }