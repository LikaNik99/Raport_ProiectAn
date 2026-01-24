// ========================
// 🔗 Conexiune WebSocket universală (fără IP fix)
// ========================
const socket = io(window.location.origin);

socket.on("connect", () => {
  console.log("✅ Conectat la server WebSocket:", socket.id);
});
socket.on("disconnect", () => {
  console.warn("⚠️ Deconectat de la server");
});

// ========================
// Util: user curent (nume + rol + email din localStorage)
// ========================
function getCurrentUserName() {
  return localStorage.getItem('username') || 'Utilizator';
}
function getCurrentUserAvatar() {
  const img = document.querySelector('.user-avatar img');
  if (img && img.src) return img.src;
  return 'https://cdn-icons-png.flaticon.com/512/847/847969.png';
}
const CURRENT_USER = getCurrentUserName();
const CURRENT_ROLE = localStorage.getItem('role') || 'user';
let CURRENT_EMAIL = localStorage.getItem('email') || "";

// Server trimite info user la connect (pt. email/rol/nume)
socket.on("me:info", (me) => {
  if (me?.email) {
    CURRENT_EMAIL = me.email;
    localStorage.setItem('email', me.email);
  }
  if (me?.role && !localStorage.getItem('role')) {
    localStorage.setItem('role', me.role);
  }
  if (me?.name && !localStorage.getItem('username')) {
    localStorage.setItem('username', me.name);
  }
});

// ========================
// 🔹 Restul codului aplicației
// ========================
document.addEventListener('DOMContentLoaded', function() {
  // aici continui logica ta cu task-urile, formularele etc.
});

// ========================
// DOM Elements
// ========================
const themeSwitch = document.getElementById('theme-switch');
const addTaskBtn = document.getElementById('add-task-btn');
const addProjectBtn = document.getElementById('add-project-btn');
const sortTasksBtn = document.getElementById('sort-tasks-btn');
const taskModal = document.getElementById('task-modal');
const projectModal = document.getElementById('project-modal');
const sortModal = document.getElementById('sort-modal');
const closeBtns = document.querySelectorAll('.close-btn');
const taskForm = document.getElementById('task-form');
const projectForm = document.getElementById('project-form');
const tasksList = document.getElementById('tasks-list');
const projectsList = document.getElementById('projects-list');
const taskProjectSelect = document.getElementById('task-project');
const priorityFilter = document.getElementById('priority-filter');
const dateFilter = document.getElementById('date-filter');
const taskSearch = document.getElementById('task-search');
const currentViewElement = document.getElementById('current-view');
const totalTasksCount = document.getElementById('total-tasks-count');
const completedTasksCount = document.getElementById('completed-tasks-count');
const pendingTasksCount = document.getElementById('pending-tasks-count');

// View buttons
const allTasksBtn = document.getElementById('all-tasks');
const todayTasksBtn = document.getElementById('today-tasks');
const importantTasksBtn = document.getElementById('important-tasks');
const completedTasksBtn = document.getElementById('completed-tasks');
const notesTasksBtn = document.getElementById('notes-tasks');

// Sort modal elements
const applySortBtn = document.getElementById('apply-sort-btn');

// Notificări (bell)
const notifBtn = document.getElementById('notif-btn');
const notifBadge = document.getElementById('notif-badge');
const notifPanel = document.getElementById('notif-panel');
const notifList = document.getElementById('notif-list');
const notifMarkRead = document.getElementById('notif-mark-read');
const notifClearAll = document.getElementById('notif-clear-all');

// Rating modal (admin)
const ratingModal = document.getElementById('rating-modal');
const ratingClose = document.getElementById('rating-close');
const ratingStars = document.getElementById('rating-stars');
const ratingComment = document.getElementById('rating-comment');
const ratingSubmit = document.getElementById('rating-submit');

// ========================
// State
// ========================
let tasks = JSON.parse(localStorage.getItem('tasks')) || [];
let projects = JSON.parse(localStorage.getItem('projects')) || [];
let notifications = []; // {id,type,taskId,taskTitle,stars,comment,createdAt,read,closedBy,ratedByName,ratedByRole}
let currentView = 'all';
let currentSort = { by: 'dueDate', order: 'asc' };
let currentFilters = { priority: 'all', date: 'all' };
let searchQuery = '';
let ratingState = { taskId: null, stars: 0 };

// ========================
// Helpers
// ========================
function normalizeTask(t) {
  const claimedRaw = t.claimedBy || null;
  let claimedBy = null;
  if (claimedRaw) {
    if (typeof claimedRaw === 'string') {
      claimedBy = { name: claimedRaw };
    } else if (typeof claimedRaw === 'object') {
      claimedBy = { name: claimedRaw.name || String(claimedRaw), avatar: claimedRaw.avatar || null };
    }
  }

  // status: todo | in_progress | done
  const statusFromInput = (t.status || '').toString().toLowerCase().replace(/[\s-]+/g, '_');
  let status = ['todo', 'in_progress', 'done'].includes(statusFromInput) ? statusFromInput : null;
  if (!status) {
    if (t.isCompleted || t.completed) status = 'done';
    else if (t.claimedByEmail || claimedBy) status = 'in_progress';
    else status = 'todo';
  }
  const isCompleted = (t.isCompleted !== undefined ? !!t.isCompleted : !!t.completed) || status === 'done';

  return {
    id: (t.id !== undefined && t.id !== null) ? String(t.id) : generateId(),
    title: t.title ? String(t.title) : "(Fără titlu)",
    description: t.description ? String(t.description) : "",
    dueDate: t.dueDate || null,
    priority: t.priority || 'medium',
    project: t.project || null,
    labels: Array.isArray(t.labels) ? t.labels : (t.labels ? String(t.labels).split(',').map(s=>s.trim()).filter(Boolean) : []),
    isCompleted,
    isImportant: !!t.isImportant,
    createdAt: t.createdAt || new Date().toISOString(),
    claimedBy,
    claimedByEmail: t.claimedByEmail || null,
    status,

    // rating meta
    rating: typeof t.rating === 'number' ? t.rating : null,
    feedback: t.feedback || "",
    ratedBy: t.ratedBy || null,
    ratedByName: t.ratedByName || null,
    ratedByRole: t.ratedByRole || null,
    ratedAt: t.ratedAt || null,
    closedBy: t.closedBy || null
  };
}
function normalizeProject(p) {
  return {
    id: String(p.id || generateId()),
    name: String(p.name || "Project"),
    color: p.color || "#4e73df"
  };
}
function setTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('theme', theme);
}
function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2);
}
function escapeHtml(str) {
  if (str === undefined || str === null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
function starHTML(n) {
  const s = Math.max(1, Math.min(5, parseInt(n || 0, 10)));
  const full = "★".repeat(s);
  const empty = "☆".repeat(5 - s);
  return `<span class="stars" style="color:#f6b400">${full}${empty}</span>`;
}
function statusClass(s) {
  switch ((s || '').toLowerCase()) {
    case 'in_progress': return 'status-in-progress';
    case 'done': return 'status-done';
    default: return 'status-todo';
  }
}
function setStatusSelectStyle(selectEl) {
  if (!selectEl) return;
  ['status-todo','status-in-progress','status-done'].forEach(c => selectEl.classList.remove(c));
  const s = (selectEl.value || selectEl.getAttribute('data-prev') || 'todo');
  selectEl.classList.add(statusClass(s));
}

// ========================
// SOCKET: TASKS
// ========================
socket.on("tasks:init", (serverTasks) => {
  if (!Array.isArray(serverTasks)) return;
  tasks = serverTasks.map(t => normalizeTask(t));
  saveTasks();
  renderTasks();
  updateStats();
});
socket.on("task:created", (task) => {
  const nt = normalizeTask(task);
  const idx = tasks.findIndex(t => t.id === nt.id);
  if (idx === -1) tasks.push(nt); else tasks[idx] = nt;
  saveTasks(); renderTasks(); updateStats();
});
socket.on("task:updated", (task) => {
  const updated = normalizeTask(task);
  const idx = tasks.findIndex(t => t.id === updated.id);
  if (idx !== -1) tasks[idx] = updated; else tasks.push(updated);
  saveTasks(); renderTasks(); updateStats();
});
socket.on("task:deleted", (id) => {
  tasks = tasks.filter(t => t.id !== id);
  saveTasks(); renderTasks(); updateStats();
});

// realtime status sync (To Do / In Progress / Done)
socket.on("task:status", (payload) => {
  try {
    const { id, status, claimedBy, claimedByEmail } = payload || {};
    if (!id || !status) return;
    const idx = tasks.findIndex(t => String(t.id) === String(id));
    if (idx === -1) return;
    tasks[idx].status = status;
    tasks[idx].isCompleted = status === 'done';
    if (claimedBy !== undefined) tasks[idx].claimedBy = claimedBy;
    if (claimedByEmail !== undefined) tasks[idx].claimedByEmail = claimedByEmail;
    saveTasks(); renderTasks(); updateStats();
  } catch (e) { console.warn(e); }
});

// Nu mai afișăm alert-uri pentru erori — doar log
socket.on("error:permission", (msg) => console.warn("permission:", msg));
socket.on("error:validation", (msg) => console.warn("validation:", msg));

// ========================
// SOCKET: PROJECTS realtime
// ========================
socket.on("projects:init", (serverProjects) => {
  if (!Array.isArray(serverProjects)) return;
  projects = serverProjects.map(normalizeProject);
  saveProjects();
  renderProjects();
  renderTasks();
});
socket.on("project:created", (project) => {
  const p = normalizeProject(project);
  if (!projects.find(x => x.id === p.id)) projects.push(p);
  saveProjects(); renderProjects(); renderTasks();
});
socket.on("project:deleted", (id) => {
  projects = projects.filter(p => p.id !== id);
  tasks.forEach(task => { if (task.project === id) task.project = null; });
  saveProjects(); saveTasks(); renderProjects(); renderTasks();
});

// ========================
// SOCKET: NOTIFICĂRI
// ========================
socket.on("notify:bulk", (list) => {
  notifications = Array.isArray(list) ? list : [];
  renderNotifications();
});
socket.on("notify:new", (notif) => {
  notifications.push(notif);
  renderNotifications(true);
});

// ========================
// INIT
// ========================
init();
function init() {
  const savedTheme = localStorage.getItem('theme') || 'light';
  setTheme(savedTheme);
  if (themeSwitch) themeSwitch.checked = savedTheme === 'dark';

  renderTasks();
  renderProjects();
  updateStats();

  setupEventListeners();

  socket.emit("tasks:request");
  socket.emit("projects:request");
  socket.emit("notify:list:request");
  socket.emit("chat:request");
}

// ========================
// Listeners setup
// ========================
function setupEventListeners() {
  if (themeSwitch) {
    themeSwitch.addEventListener('change', function() { setTheme(this.checked ? 'dark' : 'light'); });
  }

  if (addTaskBtn) addTaskBtn.addEventListener('click', () => {
    if (CURRENT_ROLE !== 'admin') return;
    openTaskModal();
  });
  if (addProjectBtn) addProjectBtn.addEventListener('click', () => {
    if (CURRENT_ROLE !== 'admin') return;
    openProjectModal();
  });
  if (sortTasksBtn) sortTasksBtn.addEventListener('click', () => openSortModal());

  closeBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      const modal = this.closest('.modal');
      if (modal) modal.style.display = 'none';
    });
  });
  window.addEventListener('click', function(e) {
    const wrap = document.querySelector('.notif-wrapper');
    if (notifPanel && notifPanel.style.display === 'block' && wrap && !wrap.contains(e.target)) {
      notifPanel.style.display = 'none';
    }
    if (e.target.classList && e.target.classList.contains('modal')) {
      e.target.style.display = 'none';
    }
  });

  if (taskForm) taskForm.addEventListener('submit', handleTaskSubmit);
  if (projectForm) projectForm.addEventListener('submit', handleProjectSubmit);

  if (priorityFilter) priorityFilter.addEventListener('change', function(){ currentFilters.priority = this.value; renderTasks(); });
  if (dateFilter) dateFilter.addEventListener('change', function(){ currentFilters.date = this.value; renderTasks(); });
  if (taskSearch) taskSearch.addEventListener('input', function(){ searchQuery = this.value.toLowerCase(); renderTasks(); });

  if (allTasksBtn) allTasksBtn.addEventListener('click', () => setCurrentView('all'));
  if (todayTasksBtn) todayTasksBtn.addEventListener('click', () => setCurrentView('today'));
  if (importantTasksBtn) importantTasksBtn.addEventListener('click', () => setCurrentView('important'));
  if (completedTasksBtn) completedTasksBtn.addEventListener('click', () => setCurrentView('completed'));
  if (notesTasksBtn) notesTasksBtn.addEventListener('click', () => setCurrentView('notes'));

  if (applySortBtn) applySortBtn.addEventListener('click', applySort);

  // Notificări UI
  if (notifMarkRead) notifMarkRead.style.display = 'none';
  if (notifBtn) notifBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    if (!notifPanel) return;
    const visible = notifPanel.style.display === 'block';
    notifPanel.style.display = visible ? 'none' : 'block';
    const hadUnread = notifications.some(n => !n.read);
    if (hadUnread) {
      notifications = notifications.map(n => ({ ...n, read: true }));
      renderNotifications();
      socket.emit("notify:readAll");
    }
  });
  if (notifPanel) notifPanel.addEventListener('click', (e) => e.stopPropagation());
  if (notifClearAll) notifClearAll.addEventListener('click', () => {
    socket.emit("notify:clearAll");
    notifications = [];
    renderNotifications();
  });

  // Panel rate
  if (notifList) {
    notifList.addEventListener('click', (e) => {
      const btn = e.target.closest('.notif-rate');
      if (!btn) return;
      const taskId = btn.getAttribute('data-task-id');
      if (CURRENT_ROLE === 'admin' && taskId) openRatingModal(taskId);
    });
  }

  // Rating modal
  if (ratingClose) ratingClose.addEventListener('click', closeRatingModal);
  if (ratingStars) ratingStars.addEventListener('click', (e) => {
    const star = e.target?.getAttribute?.('data-star');
    if (!star) return;
    ratingState.stars = parseInt(star, 10);
    paintStars(ratingState.stars);
  });
  if (ratingSubmit) ratingSubmit.addEventListener('click', () => {
    if (!ratingState.taskId || !ratingState.stars) return alert("Alege un număr de stele (1-5).");
    const comment = (ratingComment?.value || "").trim();

    socket.emit("task:rate", { id: ratingState.taskId, stars: ratingState.stars, comment });

    notifications = notifications.filter(n => !(n.type === 'closed' && n.taskId === ratingState.taskId));
    renderNotifications();

    closeRatingModal();
  });
}

// Stars paint + open/close rating modal
function paintStars(n) {
  if (!ratingStars) return;
  const children = Array.from(ratingStars.querySelectorAll('[data-star]'));
  children.forEach(span => {
    const v = parseInt(span.getAttribute('data-star'), 10);
    span.style.color = v <= n ? '#f6b400' : '#ccc';
  });
}
function openRatingModal(taskId) {
  ratingState = { taskId, stars: 0 };
  if (ratingComment) ratingComment.value = "";
  paintStars(0);
  if (ratingModal) ratingModal.style.display = 'flex';
}
function closeRatingModal() {
  if (ratingModal) ratingModal.style.display = 'none';
  ratingState = { taskId: null, stars: 0 };
}

// ========================
// MODALS + Submit handlers (ADMIN ONLY edit)
// ========================
function openTaskModal(task = null) {
  const modalTitle = document.getElementById('modal-title');
  const taskIdInput = document.getElementById('task-id');
  const taskTitleInput = document.getElementById('task-title');
  const taskDescriptionInput = document.getElementById('task-description');
  const taskDueDateInput = document.getElementById('task-due-date');
  const taskPriorityInput = document.getElementById('task-priority');
  const taskProjectInput = document.getElementById('task-project');
  const taskLabelsInput = document.getElementById('task-labels');

  if (!taskForm || CURRENT_ROLE !== 'admin') return;

  taskForm.reset();

  if (task) {
    if (modalTitle) modalTitle.textContent = 'Edit Task';
    if (taskIdInput) taskIdInput.value = task.id;
    if (taskTitleInput) taskTitleInput.value = task.title;
    if (taskDescriptionInput) taskDescriptionInput.value = task.description || '';
    if (taskDueDateInput) taskDueDateInput.value = task.dueDate || '';
    if (taskPriorityInput) taskPriorityInput.value = task.priority;
    if (taskProjectInput) taskProjectInput.value = task.project || '';
    if (taskLabelsInput) taskLabelsInput.value = (task.labels || []).join(', ');
  } else {
    if (modalTitle) modalTitle.textContent = 'Add New Task';
    if (taskIdInput) taskIdInput.value = '';
    const today = new Date().toISOString().split('T')[0];
    if (taskDueDateInput) taskDueDateInput.value = today;
  }

  populateProjectDropdown();
  if (taskModal) taskModal.style.display = 'flex';
}

function openProjectModal() {
  if (!projectForm || !projectModal || CURRENT_ROLE !== 'admin') return;
  projectForm.reset();
  projectModal.style.display = 'flex';
}

function openSortModal() {
  try {
    const sortRadio = document.querySelector(`input[name="sort"][value="${currentSort.by}"]`);
    const orderRadio = document.querySelector(`input[name="order"][value="${currentSort.order}"]`);
    if (sortRadio) sortRadio.checked = true;
    if (orderRadio) orderRadio.checked = true;
  } catch (e) {}
  if (sortModal) sortModal.style.display = 'flex';
}
function applySort() {
  const sortByEl = document.querySelector('input[name="sort"]:checked');
  const sortOrderEl = document.querySelector('input[name="order"]:checked');
  if (sortByEl && sortOrderEl) {
    currentSort = { by: sortByEl.value, order: sortOrderEl.value };
    renderTasks();
  }
  if (sortModal) sortModal.style.display = 'none';
}

// ========================
// Projects dropdown
// ========================
function populateProjectDropdown() {
  if (!taskProjectSelect) return;
  taskProjectSelect.innerHTML = '<option value="">No Project</option>';
  projects.forEach(project => {
    const option = document.createElement('option');
    option.value = project.id;
    option.textContent = project.name;
    taskProjectSelect.appendChild(option);
  });
}

// ========================
// Handle Task Submit — ADMIN
// ========================
function handleTaskSubmit(e) {
  e.preventDefault();
  if (CURRENT_ROLE !== 'admin') return;

  const taskId = document.getElementById('task-id').value;
  const title = document.getElementById('task-title').value.trim();
  const description = document.getElementById('task-description').value.trim();
  const dueDate = document.getElementById('task-due-date').value;
  const priority = document.getElementById('task-priority').value;
  const projectId = document.getElementById('task-project').value;
  const labels = (document.getElementById('task-labels').value || '')
    .split(',')
    .map(label => label.trim())
    .filter(label => label);

  if (!title) { alert('Task title is required!'); return; }

  const serverPayload = {
    id: taskId || generateId(),
    title,
    description,
    dueDate: dueDate || null,
    priority,
    project: projectId || null,
    labels,
    completed: false,
    createdAt: new Date().toISOString(),
    claimedBy: null,
    status: 'todo'
  };

  const localTask = normalizeTask(serverPayload);

  if (taskId) {
    const existingTask = tasks.find(t => t.id === taskId);
    if (existingTask) {
      localTask.isCompleted = existingTask.isCompleted;
      localTask.isImportant = existingTask.isImportant;
      localTask.claimedBy = existingTask.claimedBy || null;
      localTask.claimedByEmail = existingTask.claimedByEmail || null;
      localTask.rating = existingTask.rating || null;
      localTask.feedback = existingTask.feedback || "";
      localTask.status = existingTask.status || (existingTask.isCompleted ? 'done' : (existingTask.claimedByEmail ? 'in_progress' : 'todo'));
      serverPayload.completed = !!existingTask.isCompleted;
      serverPayload.status = localTask.status;
      serverPayload.claimedBy = existingTask.claimedBy || null;
    }
    tasks = tasks.map(t => t.id === taskId ? localTask : t);
    socket.emit("task:update", serverPayload);
  } else {
    tasks.push(localTask);
    socket.emit("task:create", serverPayload);
  }

  saveTasks(); renderTasks(); updateStats();
  if (taskModal) taskModal.style.display = 'none';
}

// ========================
// Handle Project Submit — ADMIN (realtime)
// ========================
function handleProjectSubmit(e) {
  e.preventDefault();
  if (CURRENT_ROLE !== 'admin') return;

  const name = document.getElementById('project-name').value.trim();
  const color = document.getElementById('project-color').value || '#4e73df';

  if (!name) { alert('Project name is required!'); return; }

  socket.emit("project:create", { name, color });

  if (projectModal) projectModal.style.display = 'none';
}

// ========================
// View switching
// ========================
function setCurrentView(view) {
  currentView = view;

  document.querySelectorAll('.sidebar-menu button').forEach(btn => btn.classList.remove('active'));

  switch(view) {
    case 'all':
      if (allTasksBtn) allTasksBtn.classList.add('active');
      if (currentViewElement) currentViewElement.textContent = 'All Tasks';
      break;
    case 'today':
      if (todayTasksBtn) todayTasksBtn.classList.add('active');
      if (currentViewElement) currentViewElement.textContent = "Today's Tasks";
      break;
    case 'important':
      if (importantTasksBtn) importantTasksBtn.classList.add('active');
      if (currentViewElement) currentViewElement.textContent = 'Important Tasks';
      break;
    case 'completed':
      if (completedTasksBtn) completedTasksBtn.classList.add('active');
      if (currentViewElement) currentViewElement.textContent = 'Completed Tasks';
      break;
    case 'notes':
      if (notesTasksBtn) notesTasksBtn.classList.add('active');
      if (currentViewElement) currentViewElement.textContent = 'Notes';
      break;
  }

  renderTasks();
}

// ========================
// Render tasks (core)
// ========================
function renderTasks() {
  if (!tasksList) return;

  let filteredTasks = [...tasks];

  // View filter
  switch(currentView) {
    case 'today': {
      const today = new Date().toISOString().split('T')[0];
      filteredTasks = filteredTasks.filter(task => task.dueDate === today);
      break;
    }
    case 'important':
      filteredTasks = filteredTasks.filter(task => task.isImportant);
      break;
    case 'completed':
      filteredTasks = filteredTasks.filter(task => task.isCompleted);
      break;
    case 'notes':
      filteredTasks = filteredTasks.filter(task => task.isCompleted && typeof task.rating === 'number');
      if (CURRENT_ROLE !== 'admin') {
        const me = (CURRENT_EMAIL || "").toLowerCase();
        filteredTasks = filteredTasks.filter(t => (t.closedBy || "").toLowerCase() === me);
      }
      break;
    default:
      break;
  }

  // Ascunde task-urile completate din ORICE view, cu excepția "Completed" și "Notes"
  if (currentView !== 'completed' && currentView !== 'notes') {
    filteredTasks = filteredTasks.filter(task => !task.isCompleted);
  }

  // Priority filter
  if (currentFilters.priority !== 'all') {
    filteredTasks = filteredTasks.filter(task => task.priority === currentFilters.priority);
  }

  // Date filter
  if (currentFilters.date !== 'all') {
    const now = new Date();
    const nowStr = now.toISOString().split('T')[0];
    switch(currentFilters.date) {
      case 'today':
        filteredTasks = filteredTasks.filter(task => task.dueDate === nowStr);
        break;
      case 'week': {
        const nextWeek = new Date(now); nextWeek.setDate(now.getDate() + 7);
        filteredTasks = filteredTasks.filter(task => task.dueDate && new Date(task.dueDate) >= now && new Date(task.dueDate) <= nextWeek);
        break;
      }
      case 'month': {
        const nextMonth = new Date(now); nextMonth.setMonth(now.getMonth() + 1);
        filteredTasks = filteredTasks.filter(task => task.dueDate && new Date(task.dueDate) >= now && new Date(task.dueDate) <= nextMonth);
        break;
      }
      case 'overdue':
        filteredTasks = filteredTasks.filter(task => task.dueDate && new Date(task.dueDate) < now && !task.isCompleted);
        break;
    }
  }

  // Search
  if (searchQuery) {
    const q = searchQuery.toLowerCase();
    filteredTasks = filteredTasks.filter(task => {
      const titleMatch = (task.title || "").toLowerCase().includes(q);
      const descMatch = (task.description || "").toLowerCase().includes(q);
      const feedbackMatch = (task.feedback || "").toLowerCase().includes(q);
      return titleMatch || descMatch || feedbackMatch;
    });
  }

  // Sort
  filteredTasks.sort((a, b) => {
    let compareValue = 0;
    switch(currentSort.by) {
      case 'dueDate':
        compareValue = (a.dueDate ? new Date(a.dueDate) : new Date('9999-12-31')) - (b.dueDate ? new Date(b.dueDate) : new Date('9999-12-31'));
        break;
      case 'priority': {
        const ord = { high: 1, medium: 2, low: 3 };
        compareValue = (ord[a.priority] || 99) - (ord[b.priority] || 99);
        break;
      }
      case 'createdAt':
        compareValue = new Date(a.createdAt) - new Date(b.createdAt);
        break;
      case 'title':
        compareValue = (a.title || "").localeCompare(b.title || "");
        break;
    }
  return currentSort.order === 'asc' ? compareValue : -compareValue;
  });

  // Render
  if (filteredTasks.length === 0) {
    const emptyMsg =
      currentView === 'completed' ? 'No completed tasks yet.' :
      currentView === 'notes' ? 'No notes yet.' :
      (CURRENT_ROLE === 'admin'
        ? 'No tasks found. Add a new task or change your filters.'
        : 'No tasks found');

    tasksList.innerHTML = `
      <div class="empty-state">
        <i class="fas fa-clipboard-list"></i>
        <p>${emptyMsg}</p>
      </div>
    `;
    updateStats();
    return;
  }

  tasksList.innerHTML = '';
  filteredTasks.forEach(task => {
    const taskElement = document.createElement('div');
    taskElement.className = `task-card ${task.priority}-priority ${task.isCompleted ? 'completed' : ''}`;
    taskElement.dataset.id = task.id;

    let dueDateDisplay = 'No due date';
    if (task.dueDate) {
      const opts = { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' };
      try { dueDateDisplay = new Date(task.dueDate).toLocaleDateString(undefined, opts); } catch(e) { dueDateDisplay = task.dueDate; }
    }
    const priorityDisplay = (task.priority || 'low').charAt(0).toUpperCase() + (task.priority || 'low').slice(1);

    // Proiect
    const proj = task.project ? projects.find(p => String(p.id) === String(task.project)) : null;
    let projectDisplay = '';
    if (proj) {
      projectDisplay = `
        <div class="task-detail">
          <i class="fas fa-project-diagram"></i>
          <span class="task-project">
            <span class="project-color" style="background-color: ${proj.color || '#4e73df'}"></span>
            ${escapeHtml(proj.name)}
          </span>
        </div>
      `;
    }

    let labelsDisplay = '';
    if (task.labels && task.labels.length > 0) {
      labelsDisplay = `
        <div class="task-labels">
          ${task.labels.map(label => `<span class="task-label">${escapeHtml(label)}</span>`).join('')}
        </div>
      `;
    }

    const statusVal = task.status || (task.isCompleted ? 'done' : (task.claimedByEmail ? 'in_progress' : 'todo'));

    // NU permitem schimbare dacă task-ul e DONE (în Completed/Notes nu se poate redeschide)
    const canChangeStatus = !task.isCompleted && (
      CURRENT_ROLE === 'admin' ||
      !task.claimedByEmail ||
      (task.claimedByEmail && task.claimedByEmail === CURRENT_EMAIL)
    );

    const statusDropdown = `
      <select class="status-select ${statusClass(statusVal)}"
              data-task-id="${task.id}"
              data-prev="${statusVal}"
              ${canChangeStatus ? '' : 'disabled'}>
        <option value="todo" ${statusVal==='todo' ? 'selected' : ''}>To Do</option>
        <option value="in_progress" ${statusVal==='in_progress' ? 'selected' : ''}>In Progress</option>
        <option value="done" ${statusVal==='done' ? 'selected' : ''}>Done</option>
      </select>
    `;

    // CLAIM UI
    let claimSectionHtml = '';
    if (task.isCompleted) {
      if (task.claimedBy && task.claimedBy.name) {
        claimSectionHtml = `
          <div class="task-claimed" title="Taken by ${escapeHtml(task.claimedBy.name)}" style="display:flex;align-items:center;gap:8px;">
            <img src="${escapeHtml(task.claimedBy.avatar || getCurrentUserAvatar())}" alt="${escapeHtml(task.claimedBy.name)}" style="width:28px;height:28px;border-radius:50%;object-fit:cover;border:1px solid rgba(0,0,0,0.06)">
            <span style="font-size:12px;color:var(--muted)">Taken by: ${escapeHtml(task.claimedBy.name)}</span>
          </div>
        `;
      }
    } else {
      if (task.claimedBy && task.claimedBy.name) {
        const isMine = task.claimedByEmail && task.claimedByEmail === CURRENT_EMAIL;
        const releaseBtn = (isMine || CURRENT_ROLE === 'admin')
          ? `<button class="task-action-release" data-task-id="${task.id}" style="margin-left:8px;padding:6px 8px;border-radius:6px;border:1px solid rgba(0,0,0,0.08);background:transparent;cursor:pointer;font-size:13px;">Release</button>`
          : '';
        claimSectionHtml = `
          <div class="task-claimed" title="Taken by ${escapeHtml(task.claimedBy.name)}" style="display:flex;align-items:center;gap:8px;">
            <img src="${escapeHtml(task.claimedBy.avatar || getCurrentUserAvatar())}" alt="${escapeHtml(task.claimedBy.name)}" style="width:28px;height:28px;border-radius:50%;object-fit:cover;border:1px solid rgba(0,0,0,0.06)">
            <span style="font-size:12px;color:var(--muted)">Taken by: ${escapeHtml(task.claimedBy.name)}</span>
            ${releaseBtn}
          </div>
        `;
      } else {
        claimSectionHtml = `<button class="task-action-claim" data-task-id="${task.id}" style="margin-left:6px;padding:6px 8px;border-radius:6px;border:1px solid rgba(0,0,0,0.08);background:transparent;cursor:pointer;font-size:13px;">Take</button>`;
      }
    }

    // Rating UI (doar pe completed)
    let ratingHtml = '';
    if (task.isCompleted) {
      if (currentView === 'notes' && typeof task.rating === 'number') {
        const ratedByText = 'rated by:admin';
        ratingHtml = `
          <div class="task-rating" style="margin-top:6px;">
            Rating: ${starHTML(task.rating)}
            ${task.feedback ? `<span style="color:#777;font-size:12px;margin-left:6px;">— ${escapeHtml(task.feedback)}</span>` : ''}
            <div style="color:#666;font-size:12px;margin-top:4px;">${ratedByText}</div>
          </div>`;
      } else if (CURRENT_ROLE === 'admin' && typeof task.rating !== 'number') {
        ratingHtml = `<button class="task-action-btn btn-rate" data-task-id="${task.id}" title="Rate this task" style="margin-left:6px;"><i class="fas fa-star-half-alt"></i> Rate</button>`;
      }
    }

    // acțiuni — edit/delete doar pentru admin
    const actionsHtml = `
      <div class="task-actions-row">
        <button class="task-action-btn important ${task.isImportant ? 'active' : ''}" data-task-id="${task.id}">
          <i class="fas fa-star"></i>
        </button>
        ${CURRENT_ROLE === 'admin' ? `
        <button class="task-action-btn edit" data-task-id="${task.id}">
          <i class="fas fa-edit"></i>
        </button>
        <button class="task-action-btn delete" data-task-id="${task.id}">
          <i class="fas fa-trash"></i>
        </button>` : ``}
        ${ratingHtml}
      </div>
    `;

    taskElement.innerHTML = `
      <div class="task-header-row" style="display:flex;justify-content:space-between;align-items:center;">
        <div style="display:flex;align-items:center;gap:10px;">
          ${statusDropdown}
          <div class="task-title ${task.isCompleted ? 'completed' : ''}">
            ${escapeHtml(task.title)}
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:8px;">
          ${claimSectionHtml}
          ${actionsHtml}
        </div>
      </div>
      ${task.description ? `<div class="task-description">${escapeHtml(task.description)}</div>` : ''}
      <div class="task-details">
        <div class="task-detail">
          <i class="fas fa-calendar-alt"></i>
          <span>${dueDateDisplay}</span>
        </div>
        <div class="task-detail">
          <i class="fas fa-bolt"></i>
          <span class="task-priority ${task.priority}">${priorityDisplay}</span>
        </div>
        ${projectDisplay}
      </div>
      ${labelsDisplay}
    `;

    tasksList.appendChild(taskElement);
  });

  // Listeneri după render
  document.querySelectorAll('.complete-checkbox').forEach(checkbox => {
    checkbox.addEventListener('change', function() {
      const taskId = this.getAttribute('data-task-id');
      toggleTaskComplete(taskId, this);
    });
  });
  document.querySelectorAll('.important').forEach(btn => {
    btn.addEventListener('click', function() {
      const taskId = this.getAttribute('data-task-id');
      toggleTaskImportant(taskId);
    });
  });
  if (CURRENT_ROLE === 'admin') {
    document.querySelectorAll('.edit').forEach(btn => {
      btn.addEventListener('click', function() {
        const taskId = this.getAttribute('data-task-id');
        const task = tasks.find(t => t.id === taskId);
        if (task) openTaskModal(task);
      });
    });
    document.querySelectorAll('.delete').forEach(btn => {
      btn.addEventListener('click', function() {
        const taskId = this.getAttribute('data-task-id');
        if (confirm('Are you sure you want to delete this task?')) {
          deleteTask(taskId);
        }
      });
    });
    document.querySelectorAll('.btn-rate').forEach(btn => {
      btn.addEventListener('click', function() {
        const taskId = this.getAttribute('data-task-id');
        openRatingModal(taskId);
      });
    });
  }
  document.querySelectorAll('.task-action-claim').forEach(btn => {
    btn.addEventListener('click', function() {
      const taskId = this.getAttribute('data-task-id');
      claimTask(taskId);
    });
  });
  document.querySelectorAll('.task-action-release').forEach(btn => {
    btn.addEventListener('click', function() {
      const taskId = this.getAttribute('data-task-id');
      releaseTask(taskId);
    });
  });
  document.querySelectorAll('.status-select').forEach(sel => {
    setStatusSelectStyle(sel);
    sel.addEventListener('change', () => handleStatusChange(sel));
  });

  updateStats();
}

// ========================
// Projects render
// ========================
function renderProjects() {
  if (!projectsList) return;
  projectsList.innerHTML = '';

  if (projects.length === 0) {
    projectsList.innerHTML = '<p class="no-projects">No projects yet</p>';
    return;
  }

  projects.forEach(project => {
    const projectElement = document.createElement('div');
    projectElement.className = 'project-item';
    projectElement.innerHTML = `
      <span class="project-color" style="background-color: ${project.color}"></span>
      <span class="project-name">${escapeHtml(project.name)}</span>
      ${CURRENT_ROLE === 'admin' ? `
      <button class="delete-project" data-project-id="${project.id}">
        <i class="fas fa-times"></i>
      </button>` : ``}
    `;

    projectElement.addEventListener('click', function(e) {
      if (!e.target.classList.contains('delete-project') && !e.target.closest('.delete-project')) {
        currentView = 'all';
        currentFilters = { priority: 'all', date: 'all' };
        if (priorityFilter) priorityFilter.value = 'all';
        if (dateFilter) dateFilter.value = 'all';
        setCurrentView('all');
        const select = document.getElementById('task-project');
        if (select) select.value = project.id;
        renderTasks();
      }
    });

    if (CURRENT_ROLE === 'admin') {
      const deleteBtn = projectElement.querySelector('.delete-project');
      deleteBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        const projectId = this.getAttribute('data-project-id');
        if (confirm('Delete this project? Tasks in this project will not be deleted.')) {
          deleteProject(projectId);
        }
      });
    }

    projectsList.appendChild(projectElement);
  });

  populateProjectDropdown();
}

// ========================
// Actions
// ========================
function handleStatusChange(selectEl) {
  const taskId = selectEl.getAttribute('data-task-id');
  const newStatus = selectEl.value;
  const task = tasks.find(t => t.id === taskId);
  if (!task) return;

  const prevStatus = task.status || (task.isCompleted ? 'done' : (task.claimedByEmail ? 'in_progress' : 'todo'));

  // Done nu se redeschide (în Completed/Notes dropdown-ul e oricum disabled)
  if (task.isCompleted && newStatus !== 'done') {
    selectEl.value = prevStatus;
    setStatusSelectStyle(selectEl);
    return;
  }

  // dacă e preluat de altcineva (și nu e admin), revenim tăcut
  const isClaimedByOther = task.claimedByEmail && task.claimedByEmail !== CURRENT_EMAIL && CURRENT_ROLE !== 'admin';
  if (isClaimedByOther) {
    selectEl.value = prevStatus;
    setStatusSelectStyle(selectEl);
    return;
  }

  if (newStatus === 'done') {
    if (!task.isCompleted) {
      task.status = 'done';
      task.isCompleted = true;
      saveTasks();
      socket.emit("task:close", taskId); // server să reflecte la toți
    }
  } else if (newStatus === 'in_progress') {
    task.status = 'in_progress';
    task.isCompleted = false;

    // dacă nu e preluat, îl preia automat
    if (!task.claimedByEmail) {
      claimTask(taskId); // emite task:claim + status
    } else {
      socket.emit("task:status", { id: taskId, status: 'in_progress' });
      saveTasks();
    }
  } else { // 'todo'
    task.status = 'todo';
    task.isCompleted = false;
    saveTasks();
    socket.emit("task:status", { id: taskId, status: 'todo' });
  }

  selectEl.setAttribute('data-prev', newStatus);
  setStatusSelectStyle(selectEl);

  renderTasks();
  updateStats();
}

function toggleTaskComplete(taskId, checkboxEl) {
  const task = tasks.find(t => t.id === taskId);
  if (!task) return;

  if (task.isCompleted) {
    if (checkboxEl) { checkboxEl.checked = true; checkboxEl.disabled = true; }
    return;
  }

  task.isCompleted = true;
  task.status = 'done';
  saveTasks();
  renderTasks();
  updateStats();

  socket.emit("task:close", taskId);
}
function toggleTaskImportant(taskId) {
  const task = tasks.find(t => t.id === taskId);
  if (task) {
    task.isImportant = !task.isImportant;
    saveTasks();
    renderTasks();
  }
}
function deleteTask(taskId) {
  if (CURRENT_ROLE !== 'admin') return;
  tasks = tasks.filter(t => t.id !== taskId);
  saveTasks();
  renderTasks();
  updateStats();
  socket.emit("task:delete", taskId);
}
function deleteProject(projectId) {
  if (CURRENT_ROLE !== 'admin') return;
  projects = projects.filter(p => p.id !== projectId);
  tasks.forEach(task => {
    if (task.project === projectId) task.project = null;
  });
  saveProjects();
  saveTasks();
  renderProjects();
  renderTasks();
  socket.emit("project:delete", projectId);
}

// ========================
// CLAIM: take / release
// ========================
function claimTask(taskId) {
  const task = tasks.find(t => t.id === taskId);
  if (!task || task.isCompleted) return;

  if (task.claimedByEmail && task.claimedByEmail !== CURRENT_EMAIL && CURRENT_ROLE !== 'admin') {
    return;
  }
  const claimedBy = { name: CURRENT_USER, avatar: getCurrentUserAvatar() };

  task.claimedBy = claimedBy;
  task.claimedByEmail = CURRENT_EMAIL || null;
  if (!task.isCompleted) task.status = 'in_progress';

  saveTasks();
  renderTasks();
  updateStats();

  socket.emit("task:claim", { id: taskId, claimedBy });
  socket.emit("task:status", { id: taskId, status: 'in_progress', claimedBy, claimedByEmail: CURRENT_EMAIL || null });
}
function releaseTask(taskId) {
  const task = tasks.find(t => t.id === taskId);
  if (!task) return;

  if (task.claimedByEmail && task.claimedByEmail !== CURRENT_EMAIL && CURRENT_ROLE !== 'admin') {
    return;
  }
  if (task.isCompleted) return;

  task.claimedBy = null;
  task.claimedByEmail = null;
  if (!task.isCompleted) task.status = 'todo';

  saveTasks();
  renderTasks();
  updateStats();

  socket.emit("task:claim", { id: taskId, claimedBy: null });
  socket.emit("task:status", { id: taskId, status: 'todo', claimedBy: null, claimedByEmail: null });
}

// ========================
// Notificări — render + badge
// ========================
function renderNotifications() {
  const isClosedAndRated = (n) => {
    if (n.type !== 'closed') return false;
    const t = tasks.find(t => String(t.id) === String(n.taskId));
    return t && typeof t.rating === 'number';
  };

  const visible = notifications.filter(n => !isClosedAndRated(n));
  const unread = visible.filter(n => !n.read).length;

  if (notifBadge) {
    if (unread > 0) { notifBadge.textContent = unread; notifBadge.style.display = 'inline-block'; }
    else { notifBadge.style.display = 'none'; }
  }
  if (!notifList) return;
  if (!visible.length) {
    notifList.innerHTML = `<div style="padding:10px 12px;color:#666;">Nu ai notificări.</div>`;
    return;
  }
  notifList.innerHTML = visible.slice().reverse().map(n => {
    if (n.type === 'rating') {
      const ratedByText = 'rated by:admin';
      return `
        <div class="notif-item" style="padding:10px 12px;border-bottom:1px solid #f1f1f1;${!n.read ? 'background:#f9fbff;' : ''}">
          <div style="font-weight:600;">Task evaluat: ${escapeHtml(n.taskTitle || 'Task')}</div>
          <div style="margin:4px 0;">${starHTML(n.stars)} ${n.comment ? `<span style="color:#666;font-size:12px;margin-left:6px;">— ${escapeHtml(n.comment)}</span>` : ''}</div>
          <div style="font-size:12px;color:#666;margin:4px 0 0;">${ratedByText}</div>
          <div style="font-size:11px;color:#888;">${new Date(n.createdAt).toLocaleString()}</div>
        </div>`;
    }
    if (n.type === 'closed') {
      return `
        <div class="notif-item" style="padding:10px 12px;border-bottom:1px solid #f1f1f1;${!n.read ? 'background:#f9fbff;' : ''}">
          <div style="font-weight:600;">Task închis: ${escapeHtml(n.taskTitle || 'Task')}</div>
          <div style="font-size:12px;color:#666;">de: ${escapeHtml(n.closedBy || '')}</div>
          ${CURRENT_ROLE === 'admin' ? `<div style="margin-top:6px;"><button class="submit-btn notif-rate" data-task-id="${escapeHtml(n.taskId)}" style="padding:6px 10px;font-size:12px;">Rate</button></div>` : ``}
          <div style="font-size:11px;color:#888;margin-top:4px;">${new Date(n.createdAt).toLocaleString()}</div>
        </div>`;
    }
    return `<div class="notif-item" style="padding:10px 12px;border-bottom:1px solid #f1f1f1;">Notificare</div>`;
  }).join("");
}

// ========================
// Stats, save/load
// ========================
function updateStats() {
  const total = tasks.length;
  const completed = tasks.filter(t => t.isCompleted).length;
  const pending = total - completed;

  if (totalTasksCount) totalTasksCount.textContent = total;
  if (completedTasksCount) completedTasksCount.textContent = completed;
  if (pendingTasksCount) pendingTasksCount.textContent = pending;
}
function saveTasks() {
  try { localStorage.setItem('tasks', JSON.stringify(tasks)); }
  catch (e) { console.warn("⚠️ Couldn't save tasks to localStorage", e); }
}
function saveProjects() {
  try { localStorage.setItem('projects', JSON.stringify(projects)); }
  catch (e) { console.warn("⚠️ Couldn't save projects to localStorage", e); }
}

// ========================
// 💬 CHAT: UI + logic (floating bubble) + notificări
// ========================
(function injectChatStyles(){
  const css = `
  #chatBubble {
    position: fixed; bottom: 24px; right: 24px; width: 56px; height: 56px;
    border-radius: 50%; background: #4e73df; color: #fff; display: flex;
    align-items: center; justify-content: center; font-size: 24px; cursor: pointer;
    box-shadow: 0 6px 18px rgba(0,0,0,0.18); z-index: 9999;
  }
  #chatBubble.notify { animation: pulse-chat 1.2s infinite; }
  @keyframes pulse-chat {
    0% { transform: scale(1); box-shadow: 0 6px 18px rgba(0,0,0,0.18); }
    50% { transform: scale(1.06); box-shadow: 0 8px 24px rgba(0,0,0,0.24); }
    100% { transform: scale(1); box-shadow: 0 6px 18px rgba(0,0,0,0.18); }
  }
  #chatUnreadBadge {
    position: absolute; top: -6px; right: -6px; min-width: 18px; height: 18px;
    background: #e74c3c; color: #fff; border-radius: 9px; font-size: 11px; line-height: 18px;
    text-align: center; padding: 0 5px; display: none;
  }
  #chatBubble:hover { transform: scale(1.05); }
  #chatWindow {
    position: fixed; bottom: 96px; right: 24px; width: 280px; height: 320px;
    background: #fff; border-radius: 12px; box-shadow: 0 8px 30px rgba(0,0,0,0.2);
    display: flex; flex-direction: column; overflow: hidden; z-index: 9999; font-family: inherit;
  }
  #chatWindow.hidden { display: none; }
  .chat-header { background: #4e73df; color: #fff; padding: 10px 12px; display:flex; justify-content:space-between; align-items:center; font-weight:600; }
  .chat-messages { padding:10px; overflow-y:auto; flex:1; background:#f7f8fb; display:flex; flex-direction:column; gap:6px; }
  .chat-message { margin-bottom:0; max-width:85%; display:inline-block; padding:8px 10px; border-radius:8px; line-height:1.2; }
  .chat-message.self { background:#d1e7ff; align-self:flex-start; text-align:left; }
  .chat-message.other { background:#ececec; align-self:flex-end; text-align:right; }
  .chat-meta { display:block; font-size:11px; color:#666; margin-top:4px; }
  .chat-input { display:flex; border-top:1px solid #e6e6e6; }
  .chat-input input { flex:1; border:none; padding:10px; font-size:14px; outline:none; }
  .chat-input button { border:none; background:#4e73df; color:#fff; padding:10px 12px; cursor:pointer; }
  `;
  const style = document.createElement('style');
  style.setAttribute('data-generated','chat-styles');
  style.innerHTML = css;
  document.head.appendChild(style);
})();

// ========================
// 🎨 STATUS styles — toate stările transparente (text + border colorat)
// ========================
(function injectStatusStyles(){
  const css = `
    .status-select {
      appearance:none; -webkit-appearance:none; -moz-appearance:none;
      border:1px solid #e5e7eb; border-radius:8px; padding:6px 10px;
      font-size:13px; cursor:pointer; outline:none; font-weight:600;
      min-width:140px; background:transparent; color:#111827;
    }
    .status-select:disabled { opacity:0.6; cursor:not-allowed; }
    .status-select.status-todo { background:transparent; color:#2563eb; border-color:#60a5fa; }
    .status-select.status-in-progress { background:transparent; color:#b45309; border-color:#f59e0b; }
    .status-select.status-done { background:transparent; color:#047857; border-color:#10b981; }
  `;
  const el = document.createElement('style');
  el.setAttribute('data-generated','status-styles');
  el.innerHTML = css;
  document.head.appendChild(el);
})();

let chatBubble = document.getElementById('chatBubble');
if (!chatBubble) {
  chatBubble = document.createElement('div');
  chatBubble.id = 'chatBubble';
  chatBubble.textContent = '💬';
  document.body.appendChild(chatBubble);
}
let chatUnreadBadge = document.getElementById('chatUnreadBadge');
if (!chatUnreadBadge) {
  chatUnreadBadge = document.createElement('span');
  chatUnreadBadge.id = 'chatUnreadBadge';
  chatBubble.style.position = 'fixed';
  chatBubble.appendChild(chatUnreadBadge);
}

let chatWindow = document.getElementById('chatWindow');
if (!chatWindow) {
  chatWindow = document.createElement('div');
  chatWindow.id = 'chatWindow';
  chatWindow.className = 'hidden';
  chatWindow.innerHTML = `
    <div class="chat-header">
      <span>Chat echipă</span>
      <button id="chatCloseBtn" title="Close" style="background:transparent;border:none;color:white;font-size:16px;cursor:pointer;">✖</button>
    </div>
    <div id="chatMessages" class="chat-messages"></div>
    <div class="chat-input">
      <input id="chatInput" type="text" placeholder="Scrie un mesaj..." />
      <button id="chatSendBtn">Trimite</button>
    </div>
  `;
  document.body.appendChild(chatWindow);
}

const chatMessagesEl = document.getElementById('chatMessages');
const chatInputEl = document.getElementById('chatInput');
const chatSendBtn = document.getElementById('chatSendBtn');
const chatCloseBtn = document.getElementById('chatCloseBtn');

// Unread state
let chatMessages = [];
let chatUnread = 0;

function updateChatBadge() {
  if (!chatUnreadBadge) return;
  if (chatUnread > 0) {
    chatUnreadBadge.textContent = String(chatUnread);
    chatUnreadBadge.style.display = 'inline-block';
    chatBubble.classList.add('notify');
  } else {
    chatUnreadBadge.style.display = 'none';
    chatBubble.classList.remove('notify');
  }
}
function incrementChatUnread() {
  chatUnread++;
  updateChatBadge();
}
function resetChatUnread() {
  chatUnread = 0;
  updateChatBadge();
}
function showNativeNotification(title, body) {
  try {
    if (!("Notification" in window)) return;
    if (Notification.permission === 'granted') {
      new Notification(title, { body });
    } else if (Notification.permission === 'default') {
      Notification.requestPermission().then(p => {
        if (p === 'granted') new Notification(title, { body });
      });
    }
  } catch (e) {}
}

function isDuplicateChatMessage(msg) {
  if (!msg) return false;
  if (msg.id) {
    if (chatMessages.some(m => m.id === msg.id)) return true;
  }
  const user = (msg.user || '').toString();
  const text = (msg.text || '').toString();
  const time = (msg.time || '').toString();
  if (!user || !text) return false;
  const recent = chatMessages.slice(-20);
  for (let m of recent) {
    if ((m.user || '').toString() === user &&
        (m.text || '').toString() === text &&
        (m.time || '').toString() === time) {
      return true;
    }
  }
  return false;
}
function toggleChat() {
  if (!chatWindow) return;
  const willOpen = chatWindow.classList.contains('hidden');
  chatWindow.classList.toggle('hidden');
  if (willOpen) {
    resetChatUnread();
    setTimeout(() => { chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight; }, 50);
  }
}
function addChatMessageToDOM(msg, self = false) {
  if (!chatMessagesEl || !msg) return;
  if (isDuplicateChatMessage(msg)) return;
  chatMessages.push(msg);

  const el = document.createElement('div');
  const amSelf = (msg.user === CURRENT_USER);
  el.className = 'chat-message ' + (amSelf ? 'self' : 'other');

  const userHtml = `<strong style="font-size:13px;">${escapeHtml(msg.user)}</strong>`;
  const timeHtml = msg.time ? `<span style="font-size:11px;color:#666;margin-left:6px;">${escapeHtml(msg.time)}</span>` : '';
  el.innerHTML = `
    <div>${escapeHtml(msg.text)}</div>
    <div class="chat-meta">${userHtml} ${timeHtml}</div>
  `;
  chatMessagesEl.appendChild(el);
  chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;
}
function sendChatMessage() {
  const text = (chatInputEl && chatInputEl.value || '').trim();
  if (!text) return;
  const msg = {
    id: generateId(),
    user: CURRENT_USER,
    avatar: getCurrentUserAvatar(),
    text,
    time: new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})
  };
  addChatMessageToDOM(msg, true);
  socket.emit('chat:message', msg);
  if (chatInputEl) chatInputEl.value = '';
}
socket.on('chat:init', (msgs) => {
  if (!Array.isArray(msgs)) return;
  chatMessages = [];
  if (chatMessagesEl) chatMessagesEl.innerHTML = '';
  const last = msgs.slice(-200);
  last.forEach(m => {
    const msg = {
      id: m.id || m._id || generateId(),
      user: m.user || 'Unknown',
      text: m.text || '',
      time: m.time || ''
    };
    if (!isDuplicateChatMessage(msg)) {
      addChatMessageToDOM(msg, msg.user === CURRENT_USER);
    }
  });
});
socket.on('chat:message', (msg) => {
  if (!msg) return;
  const normalized = {
    id: msg.id || msg._id || undefined,
    user: msg.user || 'Unknown',
    text: msg.text || '',
    time: msg.time || new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})
  };
  if (isDuplicateChatMessage(normalized)) return;
  addChatMessageToDOM(normalized, normalized.user === CURRENT_USER);

  const isSelf = (normalized.user === CURRENT_USER);
  const isHidden = !chatWindow || chatWindow.classList.contains('hidden');
  if (!isSelf && isHidden) {
    incrementChatUnread();
    const body = `${normalized.user}: ${normalized.text}`.slice(0, 100);
    if (document.hidden || isHidden) showNativeNotification('New chat message', body);
  }
});

if (document.getElementById('chatBubble')) document.getElementById('chatBubble').addEventListener('click', toggleChat);
if (chatCloseBtn) chatCloseBtn.addEventListener('click', toggleChat);
if (chatSendBtn) chatSendBtn.addEventListener('click', sendChatMessage);
if (chatInputEl) {
  chatInputEl.addEventListener('keydown', function(e){
    if (e.key === 'Enter') {
      e.preventDefault();
      sendChatMessage();
    }
  });
}

// ========================
// Expose debug helpers (opțional)
// ========================
window.__TASKS_APP = {
  getTasks: () => tasks,
  setTasks: (arr) => { tasks = arr.map(normalizeTask); saveTasks(); renderTasks(); updateStats(); }
};