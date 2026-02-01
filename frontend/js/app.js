// ===== Fitness Tracker App =====
// Aplicație principală JavaScript - Versiune Română

const API_BASE = '/api';

// ===== Gestionare Stare =====
let currentUser = null;
let authToken = localStorage.getItem('fitTracker_token');
let weightChart = null;
let caloriesChart = null;

// ===== Funcții Utilitare =====
function showLoading() {
    document.getElementById('loadingOverlay').classList.add('active');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.remove('active');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span class="toast-message">${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

async function apiRequest(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers
        });

        const data = await response.json();

        if (!response.ok) {
            if (response.status === 401) {
                logout();
                throw new Error('Sesiune expirată. Te rugăm să te autentifici din nou.');
            }
            throw new Error(data.message || 'Cererea a eșuat');
        }

        return data;
    } catch (error) {
        console.error('Eroare API:', error);
        throw error;
    }
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ro-RO', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
    });
}

function getWorkoutIcon(type) {
    const icons = {
        cardio: '🏃',
        strength: '💪',
        hiit: '⚡',
        stretching: '🧘',
        other: '🏋️'
    };
    return icons[type] || '💪';
}

function getWorkoutTypeName(type) {
    const names = {
        cardio: 'Cardio',
        strength: 'Forță',
        hiit: 'HIIT',
        stretching: 'Stretching',
        other: 'Altele'
    };
    return names[type] || type;
}

function getGoalTypeName(type) {
    const names = {
        weight_loss: 'Slăbire',
        muscle_gain: 'Creștere Masă Musculară',
        endurance: 'Rezistență',
        workouts_per_week: 'Antrenamente/Săptămână',
        steps: 'Pași Zilnici',
        custom: 'Personalizat'
    };
    return names[type] || type;
}

// ===== Funcții Modal =====
function openModal(modalId) {
    document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// Închide modal la click pe overlay
document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            overlay.classList.remove('active');
        }
    });
});

// ===== Gestionare Temă =====
function initTheme() {
    const savedTheme = localStorage.getItem('fitTracker_theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
}

document.getElementById('themeSwitch')?.addEventListener('click', () => {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('fitTracker_theme', newTheme);
});

// ===== Navigare =====
function navigateTo(pageName) {
    // Actualizează link-urile de navigare
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
        if (link.dataset.page === pageName) {
            link.classList.add('active');
        }
    });

    // Actualizează secțiunile paginilor
    document.querySelectorAll('.page-section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(`${pageName}Page`).classList.add('active');

    // Încarcă datele paginii
    switch (pageName) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'workouts':
            loadWorkouts();
            break;
        case 'goals':
            loadGoals();
            break;
        case 'progress':
            loadProgress();
            break;
        case 'profile':
            loadProfile();
            break;
        case 'admin':
            loadAdmin();
            break;
    }

    // Închide meniul mobil
    document.getElementById('sidebar').classList.remove('open');
}

// Handler-uri click pentru link-uri de navigare
document.querySelectorAll('.nav-link, [data-page]').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const page = link.dataset.page;
        if (page) navigateTo(page);
    });
});

// Toggle meniu mobil
document.getElementById('mobileMenuToggle')?.addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
});

// ===== Autentificare =====
function showAuthSection() {
    document.getElementById('authSection').style.display = 'flex';
    document.getElementById('appSection').style.display = 'none';
}

function showAppSection() {
    document.getElementById('authSection').style.display = 'none';
    document.getElementById('appSection').style.display = 'flex';
    updateUserUI();
    navigateTo('dashboard');
}

async function checkAuth() {
    if (!authToken) {
        showAuthSection();
        return;
    }

    try {
        const response = await apiRequest('/auth/profile');
        currentUser = response.data;
        showAppSection();
    } catch (error) {
        authToken = null;
        localStorage.removeItem('fitTracker_token');
        showAuthSection();
    }
}

function updateUserUI() {
    if (!currentUser) return;

    const initials = currentUser.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
    document.getElementById('userAvatar').textContent = initials;
    document.getElementById('userName').textContent = currentUser.name;
    document.getElementById('userRole').textContent = currentUser.role === 'admin' ? 'Administrator' : 'Membru';

    // Afișează navigarea admin dacă e admin
    const adminNav = document.getElementById('adminNavItem');
    if (adminNav) {
        adminNav.style.display = currentUser.role === 'admin' ? 'block' : 'none';
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('fitTracker_token');
    showAuthSection();
    showToast('Te-ai deconectat cu succes');
}

document.getElementById('logoutBtn')?.addEventListener('click', logout);

// Tab-uri autentificare
document.querySelectorAll('.auth-tab').forEach(tab => {
    tab.addEventListener('click', () => {
        const tabName = tab.dataset.tab;

        document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        document.querySelectorAll('.auth-form').forEach(form => {
            form.classList.remove('active');
            if (form.dataset.form === tabName) {
                form.classList.add('active');
            }
        });
    });
});

// Formular autentificare
document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    showLoading();
    try {
        const response = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        authToken = response.data.token;
        currentUser = response.data.user;
        localStorage.setItem('fitTracker_token', authToken);

        showToast('Bine ai revenit!', 'success');
        showAppSection();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

// Formular înregistrare
document.getElementById('registerForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const data = {
        name: document.getElementById('registerName').value,
        email: document.getElementById('registerEmail').value,
        password: document.getElementById('registerPassword').value,
        age: parseInt(document.getElementById('registerAge').value) || null,
        sex: document.getElementById('registerSex').value || null,
        height: parseFloat(document.getElementById('registerHeight').value) || null,
        weight: parseFloat(document.getElementById('registerWeight').value) || null,
        activityLevel: document.getElementById('registerActivity').value
    };

    showLoading();
    try {
        const response = await apiRequest('/auth/register', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        authToken = response.data.token;
        currentUser = response.data.user;
        localStorage.setItem('fitTracker_token', authToken);

        showToast('Cont creat cu succes!', 'success');
        showAppSection();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

// ===== Panou Principal =====
async function loadDashboard() {
    try {
        const response = await apiRequest('/progress/dashboard');
        const data = response.data;

        // Actualizează statisticile
        document.getElementById('weekWorkouts').textContent = data.weekSummary.workouts;
        document.getElementById('weekDuration').textContent = data.weekSummary.duration;
        document.getElementById('weekCalories').textContent = data.weekSummary.calories.toLocaleString('ro-RO');
        document.getElementById('currentWeight').textContent = data.currentWeight || '--';

        // Actualizează antrenamentele recente
        const workoutsList = document.getElementById('recentWorkouts');
        if (data.recentWorkouts && data.recentWorkouts.length > 0) {
            workoutsList.innerHTML = data.recentWorkouts.map(workout => `
                <div class="workout-item">
                    <div class="workout-icon ${workout.type}">${getWorkoutIcon(workout.type)}</div>
                    <div class="workout-details">
                        <div class="workout-type">${getWorkoutTypeName(workout.type)}</div>
                        <div class="workout-meta">
                            <span>${workout.durationMinutes} min</span>
                            <span>${workout.caloriesBurned || 0} cal</span>
                            <span>${formatDate(workout.workoutDate)}</span>
                        </div>
                    </div>
                </div>
            `).join('');
        } else {
            workoutsList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">💪</div>
                    <h4 class="empty-title">Niciun antrenament încă</h4>
                    <p class="empty-text">Începe să îți înregistrezi antrenamentele pentru a le vedea aici</p>
                </div>
            `;
        }

        // Actualizează recomandările
        const recommendationsList = document.getElementById('recommendations');
        if (data.recommendations && data.recommendations.length > 0) {
            recommendationsList.innerHTML = data.recommendations.map(rec => `
                <div class="recommendation-item">
                    <span class="recommendation-icon">${rec.icon}</span>
                    <p class="recommendation-text">${translateRecommendation(rec.message)}</p>
                </div>
            `).join('');
        }
    } catch (error) {
        console.error('Eroare la încărcarea panoului principal:', error);
    }
}

function translateRecommendation(message) {
    // Traduceri pentru recomandări comune
    const translations = {
        "Start your week strong! Log your first workout today.": "Începe săptămâna în forță! Înregistrează primul antrenament azi.",
        "Great job!": "Excelent!",
        "Keep it up!": "Continuă tot așa!",
        "Consider adding a stretching session for recovery.": "Ia în considerare o sesiune de stretching pentru recuperare.",
        "Add some cardio to improve your endurance!": "Adaugă cardio pentru a-ți îmbunătăți rezistența!"
    };

    // Caută traduceri parțiale
    for (const [en, ro] of Object.entries(translations)) {
        if (message.includes(en)) {
            message = message.replace(en, ro);
        }
    }

    // Traduceri pentru pattern-uri
    message = message.replace(/You've done (\d+) workout\(s\) this week\. Aim for at least 3!/g,
        'Ai făcut $1 antrenament(e) săptămâna aceasta. Țintește cel puțin 3!');
    message = message.replace(/(\d+) workouts this week/g, '$1 antrenamente săptămâna aceasta');

    return message;
}

// Buton antrenament rapid
document.getElementById('quickWorkoutBtn')?.addEventListener('click', () => {
    openWorkoutModal();
});

// ===== Antrenamente =====
async function loadWorkouts() {
    const type = document.getElementById('workoutTypeFilter').value;
    const date = document.getElementById('workoutDateFilter').value;

    let endpoint = '/workouts?limit=50';
    if (type) endpoint += `&type=${type}`;
    if (date) endpoint += `&startDate=${date}&endDate=${date}`;

    try {
        const response = await apiRequest(endpoint);
        const workouts = response.data;

        const workoutsList = document.getElementById('workoutsList');
        if (workouts && workouts.length > 0) {
            workoutsList.innerHTML = workouts.map(workout => `
                <div class="workout-item" data-id="${workout.id}">
                    <div class="workout-icon ${workout.type}">${getWorkoutIcon(workout.type)}</div>
                    <div class="workout-details">
                        <div class="workout-type">${getWorkoutTypeName(workout.type)}</div>
                        <div class="workout-meta">
                            <span>⏱️ ${workout.durationMinutes} min</span>
                            <span>🔥 ${workout.caloriesBurned || 0} cal</span>
                            <span>📅 ${formatDate(workout.workoutDate)}</span>
                        </div>
                        ${workout.notes ? `<p style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.5rem;">${workout.notes}</p>` : ''}
                    </div>
                    <div class="workout-actions">
                        <button class="btn btn-icon btn-secondary" onclick="editWorkout(${workout.id})">✏️</button>
                        <button class="btn btn-icon btn-danger" onclick="deleteWorkout(${workout.id})">🗑️</button>
                    </div>
                </div>
            `).join('');
        } else {
            workoutsList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">🏋️</div>
                    <h4 class="empty-title">Nu s-au găsit antrenamente</h4>
                    <p class="empty-text">Adaugă primul tău antrenament pentru a începe</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Eroare la încărcarea antrenamentelor:', error);
        showToast('Eroare la încărcarea antrenamentelor', 'error');
    }
}

function openWorkoutModal(workout = null) {
    const form = document.getElementById('workoutForm');
    const title = document.getElementById('workoutModalTitle');

    if (workout) {
        title.textContent = 'Editează Antrenament';
        document.getElementById('workoutId').value = workout.id;
        document.getElementById('workoutType').value = workout.type;
        document.getElementById('workoutDuration').value = workout.durationMinutes;
        document.getElementById('workoutCalories').value = workout.caloriesBurned || '';
        document.getElementById('workoutDate').value = workout.workoutDate;
        document.getElementById('workoutNotes').value = workout.notes || '';
    } else {
        title.textContent = 'Adaugă Antrenament';
        form.reset();
        document.getElementById('workoutId').value = '';
        document.getElementById('workoutDate').value = new Date().toISOString().split('T')[0];
    }

    openModal('workoutModal');
}

async function editWorkout(id) {
    try {
        const response = await apiRequest(`/workouts/${id}`);
        openWorkoutModal(response.data);
    } catch (error) {
        showToast('Eroare la încărcarea antrenamentului', 'error');
    }
}

async function deleteWorkout(id) {
    if (!confirm('Ești sigur că vrei să ștergi acest antrenament?')) return;

    try {
        await apiRequest(`/workouts/${id}`, { method: 'DELETE' });
        showToast('Antrenament șters', 'success');
        loadWorkouts();
    } catch (error) {
        showToast('Eroare la ștergerea antrenamentului', 'error');
    }
}

document.getElementById('workoutForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const id = document.getElementById('workoutId').value;
    const data = {
        type: document.getElementById('workoutType').value,
        durationMinutes: parseInt(document.getElementById('workoutDuration').value),
        caloriesBurned: parseInt(document.getElementById('workoutCalories').value) || null,
        workoutDate: document.getElementById('workoutDate').value,
        notes: document.getElementById('workoutNotes').value || null
    };

    showLoading();
    try {
        if (id) {
            await apiRequest(`/workouts/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
            showToast('Antrenament actualizat!', 'success');
        } else {
            await apiRequest('/workouts', {
                method: 'POST',
                body: JSON.stringify(data)
            });
            showToast('Antrenament înregistrat!', 'success');
        }

        closeModal('workoutModal');
        loadWorkouts();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

document.getElementById('addWorkoutBtn')?.addEventListener('click', () => openWorkoutModal());

document.getElementById('workoutTypeFilter')?.addEventListener('change', loadWorkouts);
document.getElementById('workoutDateFilter')?.addEventListener('change', loadWorkouts);
document.getElementById('clearFiltersBtn')?.addEventListener('click', () => {
    document.getElementById('workoutTypeFilter').value = '';
    document.getElementById('workoutDateFilter').value = '';
    loadWorkouts();
});

// ===== Obiective =====
async function loadGoals() {
    try {
        const response = await apiRequest('/goals');
        const goals = response.data;

        const goalsList = document.getElementById('goalsList');
        if (goals && goals.length > 0) {
            goalsList.innerHTML = goals.map(goal => {
                const progress = goal.targetValue > 0
                    ? Math.min(100, Math.round((goal.currentValue / goal.targetValue) * 100))
                    : 0;
                const progressClass = goal.isCompleted ? 'success' : 'primary';

                return `
                    <div class="goal-card" data-id="${goal.id}">
                        <div class="goal-header">
                            <div>
                                <div class="goal-type">${getGoalTypeName(goal.goalType)}</div>
                                ${goal.description ? `<p style="font-size: 0.75rem; color: var(--text-muted);">${goal.description}</p>` : ''}
                            </div>
                            <div>
                                ${goal.isCompleted
                        ? '<span class="badge badge-success">✓ Completat</span>'
                        : '<span class="badge badge-primary">În Progres</span>'}
                            </div>
                        </div>
                        <div class="goal-progress-text">
                            <span>${goal.currentValue} / ${goal.targetValue} ${goal.targetUnit}</span>
                            <span>${progress}%</span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill ${progressClass}" style="width: ${progress}%"></div>
                        </div>
                        ${goal.deadline ? `
                            <div class="goal-deadline">
                                <span>📅</span>
                                <span>Termen limită: ${formatDate(goal.deadline)}</span>
                            </div>
                        ` : ''}
                        <div style="display: flex; gap: 0.5rem; margin-top: 1rem;">
                            <button class="btn btn-secondary btn-sm" onclick="updateGoalProgress(${goal.id}, ${goal.currentValue})">Actualizează Progres</button>
                            <button class="btn btn-icon btn-secondary btn-sm" onclick="editGoal(${goal.id})">✏️</button>
                            <button class="btn btn-icon btn-danger btn-sm" onclick="deleteGoal(${goal.id})">🗑️</button>
                        </div>
                    </div>
                `;
            }).join('');
        } else {
            goalsList.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div class="empty-icon">🎯</div>
                    <h4 class="empty-title">Niciun obiectiv setat</h4>
                    <p class="empty-text">Creează primul tău obiectiv fitness</p>
                    <button class="btn btn-primary" onclick="openGoalModal()">Adaugă Obiectiv</button>
                </div>
            `;
        }
    } catch (error) {
        console.error('Eroare la încărcarea obiectivelor:', error);
        showToast('Eroare la încărcarea obiectivelor', 'error');
    }
}

function openGoalModal(goal = null) {
    const form = document.getElementById('goalForm');
    const title = document.getElementById('goalModalTitle');

    if (goal) {
        title.textContent = 'Editează Obiectiv';
        document.getElementById('goalId').value = goal.id;
        document.getElementById('goalType').value = goal.goalType;
        document.getElementById('goalTarget').value = goal.targetValue;
        document.getElementById('goalUnit').value = goal.targetUnit;
        document.getElementById('goalCurrent').value = goal.currentValue;
        document.getElementById('goalDeadline').value = goal.deadline || '';
        document.getElementById('goalDescription').value = goal.description || '';
    } else {
        title.textContent = 'Adaugă Obiectiv';
        form.reset();
        document.getElementById('goalId').value = '';
    }

    openModal('goalModal');
}

async function editGoal(id) {
    try {
        const response = await apiRequest(`/goals/${id}`);
        openGoalModal(response.data);
    } catch (error) {
        showToast('Eroare la încărcarea obiectivului', 'error');
    }
}

async function deleteGoal(id) {
    if (!confirm('Ești sigur că vrei să ștergi acest obiectiv?')) return;

    try {
        await apiRequest(`/goals/${id}`, { method: 'DELETE' });
        showToast('Obiectiv șters', 'success');
        loadGoals();
    } catch (error) {
        showToast('Eroare la ștergerea obiectivului', 'error');
    }
}

async function updateGoalProgress(id, currentValue) {
    const newValue = prompt('Introdu noua valoare a progresului:', currentValue);
    if (newValue === null) return;

    try {
        await apiRequest(`/goals/${id}/progress`, {
            method: 'PUT',
            body: JSON.stringify({ currentValue: parseFloat(newValue) })
        });
        showToast('Progres actualizat!', 'success');
        loadGoals();
    } catch (error) {
        showToast('Eroare la actualizarea progresului', 'error');
    }
}

document.getElementById('goalForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const id = document.getElementById('goalId').value;
    const data = {
        goalType: document.getElementById('goalType').value,
        targetValue: parseFloat(document.getElementById('goalTarget').value),
        targetUnit: document.getElementById('goalUnit').value,
        currentValue: parseFloat(document.getElementById('goalCurrent').value) || 0,
        deadline: document.getElementById('goalDeadline').value || null,
        description: document.getElementById('goalDescription').value || null
    };

    showLoading();
    try {
        if (id) {
            await apiRequest(`/goals/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
            showToast('Obiectiv actualizat!', 'success');
        } else {
            await apiRequest('/goals', {
                method: 'POST',
                body: JSON.stringify(data)
            });
            showToast('Obiectiv creat!', 'success');
        }

        closeModal('goalModal');
        loadGoals();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

document.getElementById('addGoalBtn')?.addEventListener('click', () => openGoalModal());

// ===== Progres =====
async function loadProgress() {
    const period = document.getElementById('weightPeriod')?.value || 'month';

    try {
        const response = await apiRequest(`/progress/stats?period=${period}`);
        const stats = response.data;

        // Actualizează statisticile
        document.getElementById('weightChange').textContent = stats.weight.change !== null
            ? (stats.weight.change > 0 ? '+' : '') + stats.weight.change
            : '--';
        document.getElementById('totalWorkoutsAll').textContent = stats.workouts.total;
        document.getElementById('totalCaloriesAll').textContent = stats.workouts.totalCalories.toLocaleString('ro-RO');

        // Actualizează graficul greutății
        updateWeightChart(stats.weight.history);

        // Actualizează graficul caloriilor
        updateCaloriesChart(stats.calories.history);
    } catch (error) {
        console.error('Eroare la încărcarea progresului:', error);
    }
}

function updateWeightChart(data) {
    const ctx = document.getElementById('weightChart');
    if (!ctx) return;

    if (weightChart) {
        weightChart.destroy();
    }

    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? '#334155' : '#e2e8f0';

    weightChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.map(d => formatDate(d.date)),
            datasets: [{
                label: 'Greutate (kg)',
                data: data.map(d => d.weight),
                borderColor: '#6366f1',
                backgroundColor: 'rgba(99, 102, 241, 0.1)',
                fill: true,
                tension: 0.4,
                pointRadius: 4,
                pointBackgroundColor: '#6366f1'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                x: {
                    grid: { color: gridColor },
                    ticks: { color: textColor }
                },
                y: {
                    grid: { color: gridColor },
                    ticks: { color: textColor }
                }
            }
        }
    });
}

function updateCaloriesChart(data) {
    const ctx = document.getElementById('caloriesChart');
    if (!ctx) return;

    if (caloriesChart) {
        caloriesChart.destroy();
    }

    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? '#334155' : '#e2e8f0';

    caloriesChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.map(d => formatDate(d.date)),
            datasets: [{
                label: 'Calorii Arse',
                data: data.map(d => d.calories),
                backgroundColor: 'rgba(16, 185, 129, 0.7)',
                borderColor: '#10b981',
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: textColor }
                },
                y: {
                    grid: { color: gridColor },
                    ticks: { color: textColor }
                }
            }
        }
    });
}

document.getElementById('weightPeriod')?.addEventListener('change', loadProgress);

document.getElementById('recordProgressBtn')?.addEventListener('click', () => {
    document.getElementById('progressForm').reset();
    document.getElementById('progressDate').value = new Date().toISOString().split('T')[0];
    if (currentUser?.weight) {
        document.getElementById('progressWeight').value = currentUser.weight;
    }
    openModal('progressModal');
});

document.getElementById('progressForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const data = {
        weight: parseFloat(document.getElementById('progressWeight').value),
        recordDate: document.getElementById('progressDate').value,
        notes: document.getElementById('progressNotes').value || null
    };

    showLoading();
    try {
        await apiRequest('/progress', {
            method: 'POST',
            body: JSON.stringify(data)
        });

        showToast('Progres înregistrat!', 'success');
        closeModal('progressModal');
        loadProgress();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

// ===== Profil =====
function loadProfile() {
    if (!currentUser) return;

    document.getElementById('profileName').value = currentUser.name || '';
    document.getElementById('profileEmail').value = currentUser.email || '';
    document.getElementById('profileAge').value = currentUser.age || '';
    document.getElementById('profileSex').value = currentUser.sex || '';
    document.getElementById('profileHeight').value = currentUser.height || '';
    document.getElementById('profileWeight').value = currentUser.weight || '';
    document.getElementById('profileActivity').value = currentUser.activityLevel || 'beginner';
}

document.getElementById('profileForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const data = {
        name: document.getElementById('profileName').value,
        age: parseInt(document.getElementById('profileAge').value) || null,
        sex: document.getElementById('profileSex').value || null,
        height: parseFloat(document.getElementById('profileHeight').value) || null,
        weight: parseFloat(document.getElementById('profileWeight').value) || null,
        activityLevel: document.getElementById('profileActivity').value
    };

    showLoading();
    try {
        const response = await apiRequest('/auth/profile', {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        currentUser = response.data;
        updateUserUI();
        showToast('Profil actualizat!', 'success');
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

document.getElementById('passwordForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (newPassword !== confirmPassword) {
        showToast('Parolele nu coincid', 'error');
        return;
    }

    const data = {
        currentPassword: document.getElementById('currentPassword').value,
        newPassword: newPassword
    };

    showLoading();
    try {
        await apiRequest('/auth/password', {
            method: 'PUT',
            body: JSON.stringify(data)
        });

        showToast('Parolă schimbată cu succes!', 'success');
        document.getElementById('passwordForm').reset();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        hideLoading();
    }
});

// ===== Admin =====
async function loadAdmin() {
    if (currentUser?.role !== 'admin') return;

    try {
        // Încarcă statisticile
        const statsResponse = await apiRequest('/admin/stats');
        const stats = statsResponse.data;

        document.getElementById('totalUsers').textContent = stats.users.total;
        document.getElementById('activeUsers').textContent = stats.users.active;
        document.getElementById('totalPlatformWorkouts').textContent = stats.workouts.total;
        document.getElementById('goalCompletionRate').textContent = stats.goals.completionRate + '%';

        // Încarcă utilizatorii
        await loadUsers();
    } catch (error) {
        console.error('Eroare la încărcarea datelor admin:', error);
    }
}

async function loadUsers(search = '') {
    try {
        let endpoint = '/admin/users?limit=50';
        if (search) endpoint += `&search=${encodeURIComponent(search)}`;

        const response = await apiRequest(endpoint);
        const users = response.data;

        const tbody = document.getElementById('usersTableBody');
        tbody.innerHTML = users.map(user => `
            <tr>
                <td>
                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <div class="user-avatar" style="width: 32px; height: 32px; font-size: 0.75rem;">
                            ${user.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)}
                        </div>
                        <span>${user.name}</span>
                    </div>
                </td>
                <td>${user.email}</td>
                <td><span class="badge ${user.role === 'admin' ? 'badge-primary' : 'badge-secondary'}">${user.role === 'admin' ? 'Admin' : 'Utilizator'}</span></td>
                <td>
                    <span class="user-status-badge">
                        <span class="status-dot ${user.isBlocked ? 'blocked' : 'active'}"></span>
                        ${user.isBlocked ? 'Blocat' : 'Activ'}
                    </span>
                </td>
                <td>${formatDate(user.createdAt)}</td>
                <td>
                    ${user.role !== 'admin' ? `
                        <button class="btn btn-sm ${user.isBlocked ? 'btn-success' : 'btn-secondary'}" 
                            onclick="toggleBlockUser(${user.id})">
                            ${user.isBlocked ? 'Deblochează' : 'Blochează'}
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteUser(${user.id})">Șterge</button>
                    ` : '<span class="text-muted">—</span>'}
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Eroare la încărcarea utilizatorilor:', error);
    }
}

async function toggleBlockUser(id) {
    try {
        await apiRequest(`/admin/users/${id}/block`, { method: 'PUT' });
        showToast('Status utilizator actualizat', 'success');
        loadUsers(document.getElementById('userSearch')?.value);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function deleteUser(id) {
    if (!confirm('Ești sigur că vrei să ștergi acest utilizator? Această acțiune nu poate fi anulată.')) return;

    try {
        await apiRequest(`/admin/users/${id}`, { method: 'DELETE' });
        showToast('Utilizator șters', 'success');
        loadAdmin();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

let searchTimeout;
document.getElementById('userSearch')?.addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        loadUsers(e.target.value);
    }, 300);
});

// ===== Inițializare Aplicație =====
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    checkAuth();
});

// Funcții accesibile global
window.openModal = openModal;
window.closeModal = closeModal;
window.openWorkoutModal = openWorkoutModal;
window.editWorkout = editWorkout;
window.deleteWorkout = deleteWorkout;
window.openGoalModal = openGoalModal;
window.editGoal = editGoal;
window.deleteGoal = deleteGoal;
window.updateGoalProgress = updateGoalProgress;
window.toggleBlockUser = toggleBlockUser;
window.deleteUser = deleteUser;
