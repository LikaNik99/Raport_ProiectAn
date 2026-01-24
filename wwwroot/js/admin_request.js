// admin_request.js
// Extrage indexul cererii din query string
function getQueryParam(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}

async function populateRequestDetails() {
    var ws = new WebSocket(`ws://${window.location.host}/Main/SyncDataWebSocket`);
    ws.onopen = function () {

    };
    ws.onerror = function (error) {
        console.error('❌ WebSocket admin error:', error);
    };
    ws.onmessage = function (data) {
    }
    // Obține cererea din API sau fallback local
    const reqIdx = parseInt(getQueryParam('request'), 10);
    let requests = [];

    // Admin vede toate cererile
    requests = await new Promise((resolve, reject) => {
        $.ajax({
            url: '/requests',
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            },
            success: function (data) {
                resolve(data);
            },
            error: function () {
                resolve([]);
            }
        });
    });

    const request = (Number.isInteger(reqIdx) && requests[reqIdx]) ? requests[reqIdx] : (requests[0] || {});
    console.log(request);
    // Data și ora cererii (format clar, în meta)
    const dateRow = document.querySelector('.request-time');
    if (dateRow) dateRow.textContent = request.data || '-';
    const passwordList = [];
    // Populează lista de parole pentru selectare folosind AJAX
    const passwordSelect = document.getElementById('passwordSelect');
    const passwordDetails = document.getElementById('passwordDetails');
    if (passwordSelect) {
        passwordSelect.innerHTML = '<option value="">-- Selectează o parolă --</option>';
        $.ajax({
            url: '/passwords',
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            },
            success: function (passwords) {
                var jsonPasswords = JSON.parse(passwords)
                console.log(jsonPasswords);
                passwordList.push(...jsonPasswords);

                jsonPasswords.forEach(p => {
                    passwordSelect.innerHTML += `<option value="${p._id.$oid}">${p.denumire} (${p.username})</option>`;
                });
                passwordSelect.onchange = function () {
                    const selected = jsonPasswords.find(p => p._id.$oid == this.value);
                    if (selected) {
                        passwordDetails.innerHTML = `<b>Platformă:</b> ${selected.denumire}<br><b>Utilizator:</b> ${selected.username}<br><b>Parolă:</b> <span style='color:#ffd54f;'>${selected.parola}</span>`;
                    } else {
                        passwordDetails.innerHTML = '';
                    }
                };
                // Dacă cererea are deja o parolă selectată, preselectează
                if (request.selectedPasswordId) {
                    passwordSelect.value = request.selectedPasswordId;
                    const selected = jsonPasswords.find(p => p.id == request.selectedPasswordId);
                    if (selected) {
                        passwordDetails.innerHTML = `<b>Platformă:</b> ${selected.denumire}<br><b>Utilizator:</b> ${selected.username}<br><b>Parolă:</b> <span style='color:#ffd54f;'>${selected.parola}</span>`;
                    }
                }
            },
            error: function () {
                passwordSelect.innerHTML = '<option value="">-- Eroare la încărcarea parolelor --</option>';
            }
        });
    }

    // Populează UI-ul cu datele cererii
    // Titlu cerere (nume sau mesaj sau fallback)
    document.getElementById('requestTitle').textContent = request.nume || 'Cerere acces parole';
    // Status
    const statusValue = request.status || 'Requested';
    // Status
    const statusEl = document.querySelector('.request-status');
    if (statusEl) statusEl.textContent = statusValue;
    // Status din meta
    const statusMeta = Array.from(document.querySelectorAll('.request-label')).find(el => el.textContent.includes('Status:'));
    if (statusMeta && statusMeta.nextElementSibling) statusMeta.nextElementSibling.textContent = statusValue;

    // Solicitant
    const solicitantMeta = Array.from(document.querySelectorAll('.request-label')).find(el => el.textContent.includes('Solicitant:'));
    if (solicitantMeta && solicitantMeta.nextElementSibling) solicitantMeta.nextElementSibling.textContent = request.nume || '-';

    // Scop
    const scopMeta = Array.from(document.querySelectorAll('.request-label')).find(el => el.textContent.includes('Scop:'));
    if (scopMeta && scopMeta.nextElementSibling) scopMeta.nextElementSibling.textContent = request.scop || '-';

    // Detalii
    const detaliiMeta = Array.from(document.querySelectorAll('.request-label')).find(el => el.textContent.includes('Detalii:'));
    if (detaliiMeta && detaliiMeta.nextElementSibling) detaliiMeta.nextElementSibling.textContent = request.detalii || '-';

    // Prioritate
    const prioritateMeta = Array.from(document.querySelectorAll('.request-label')).find(el => el.textContent.includes('Prioritate:'));
    if (prioritateMeta && prioritateMeta.nextElementSibling) prioritateMeta.nextElementSibling.textContent = request.prioritate || '-';

    // Data
    const dataMeta = document.querySelector('.request-time');
    if (dataMeta) dataMeta.textContent = request.data.$date || '-';

    // Butoane aprobare/respingere
    const approveBtn = document.querySelector('.approve-btn');
    const rejectBtn = document.querySelector('.reject-btn');
    approveBtn.onclick = async function (e) {
        e.preventDefault();
        // Salvează parola selectată în cerere
        if (passwordSelect && passwordSelect.value) {
            request.selectedPasswordId = passwordSelect.value;
        } else {
            //alert('Selectează o parolă pentru a aproba cererea!');
            return;
        }
        request.status = 'Aprobat';
        // Persistă în API dacă posibil
        try {
            // Inlocuire cu AJAX
            await new Promise((resolve, reject) => {
                $.ajax({
                    url: `/requests/${request._id}`,
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    data: JSON.stringify({ updates: request }),
                    success: function (data) {
                        resolve(data);
                    },
                    error: function (xhr, status, error) {
                        reject(error);
                    }
                });
            });

            //populateRequestDetails();
        } catch (err) {
            console.error('Eroare actualizare cerere:', err);
            //alert('Eroare actualizare cerere. Verifică API-ul.');
            return;
        }
        // --- WEBSOCKET: Trimite datele către client dacă există clientId ---
        if (request.clientId) {
            console.log('📤 Trimit aprobare către clientId:', request.clientId);
            try {
                const selected = passwordList.find(p => p._id.$oid == request.selectedPasswordId);
                //if (selected) {
                // Așteaptă puțin pentru ca serverul să proceseze identificarea
                setTimeout(() => {
                    const message = {
                        to: request.clientId,
                        type: 'requestUpdate',
                        status: 'Aprobat',
                        requestId: request._id || reqIdx,
                        platforma: selected.denumire,
                        user: selected.username,
                        parola: selected.parola,
                        scop: request.scop,
                        data: new Date().toLocaleString()
                    };
                    console.log('📤 Trimit mesaj:', message);
                    ws.send(JSON.stringify(message));
                }, 100);

                //} else {
                //    console.error('❌ Parola selectată nu a fost găsită!');
                //}
            } catch (e) { console.error('WebSocket admin error:', e); }
        } else {
            console.warn('⚠️ Cererea nu are clientId - nu pot trimite notificare!');
        }
        window.close();
        alert('Cererea a fost aprobată! Parola va fi trimisă clientului în timp real.');
    };

    rejectBtn.onclick = async function (e) {
        e.preventDefault();
        request.status = 'Respins';
        try {
            // Inlocuire cu AJAX
            await fetch(`/requests/${request._id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ updates: request })
            });

            populateRequestDetails();
        } catch (err) {
            console.error('Eroare respingere cerere:', err);
            alert('Eroare respingere cerere. Verifică API-ul.');
            return;
        }
        // --- WEBSOCKET: Notifică clientul că cererea a fost respinsă ---
        if (request.clientId) {
            console.log('📤 Trimit respingere către clientId:', request.clientId);
            try {
                setTimeout(() => {
                    const message = {
                        to: request.clientId,
                        type: 'requestUpdate',
                        status: 'Respins',
                        requestId: request.id || reqIdx,
                        data: new Date().toLocaleString()
                    };
                    console.log('📤 Trimit mesaj:', message);
                    ws.send(JSON.stringify(message));
                }, 100);

            } catch (e) { console.error('WebSocket admin error:', e); }
        } else {
            console.warn('⚠️ Cererea nu are clientId - nu pot trimite notificare!');
        }
        alert('Cererea a fost respinsă! Clientul va fi notificat în timp real.');
        window.close();
    };
}

window.addEventListener('DOMContentLoaded', populateRequestDetails);
