// Funcționalitate buton "Creare articol" (mutat din HTML în JS)
document.addEventListener('DOMContentLoaded', function () {
    var createBtn = document.getElementById('create-btn');
    if (createBtn) {
        createBtn.addEventListener('click', function () {
            if (document.querySelector('.modal')) return;
            const modalBg = document.createElement('div');
            modalBg.className = 'modal';
            modalBg.style = 'position:absolute;top:25%;left:40%;'
            modalBg.innerHTML = `
            <div class="modal-content" style="background:#232533;color:#e3e6f3;border-radius:18px;padding:36px 36px 24px 36px;min-width:400px;max-width:95vw;box-shadow:0 8px 32px #0008;position:relative;display:flex;flex-direction:column;gap:24px;">
                    <span class="close" onclick="document.querySelector('.modal').remove()" style="position:absolute;top:18px;right:24px;font-size:2rem;cursor:pointer;color:#bbb;">&times;</span>
                    <h2 style="margin-top:0;margin-bottom:10px;font-size:1.6em;font-weight:800;letter-spacing:0.01em;text-align:center;">New Conectare</h2>
                    <div style="display:flex;justify-content:center;align-items:center;margin-bottom:10px;">
                        <input type="text" id="itemName" placeholder="Item name (necesar)" required style="font-size:1.25em;font-weight:700;color:#6d8cff;background:#23283a;border-radius:10px;border:2px solid #23283a;outline:none;width:90%;max-width:340px;padding:12px 18px;text-align:center;box-shadow:0 2px 8px #0002;transition:border 0.2s,box-shadow 0.2s;">
                    </div>
                    <form id="addItemForm" class="styled-form" style="display:flex;flex-direction:column;gap:24px;">
                            <div style="display:flex;gap:24px;flex-wrap:wrap;">
                                    <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                                            <div style="font-weight:600;margin-bottom:0.5em;">Date de logare</div>
                                            <div style="color:#bdbdbd;font-size:0.97em;">Nume utilizator</div>
                                            <div style="display:flex;align-items:center;gap:8px;">
                                                    <input type="text" id="itemUser" placeholder="Nume utilizator" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                                            </div>
                                            <div style="color:#bdbdbd;font-size:0.97em;margin-top:0.7em;">Parolă</div>
                                            <div style="display:flex;align-items:center;gap:8px;">
                                                    <input type="password" id="itemPassword" placeholder="Parolă" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100px;">
                                                    <button type="button" class="icon-btn" id="pwToggleBtn" tabindex="-1" style="background:none;border:none;color:#8ab4f8;font-size:1.1em;cursor:pointer;">👁️</button>
                                                    <button type="button" class="icon-btn" id="pwGenBtn" title="Generează parolă" tabindex="-1" style="background:none;border:none;color:#8ab4f8;font-size:1.1em;cursor:pointer;">&#x21bb;</button>
                                            </div>
                                    </div>
                                    <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                                            <div style="font-weight:600;margin-bottom:0.5em;">Completare automată</div>
                                            <div style="color:#bdbdbd;font-size:0.97em;">Site web</div>
                                            <div style="display:flex;align-items:center;gap:8px;">
                                                    <input type="text" id="itemSite" placeholder="Site web (URI)" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                                            </div>
                                    </div>
                            </div>
                            <div style="background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                                    <div style="font-weight:600;margin-bottom:0.5em;">Opțiuni suplimentare</div>
                                    <textarea id="itemNote" placeholder="Note" style="width:100%;margin-bottom:12px;padding:10px 12px;border-radius:7px;border:none;background:#191b26;color:#e3e6f3;font-size:1rem;outline:none;"></textarea>
                            </div>
                            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:2em;gap:18px;">
                                    <button type="submit" class="btn-primary" style="padding:10px 32px;background:#1976d2;color:#fff;border:none;border-radius:8px;cursor:pointer;font-weight:600;font-size:1em;box-shadow:0 2px 8px #1976d255;">Salvare</button>
                                    <button type="button" class="btn-secondary" id="cancelBtn" style="padding:10px 18px;background:#2d2323;color:#ff5252;border:none;border-radius:8px;cursor:pointer;font-size:1.1em;font-weight:600;">Anulare</button>
                            </div>
                    </form>
            </div>`;
            document.body.appendChild(modalBg);
            // Funcționalitate show/hide parolă
            document.getElementById('pwToggleBtn').onclick = function (e) {
                e.preventDefault();
                const pw = document.getElementById('itemPassword');
                if (pw.type === 'password') { pw.type = 'text'; this.textContent = '🙈'; }
                else { pw.type = 'password'; this.textContent = '👁️'; }
            };
            // Generator demo parolă
            document.getElementById('pwGenBtn').onclick = function (e) {
                e.preventDefault();
                const pw = Math.random().toString(36).slice(-10) + Math.random().toString(36).slice(-5);
                document.getElementById('itemPassword').value = pw;
            };
            // Închidere
            document.getElementById('cancelBtn').onclick = function (e) {
                e.preventDefault();
                modalBg.remove();
            };
            // Salvare articol nou
            // This function assumes jQuery is loaded (window.$ or window.jQuery is available)
            document.getElementById('addItemForm').onsubmit = function (e) {
                e.preventDefault();

                // Assuming 'apiAvailable' is true to attempt the AJAX call
                // Helper to get user info (assuming getCurrentUser is defined elsewhere)
                const user = typeof getCurrentUser === 'function' ? getCurrentUser() : null;

                // 1. Gather form data
                const newItem = {
                    userId: user ? (user.id || user.email) : 'local',
                    denumire: document.getElementById('itemName').value,
                    proprietar: user ? (user.email || 'Eu') : 'Eu',
                    tip: 'Conectare',
                    username: document.getElementById('itemUser').value,
                    parola: document.getElementById('itemPassword').value,
                    saitweb: document.getElementById('itemSite').value,
                    detalii: document.getElementById('itemNote').value,
                };
                console.log(JSON.stringify(newItem));
                // 2. AJAX Call to /password/add
                $.ajax({
                    url: '/passwords',
                    type: 'POST',
                    data: JSON.stringify({ passwordData: newItem }),
                    contentType: 'application/json',
                    success: function (res) {
                        console.log(res);
                        console.log(user);


                        // 3. Success: Refresh list and close modal
                        // Get the user identifier for the next API call

                        if (typeof getPasswords === 'function') {
                            // Assuming getPasswords uses $.ajax() or fetch internally
                            getPasswords()
                                .then(list => {
                                    if (typeof modalBg !== 'undefined' && typeof modalBg.remove === 'function') {
                                        modalBg.remove();
                                    }
                                    if (typeof renderPasswords === 'function') {
                                        renderPasswords(list);
                                    }
                                })
                                .catch(err => {
                                    console.error("Eroare la reîncărcarea listei:", err);
                                    alert('Parola a fost salvată, dar a apărut o eroare la reîncărcarea listei.');
                                });
                        } else {
                            // Handle case where getPasswords is unavailable
                            if (typeof modalBg !== 'undefined' && typeof modalBg.remove === 'function') {
                                modalBg.remove();
                            }
                            alert('Salvare reușită. Nu s-a putut reîncărca lista.');
                        }
                    },
                    error: function (xhr, status, error) {
                        // 4. Error Handling
                        console.error("Eroare AJAX:", status, error);
                        alert('Eroare la salvare. Verifică API-ul.');
                    }
                });


            };
        });
    }


    document.getElementById('logout-btn').addEventListener('click', function () {
        window.location.href = 'login';
    });
    // Navigare între secțiuni
    document.getElementById('nav-all').onclick = function () {
        console.log('clicked');
        document.getElementById('nav-all').classList.add('active');
        document.getElementById('nav-history').classList.remove('active');
        // Afișează secțiunea parolelor și ascunde istoricul cererilor dacă există
        document.getElementById('password-list').style.display = '';
        var reqSection = document.getElementById('request-history-section');
        if (reqSection) reqSection.style.display = 'none';
        document.getElementById('create-btn').style.display = 'inline-block';
        if (typeof renderPasswords === 'function') {
            async function load() {
                var list = await getPasswords();
                renderPasswords(list);
            }

            load();
        } else {
            document.getElementById('password-list').innerHTML = '<div style="color:red;font-weight:bold;">Eroare: funcția renderPasswords nu este definită!</div>';
        }
    };
    document.getElementById('nav-history').onclick = function () {
        document.getElementById('nav-history').classList.add('active');
        document.getElementById('nav-all').classList.remove('active');
        // Ascunde secțiunea parolelor și arată doar istoricul cererilor
        document.getElementById('password-list').style.display = 'none';
        // Creează sau arată secțiunea pentru istoricul cererilor
        let reqSection = document.getElementById('request-history-section');
        if (!reqSection) {
            reqSection = document.createElement('section');
            reqSection.id = 'request-history-section';
            reqSection.style.marginTop = '32px';
            document.querySelector('.main-content').appendChild(reqSection);
        }
        reqSection.style.display = '';
        document.getElementById('create-btn').style.display = 'none';
        // Dacă există cereri, afișează direct lista; altfel, arată doar mesajul informativ
        $.ajax({
            url: '/requests',
            method: 'GET',
            dataType: 'json',
            success: function (history) {
                reqSection.innerHTML = '<h3 style="color:#b8c2e0;margin-bottom:18px;">Istoricul cererilor</h3>';
                if (history.length > 0 && typeof renderRequestHistory === 'function') {
                    history.slice().reverse().forEach((item, idx, arr) => {
                        const div = document.createElement('div');
                        div.className = 'request-history-item';
                        div.style = 'background:#181c2f;border-radius:18px;padding:24px 28px 18px 28px;margin-bottom:24px;box-shadow:0 2px 16px #0003;max-width:600px;color:#b8c2e0;cursor:pointer;position:relative;';
                        const realIdx = history.length - 1 - idx;
                        div.innerHTML = `
                            <div style="font-size:1.25em;font-weight:700;color:#8faaff;margin-bottom:8px;display:flex;align-items:center;justify-content:space-between;">
                                <span>${item.nume || item.mesaj || 'Cerere acces parole'}</span>
                                <span class='delete-request-btn' title='Șterge cererea' style='color:#e57373;cursor:pointer;font-size:1.3em;margin-left:12px;user-select:none;'>&#128465;</span>
                            </div>
                            ${item.scop ? `<div style='margin-bottom:2px;'><b>Scop:</b> <span style='color:#fff;'>${item.scop}</span></div>` : ''}
                            ${item.detalii ? `<div style='margin-bottom:2px;'><b>Detalii:</b> <span style='color:#fff;'>${item.detalii}</span></div>` : ''}
                            ${item.prioritate ? `<div style='margin-bottom:2px;'><b>Prioritate:</b> <span style='color:${getPriorityColor(item.prioritate)};'>${item.prioritate}</span></div>` : ''}
                            <div style='margin-top:6px;font-size:0.98em;color:#a0a8c3;'><b>Data:</b> ${formatISODate(item.data.$date) || '-'}</div>
                        `;
                        div.onclick = function (e) {
                            if (e.target.classList.contains('delete-request-btn')) return;
                            window.open(`admin/request=${item._id}`, '_blank');
                        };
                        div.querySelector('.delete-request-btn').onclick = function (e) {
                            e.stopPropagation();
                            if (confirm('Sigur vrei să ștergi această cerere din istoric?')) {
                                console.log(item);
                                $.ajax({
                                    url: '/requests/' + (item._id ? item._id : realIdx),
                                    method: 'DELETE',
                                    success: function () {
                                        // Reîncarcă lista după ștergere
                                        document.getElementById('nav-history').onclick();
                                        async function load() {
                                            var list = await getPasswords();
                                            renderPasswords(list);
                                        }

                                        load();
                                    },
                                    error: function (jqXHR, textStatus, errorThrown) {
                                        alert('Eroare la ștergere cerere!');
                                    }
                                });
                            }
                        };
                        reqSection.appendChild(div);
                    });
                } else {
                    reqSection.innerHTML += '<div style="color:#888;padding:2em;">Nicio cerere salvată.</div>';
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                reqSection.innerHTML = '<div style="color:red;font-weight:bold;">Eroare la încărcarea istoricului cererilor!</div>';
            }
        });
    };
    // Dialog modern pentru creare articol (stil Bitwarden)
    document.getElementById('create-btn').addEventListener('click', function () {
        if (document.querySelector('.modal')) return;
        const modalBg = document.createElement('div');
        modalBg.className = 'modal';
        modalBg.style = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:rgba(20,22,30,0.85);z-index:1000;display:flex;align-items:center;justify-content:center;';
        modalBg.innerHTML = `
        <div class="modal-content" style="background:#232533;color:#e3e6f3;border-radius:18px;padding:36px 36px 24px 36px;min-width:400px;max-width:95vw;box-shadow:0 8px 32px #0008;position:relative;display:flex;flex-direction:column;gap:24px;">
            <span class="close" onclick="document.querySelector('.modal').remove()" style="position:absolute;top:18px;right:24px;font-size:2rem;cursor:pointer;color:#bbb;">&times;</span>
            <h2 style="margin-top:0;margin-bottom:18px;font-size:1.5em;font-weight:700;letter-spacing:0.01em;">New Conectare</h2>
            <form id="addItemForm" class="styled-form" style="display:flex;flex-direction:column;gap:24px;">
                <div style="background:#23283a;border-radius:12px;padding:18px 24px 12px 24px;display:flex;align-items:center;gap:18px;box-shadow:0 2px 8px #0002;">
                    <div style="flex:1;min-width:0;">
                        <input type="text" id="itemName" placeholder="Nume Item:" required style="font-size:1.25em;font-weight:700;color:#6d8cff;background:none;border:none;outline:none;width:100%;padding:0;">
                    </div>
                </div>
                <div style="display:flex;gap:24px;flex-wrap:wrap;">
                    <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                        <div style="font-weight:600;margin-bottom:0.5em;">Date de logare</div>
                        <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px;">
                            <input type="text" id="itemUser" placeholder="Nume utilizator" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                        </div>
                        <div style="display:flex;align-items:center;gap:8px;">
                            <input type="password" id="itemPassword" placeholder="Parolă" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100px;">
                            <button type="button" class="icon-btn" id="pwToggleBtn" tabindex="-1" style="background:none;border:none;color:#8ab4f8;font-size:1.1em;cursor:pointer;">👁️</button>
                            <button type="button" class="icon-btn" id="pwGenBtn" title="Generează parolă" tabindex="-1" style="background:none;border:none;color:#8ab4f8;font-size:1.1em;cursor:pointer;">&#x21bb;</button>
                        </div>
                    </div>
                    <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                        <div style="font-weight:600;margin-bottom:0.5em;">Completare automată</div>
                        <div style="display:flex;align-items:center;gap:8px;">
                            <input type="text" id="itemSite" placeholder="Site web (URI)" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                        </div>
                    </div>
                </div>
                <div style="background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                    <div style="font-weight:600;margin-bottom:0.5em;">Opțiuni suplimentare</div>
                    <textarea id="itemNote" placeholder="Note" style="width:100%;margin-bottom:12px;padding:10px 12px;border-radius:7px;border:none;background:#191b26;color:#e3e6f3;font-size:1rem;outline:none;"></textarea>
                </div>
                <div style="display:flex;justify-content:space-between;align-items:center;margin-top:2em;gap:18px;">
                    <button type="submit" class="btn-primary" style="padding:10px 32px;background:#1976d2;color:#fff;border:none;border-radius:8px;cursor:pointer;font-weight:600;font-size:1em;box-shadow:0 2px 8px #1976d255;">Salvare</button>
                    <button type="button" class="btn-secondary" id="cancelBtn" style="padding:10px 18px;background:#2d2323;color:#ff5252;border:none;border-radius:8px;cursor:pointer;font-size:1.1em;font-weight:600;">Anulare</button>
                </div>
            </form>
        </div>`;
        document.body.appendChild(modalBg);
        // Funcționalitate show/hide parolă
        document.getElementById('pwToggleBtn').onclick = function (e) {
            e.preventDefault();
            const pw = document.getElementById('itemPassword');
            if (pw.type === 'password') { pw.type = 'text'; this.textContent = '🙈'; }
            else { pw.type = 'password'; this.textContent = '👁️'; }
        };
        // Generator demo parolă
        document.getElementById('pwGenBtn').onclick = function (e) {
            e.preventDefault();
            const pw = Math.random().toString(36).slice(-10) + Math.random().toString(36).slice(-5);
            document.getElementById('itemPassword').value = pw;
        };
        // Închidere
        document.getElementById('cancelBtn').onclick = function (e) {
            e.preventDefault();
            modalBg.remove();
        };
        // Salvare articol nou
        document.getElementById('addItemForm').onsubmit = function (e) {
            e.preventDefault();
            const passwords = JSON.parse(localStorage.getItem('passwords') || '[]');
            const now = new Date();
            const newItem = {
                id: Date.now(),
                denumire: document.getElementById('itemName').value,
                proprietar: 'Eu',
                tip: 'Conectare',
                username: document.getElementById('itemUser').value,
                parola: document.getElementById('itemPassword').value,
                saitweb: document.getElementById('itemSite').value,
                detalii: document.getElementById('itemNote').value,
                istoric: [{ actiune: 'creat', data: now.toLocaleString() }],
                lastEdited: now.toLocaleString(),
                created: now.toLocaleString()
            };
            passwords.push(newItem);
            localStorage.setItem('passwords', JSON.stringify(passwords));
            modalBg.remove();
            if (typeof renderPasswords === 'function') {
                renderPasswords(passwords);
            } else {
                location.reload();
            }
        };
    });
});
//// Închidere cu X
//modalBg.querySelector('#close-modal').onclick = function () {
//    modalBg.remove();
//};
window.showEditDialog = showEditDialog;

// Structura articolului de parolă
// { id, denumire, proprietar, tip, username, parola, detalii }

async function getPasswords(userId) {
    return $.ajax({
        url: userId ? '/passwords?userId=' + userId : '/passwords',
        method: 'GET',
        dataType: 'json',
        success: function (passwords) {
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error('Request failed:', textStatus, errorThrown);
        }
    });
}

function savePasswords(list) {
    localStorage.setItem('passwords', JSON.stringify(list));
}

function renderPasswords(list) {
    var container = document.getElementById('password-list');
    container.innerHTML = '';

    if (list.length == 0) {
        container.innerHTML = '<div style="color:#888;padding:2em;">Nicio parolă salvată.</div>';
        return;
    }
    list.forEach(item => {
        const div = document.createElement('div');
        div.className = 'password-card';
        div.innerHTML = `
            <div class="pw-card-main">
                <div class="pw-card-info">
                    <div class="pw-card-title" title="${item.denumire}">${item.denumire}</div>
                    <div class="pw-card-meta">
                        <span class="pw-card-user"><b style='color:#8ab4f8;'>👤</b> ${item.username || '<i>fără user</i>'}</span>
                        <span class="pw-card-type"><b>Tip:</b> ${item.tip || 'N/A'}</span>
                        <span class="pw-card-owner"><b>Proprietar:</b> ${item.proprietar}</span>
                        ${item.saitweb ? `<span class="pw-card-site"><b style='color:#8ab4f8;'>🌐</b> <a href="${item.saitweb}" target="_blank">${item.saitweb}</a></span>` : ''}
                    </div>
                </div>
                <div class="pw-card-actions">
                    <button class="pw-action-btn" title="Editare">Edit</button>
                    <button class="pw-action-btn delete" title="Ștergere">Șterge</button>
                </div>
            </div>
        `;
        // acțiuni butoane
        const btns = div.querySelectorAll('.pw-action-btn');
        btns[0].onclick = () => showEditDialog(item._id.$oid);
        btns[1].onclick = () => { if (confirm('Sigur vrei să ștergi acest articol?')) deleteItem(item._id.$oid); };
        div.querySelector('.pw-card-title').onclick = () => showPasswordDialog(item._id.$oid);
        container.appendChild(div);
    });
    // Expune funcțiile globale pentru acces din HTML inline/modal
    window.renderPasswords = renderPasswords;
    window.showEditDialog = showEditDialog;
    window.showPasswordDialog = showPasswordDialog;
}

function closeAllMenus() {
    document.querySelectorAll('.context-menu').forEach(m => m.remove());
}

function deleteItem(id) {
    return $.ajax({
        url: '/passwords/' + id,
        method: 'DELETE',
        success: function (passwords) {
            console.log('Passwords:', passwords);
            async function load() {
                var list = await getPasswords();
                renderPasswords(list);
            }

            load();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error('Request failed:', textStatus, errorThrown);
        }
    });

    async function load() {
        var list = await getPasswords();
        renderPasswords(list);
    }

    load();
}

function cloneItem(item) {
    const passwords = getPasswords();
    const newItem = { ...item, id: Date.now(), denumire: item.denumire + ' (copie)' };
    passwords.push(newItem);
    savePasswords(passwords);
    renderPasswords(getPasswords());
}


function showPasswordDialog(id) {
    const passwords = getPasswords();
    const item = passwords.find(p => p.id === id);
    if (!item) return;
    if (document.getElementById('modal-bg')) document.getElementById('modal-bg').remove();
    const modalBg = document.createElement('div');
    modalBg.id = 'modal-bg';
    modalBg.style = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:rgba(20,22,30,0.85);z-index:1000;display:flex;align-items:center;justify-content:center;';
    const modal = document.createElement('div');
    modal.style = 'background:#232533;padding:36px 36px 24px 36px;border-radius:18px;min-width:400px;max-width:95vw;color:#e3e6f3;box-shadow:0 8px 32px #0008;position:relative;display:flex;flex-direction:column;gap:24px;';
    modal.innerHTML = `
        <button id="close-modal" style="position:absolute;top:18px;right:24px;background:none;border:none;font-size:2rem;color:#bbb;cursor:pointer;">&times;</button>
        <h2 style="margin-top:0;margin-bottom:18px;font-size:1.5em;font-weight:700;letter-spacing:0.01em;text-align:center;">Vizualizare articol</h2>
        <div style="display:flex;justify-content:center;align-items:center;margin-bottom:10px;">
            <input type="text" value="${item.denumire}" readonly style="font-size:1.25em;font-weight:700;color:#6d8cff;background:#23283a;border-radius:10px;border:2px solid #23283a;outline:none;width:90%;max-width:340px;padding:12px 18px;text-align:center;box-shadow:0 2px 8px #0002;">
        </div>
        <div style="display:flex;gap:24px;flex-wrap:wrap;">
            <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                <div style="font-weight:600;margin-bottom:0.5em;">Date de logare</div>
                <div style="display:flex;align-items:center;gap:8px;">
                    <input type="text" value="${item.username || ''}" readonly style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                    <button title="Copy" style="background:none;border:none;color:#8ab4f8;cursor:pointer;font-size:1.1em;" onclick="navigator.clipboard.writeText('${item.username || ''}')">📋</button>
                </div>
                <div style="display:flex;align-items:center;gap:8px;margin-top:10px;">
                    <input type="password" id="pwfield" value="${item.parola || ''}" readonly style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:180px;">
                    <button id="showpw" title="Show/Hide" style="background:none;border:none;color:#8ab4f8;cursor:pointer;font-size:1.1em;">👁️</button>
                    <button title="Copy" style="background:none;border:none;color:#8ab4f8;cursor:pointer;font-size:1.1em;" onclick="navigator.clipboard.writeText('${item.parola || ''}')">📋</button>
                </div>
            </div>
            <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                <div style="font-weight:600;margin-bottom:0.5em;">Completare automată</div>
                <div style="display:flex;align-items:center;gap:8px;">
                    <input type="text" value="${item.saitweb || ''}" readonly style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                    <button title="Copy" style="background:none;border:none;color:#8ab4f8;cursor:pointer;font-size:1.1em;" onclick="navigator.clipboard.writeText('${item.saitweb || ''}')">📋</button>
                </div>
            </div>
        </div>
        <div style="background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
            <div style="font-weight:600;margin-bottom:0.5em;">Istoricul articolului</div>
            <div style="color:#bdbdbd;font-size:0.97em;">Last edited: ${item.lastEdited || '-'}<br>Creată: ${item.created || '-'}<br>Parolă actualizată: ${item.lastEdited || '-'}</div>
            <a href="#" style="color:#8ab4f8;font-size:0.97em;">Istoric parole</a>
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:2em;gap:18px;">
            <button id="edit-btn" style="padding:10px 32px;background:#1976d2;color:#fff;border:none;border-radius:8px;cursor:pointer;font-weight:600;font-size:1em;box-shadow:0 2px 8px #1976d255;">Editare</button>
            <button id="delete-btn" style="padding:10px 18px;background:#2d2323;color:#ff5252;border:none;border-radius:8px;cursor:pointer;font-size:1.2em;font-weight:600;">🗑️ Șterge</button>
        </div>
    `;
    modalBg.appendChild(modal);
    document.body.appendChild(modalBg);
    document.getElementById('close-modal').onclick = () => modalBg.remove();
    document.getElementById('modal-bg').onclick = e => { if (e.target === modalBg) modalBg.remove(); };
    // Show/hide password
    modal.querySelector('#showpw').onclick = function () {
        const pwf = modal.querySelector('#pwfield');
        if (pwf.type === 'password') { pwf.type = 'text'; this.textContent = '🙈'; }
        else { pwf.type = 'password'; this.textContent = '👁️'; }
    };
    // Editare articol
    modal.querySelector('#edit-btn').onclick = function () {
        modalBg.remove();
        setTimeout(() => window.showEditDialog(item.id), 0);
    };
    // Ștergere articol
    modal.querySelector('#delete-btn').onclick = function () {
        if (confirm('Sigur vrei să ștergi acest articol?')) {
            const newList = passwords.filter(p => p.id !== id);
            savePasswords(newList);
            modalBg.remove();
            async function load() {
                var list = await getPasswords(id);
                return list;
            }
            renderPasswords(load());
        }
    };
}

async function showEditDialog(id) {
    console.log('showEditDialog', id);
    var passwords = [];

    async function load() {
        var list = await getPasswords(id);
        passwords.length = 0;
        passwords.push(...list);

        // 3. Find the item using the correct nested property access
        var item = passwords.find(p => p._id.$oid === id);

        return item;
    }

    async function run() {
        return await load();
    }
    const item = await run();
    console.log(passwords);
    //var item = passwords.find(password => password.userId === 'admin');
    console.log(item);
    if (!item) return;
    if (document.getElementById('modal-bg')) document.getElementById('modal-bg').remove();
    const modalBg = document.createElement('div');
    modalBg.id = 'modal-bg';
    modalBg.className = 'modal';
    modalBg.style = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:rgba(20,22,30,0.85);z-index:1000;display:flex;align-items:center;justify-content:center;';
    const modal = document.createElement('div');
    modal.style = 'background:#232533;color:#e3e6f3;border-radius:18px;padding:36px 36px 24px 36px;min-width:400px;max-width:95vw;box-shadow:0 8px 32px #0008;position:relative;display:flex;flex-direction:column;gap:24px;';
    modal.innerHTML = `
        <button id="close-modal" style="position:absolute;top:18px;right:24px;background:none;border:none;font-size:2rem;color:#bbb;cursor:pointer;">&times;</button>
        <h2 style="margin-top:0;margin-bottom:18px;font-size:1.5em;font-weight:700;letter-spacing:0.01em;text-align:center;">Editare articol</h2>
        <div style="display:flex;justify-content:center;align-items:center;margin-bottom:10px;">
            <input type="text" id="denumire-edit" name="denumire" value="${item.denumire || ''}" style="font-size:1.25em;font-weight:700;color:#6d8cff;background:#23283a;border-radius:10px;border:2px solid #23283a;outline:none;width:90%;max-width:340px;padding:12px 18px;text-align:center;box-shadow:0 2px 8px #0002;">
        </div>
        <form id="edit-form" class="styled-form" style="display:flex;flex-direction:column;gap:24px;">
            <div style="display:flex;gap:24px;flex-wrap:wrap;">
                <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                    <div style="font-weight:600;margin-bottom:0.5em;">Date de logare</div>
                    <div style="display:flex;align-items:center;gap:8px;">
                        <input type="text" id="username-edit" name="username" value="${item.username || ''}" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                    </div>
                    <div style="display:flex;align-items:center;gap:8px;margin-top:10px;">
                        <input type="password" id="parola-edit" name="parola" value="${item.parola || ''}" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:180px;">
                        <button type="button" class="icon-btn" id="pwToggleBtnEdit" tabindex="-1"><span id="pwIconEdit">👁️</span></button>
                    </div>
                </div>
                <div style="flex:1;min-width:220px;background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                    <div style="font-weight:600;margin-bottom:0.5em;">Completare automată</div>
                    <div style="display:flex;align-items:center;gap:8px;">
                        <input type="text" id="saitweb-edit" name="saitweb" value="${item.saitweb || ''}" style="background:#191b26;color:#e3e6f3;font-size:1em;border:none;border-radius:7px;padding:8px 10px;width:100%;">
                    </div>
                </div>
            </div>
            <div style="background:#23283a;border-radius:10px;padding:18px 18px 8px 18px;box-shadow:0 2px 8px #0002;">
                <div style="font-weight:600;margin-bottom:0.5em;">Opțiuni suplimentare</div>
                <textarea id="note-edit" placeholder="Note" style="width:100%;margin-bottom:12px;padding:10px 12px;border-radius:7px;border:none;background:#191b26;color:#e3e6f3;font-size:1rem;outline:none;">${item.detalii || ''}</textarea>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:2em;gap:18px;">
                <button type="submit" class="btn-primary" style="padding:10px 32px;background:#1976d2;color:#fff;border:none;border-radius:8px;cursor:pointer;font-weight:600;font-size:1em;box-shadow:0 2px 8px #1976d255;">Salvează</button>
                <button type="button" class="btn-secondary" id="cancelBtnEdit" style="padding:10px 18px;background:#2d2323;color:#ff5252;border:none;border-radius:8px;cursor:pointer;font-size:1.1em;font-weight:600;">Anulează</button>
            </div>
        </form>
    `;
    // Adaugă modalul în DOM înainte de a atașa evenimente pe elemente din el
    modalBg.appendChild(modal);
    document.body.appendChild(modalBg);
    if (!document.getElementById('modal-style')) {
        const style = document.createElement('style');
        style.id = 'modal-style';
        style.textContent = `
        .modal { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(20, 22, 30, 0.85); display: flex; align-items: center; justify-content: center; z-index: 1000; }
        .modal-content { background: #232533; color: #e3e6f3; border-radius: 16px; padding: 32px 32px 16px 32px; width: 480px; box-shadow: 0 4px 32px #0008; position: relative; }
        .close { position: absolute; top: 18px; right: 24px; font-size: 2rem; cursor: pointer; color: #bbb; }
        .styled-form h3 { margin-top: 24px; margin-bottom: 8px; font-size: 1.1rem; color: #b8c2e0; }
        .section { background: #23283a; border-radius: 10px; padding: 18px 18px 8px 18px; margin-bottom: 18px; box-shadow: 0 2px 8px #0002; }
        .styled-form input, .styled-form select, .styled-form textarea { width: 100%; margin-bottom: 12px; padding: 10px 12px; border-radius: 7px; border: none; background: #191b26; color: #e3e6f3; font-size: 1rem; outline: none; transition: box-shadow 0.2s; }
        .styled-form input:focus, .styled-form textarea:focus, .styled-form select:focus { box-shadow: 0 0 0 2px #7da7f7; }
        .password-row { display: flex; align-items: center; gap: 6px; }
        .icon-btn { background: none; border: none; color: #a5b4fc; font-size: 1.2rem; cursor: pointer; padding: 4px 8px; transition: color 0.2s; }
        .icon-btn:hover { color: #fff; }
        .checkbox-row { display: flex; align-items: center; gap: 7px; margin-top: 8px; }
        .modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
        .btn-primary { background: #6d8cff; color: #fff; border: none; border-radius: 8px; padding: 10px 28px; font-size: 1rem; cursor: pointer; font-weight: 600; box-shadow: 0 2px 8px #0001; transition: background 0.2s; }
        .btn-primary:hover { background: #4d6edb; }
        .btn-secondary { background: none; color: #b8c2e0; border: 1px solid #4d6edb; border-radius: 8px; padding: 10px 22px; font-size: 1rem; cursor: pointer; }
        .btn-secondary:hover { background: #2c3142; }
        `;
        document.head.appendChild(style);
    }
    // Show/hide parolă
    document.getElementById('pwToggleBtnEdit').onclick = function (e) {
        e.preventDefault();
        const pw = document.getElementById('parola-edit');
        const icon = document.getElementById('pwIconEdit');
        if (pw.type === 'password') { pw.type = 'text'; icon.textContent = '🙈'; }
        else { pw.type = 'password'; icon.textContent = '👁️'; }
    };
    // Închidere
    const closeModal = function (e) {
        if (e) e.preventDefault();
        modalBg.remove();
    };
    document.getElementById('cancelBtnEdit').onclick = closeModal;
    document.getElementById('close-modal').onclick = closeModal;
    // Salvare articol editat
    document.getElementById('edit-form').onsubmit = function (e) {
        e.preventDefault();

        const now = new Date();
        // Assuming API is available if jQuery ($) is loaded
        const apiAvailable = typeof $ === 'function';

        // Find the item being edited to access its MongoDB ID structure
        // Assumes 'id' is the target $oid string (e.g., "6917cb24a75d735b2d7c7711")
        const oldItem = passwords.find(p => p._id?.$oid === id);

        if (!oldItem) {
            alert('Eroare: Elementul de editat nu a fost găsit.');
            return;
        }

        // 1. Gather all updated form data
        const updatedFields = {
            // Send the clean ID string to the backend for identification
            id: oldItem._id.$oid,
            userId: oldItem.userId, // Include userId for verification if needed
            denumire: document.getElementById('denumire-edit').value,
            dosar: document.getElementById('dosar-edit')?.value || '',
            username: document.getElementById('username-edit').value,
            parola: document.getElementById('parola-edit').value,
            saitweb: document.getElementById('saitweb-edit').value,
            detalii: document.getElementById('note-edit')?.value || '',
            // Set the lastEdited date on the client side (server should validate/override this)
            lastEdited: now.toISOString()
        };
        console.log(updatedFields);
        // --- API Implementation using $.ajax() ---
        $.ajax({
            // Use PUT method standard for updating a resource
            url: '/passwords/' + updatedFields.id,
            method: 'PUT',
            data: JSON.stringify({ updates: updatedFields }),
            contentType: 'application/json',
            success: function (res) {

                console.log(res);
                // Assume getPasswords and renderPasswords are available and fetch/display data
                if (typeof getPasswords === 'function' && typeof renderPasswords === 'function') {
                    console.log(updatedFields);

                    // Refresh the entire list from the server
                    getPasswords()
                        .then(list => {
                            if (typeof modalBg !== 'undefined' && typeof modalBg.remove === 'function') {
                                modalBg.remove();
                            }
                            renderPasswords(list);
                        })
                        .catch(err => {
                            console.error("Eroare la reîncărcarea listei:", err);
                            alert('Actualizare reușită, dar a apărut o eroare la reîncărcarea listei.');
                        });
                } else {

                    // Close modal even if refresh functions are missing
                    if (typeof modalBg !== 'undefined' && typeof modalBg.remove === 'function') {
                        modalBg.remove();
                    }
                    alert('Actualizare reușită. Nu s-a putut reîncărca lista.');
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                // Network or HTTP status error (e.g., 404, 500)
                console.error("Eroare AJAX la actualizare:", textStatus, errorThrown);
                alert('Eroare la salvare. Verifică API-ul.');
            }
        });


    };
}

//TODO: Check what is this
//// Căutare
//document.getElementById('search').addEventListener('input', function (e) {
//    const val = e.target.value.toLowerCase();
//    const passwords = getPasswords();
//    renderPasswords(passwords.filter(p => p.denumire.toLowerCase().includes(val)));
//});

// Formular adăugare articol nou
function addPasswordForm() {
    const container = document.createElement('div');
    container.className = 'add-password-form';
    container.innerHTML = `
        <h3>Adaugă articol nou</h3>
        <form id="add-password-form">
            <input type="text" id="denumire" placeholder="Denumire" required><br>
            <input type="text" id="proprietar" placeholder="Proprietar" value="Eu" required><br>
            <input type="text" id="tip" placeholder="Tip (ex: Conectare, Card)"><br>
            <input type="text" id="username" placeholder="Username"><br>
            <input type="password" id="parola" placeholder="Parolă"><br>
            <input type="text" id="detalii" placeholder="Detalii"><br>
            <button type="submit">Adaugă</button>
        </form>
        <hr>
    `;
    document.querySelector('.main-content').insertBefore(container, document.querySelector('#password-list'));
    document.getElementById('add-password-form').addEventListener('submit', function (e) {
        e.preventDefault();
        const passwords = getPasswords();
        const newItem = {
            id: Date.now(),
            denumire: document.getElementById('denumire').value,
            proprietar: document.getElementById('proprietar').value,
            tip: document.getElementById('tip').value,
            username: document.getElementById('username').value,
            parola: document.getElementById('parola').value,
            detalii: document.getElementById('detalii').value
        };
        passwords.push(newItem);
        savePasswords(passwords);
        renderPasswords(passwords);
        this.reset();
    });
}

// Eliminat popularea demo automată pentru a nu suprascrie datele utilizatorului
// Afișează istoricul cererilor
function getPriorityColor(prioritate) {
    switch (prioritate?.toLowerCase()) {
        case 'scazut':
        case 'scăzut':
            return '#81c784'; // Verde
        case 'mediu':
            return '#ffd54f'; // Galben
        case 'ridicat':
            return '#e57373'; // Roșu
        default:
            return '#ffd54f'; // Default galben
    }
}

function renderRequestHistory() {
    const container = document.getElementById('password-list');
    const history = JSON.parse(localStorage.getItem('requestHistory') || '[]');
    container.innerHTML = '<h3 style="color:#b8c2e0;margin-bottom:18px;">Istoricul cererilor</h3>';
    if (history.length === 0) {
        container.innerHTML += '<div style="color:#888;padding:2em;">Nicio cerere salvată.</div>';
        return;
    }
    history.slice().reverse().forEach((item, idx, arr) => {
        const div = document.createElement('div');
        div.className = 'request-history-item';
        div.style = 'background:#181c2f;border-radius:18px;padding:24px 28px 18px 28px;margin-bottom:24px;box-shadow:0 2px 16px #0003;max-width:600px;color:#b8c2e0;cursor:pointer;position:relative;';
        // indexul real în array-ul original (deoarece slice().reverse())
        const realIdx = history.length - 1 - idx;
        div.innerHTML = `
            <div style=\"font-size:1.25em;font-weight:700;color:#8faaff;margin-bottom:8px;display:flex;align-items:center;justify-content:space-between;\">
                <span>${item.nume || item.mesaj || 'Cerere acces parole'}</span>
                <span class='delete-request-btn' title='Șterge cererea' style='color:#e57373;cursor:pointer;font-size:1.3em;margin-left:12px;user-select:none;'>&#128465;</span>
            </div>
            ${item.scop ? `<div style='margin-bottom:2px;'><b>Scop:</b> <span style='color:#fff;'>${item.scop}</span></div>` : ''}
            ${item.detalii ? `<div style='margin-bottom:2px;'><b>Detalii:</b> <span style='color:#fff;'>${item.detalii}</span></div>` : ''}
            ${item.prioritate ? `<div style='margin-bottom:2px;'><b>Prioritate:</b> <span style='color:${getPriorityColor(item.prioritate)};'>${item.prioritate}</span></div>` : ''}
            <div style='margin-top:6px;font-size:0.98em;color:#a0a8c3;'><b>Data:</b> ${formatISODate(item.data) || '-'}</div>
        `;
        // Click pe card = deschide admin_request.html
        div.onclick = function (e) {
            if (e.target.classList.contains('delete-request-btn')) return;
            window.open(`admin?request=${realIdx}`, '_blank');
        };
        // Click pe iconița de ștergere
        div.querySelector('.delete-request-btn').onclick = function (e) {
            e.stopPropagation();
            if (confirm('Sigur vrei să ștergi această cerere din istoric?')) {
                const hist = JSON.parse(localStorage.getItem('requestHistory') || '[]');
                hist.splice(realIdx, 1);
                localStorage.setItem('requestHistory', JSON.stringify(hist));
                renderRequestHistory();
            }
        };
        container.appendChild(div);
    });
}

function formatISODate(isoDate, formatType = 'local') {
    let dateString;

    // Handle MongoDB BSON date structure (e.g., { $date: "2023-02-01T00:00:00Z" })
    if (typeof isoDate === 'object' && isoDate !== null && isoDate.hasOwnProperty('$date')) {
        dateString = isoDate.$date;
    } else if (typeof isoDate === 'string') {
        dateString = isoDate;
    } else {
        return 'N/A'; // Or handle error appropriately
    }

    // Attempt to create a Date object
    const date = new Date(dateString);

    // Check if the date is valid
    if (isNaN(date.getTime())) {
        return 'Invalid Date';
    }

    if (formatType === 'utc') {
        // Example: 2023-02-01 00:00 UTC
        return date.getUTCFullYear() + '-' +
            String(date.getUTCMonth() + 1).padStart(2, '0') + '-' +
            String(date.getUTCDate()).padStart(2, '0') +
            ' UTC';
    } else {
        // Formats the date based on the user's locale (e.g., 01/02/2023, 02:00:00)
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
}

window.renderRequestHistory = renderRequestHistory;
// Inițializare UI

async function load() {
    var list = await getPasswords();
    renderPasswords(list);
}

load();
// Final de fișier - închidere blocuri lipsă
