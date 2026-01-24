// La încărcare, verifică dacă există cereri cu status Aprobat/Respins și afișează modalul corespunzător
window.addEventListener('DOMContentLoaded', async function() {
    // Actualizează culoarea dropdown-ului de prioritate la schimbare
    const prioritySelect = document.getElementById('priority');
    if (prioritySelect) {
        prioritySelect.addEventListener('change', function() {
            this.setAttribute('data-priority', this.value);
        });
    }
    // --- WEBSOCKET CLIENT ---
    // Generează sau recuperează un id unic pentru client (ex: localStorage)
    let clientId = localStorage.getItem('clientId');
    if (!clientId) {
        clientId = 'client_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('clientId', clientId);
    }
    let ws;
    try {
        ws = new WebSocket(`ws://${window.location.host}/Main/SyncDataWebSocket`);
        ws.onopen = function() {
            console.log('✓ WebSocket conectat cu clientId:', clientId);
            console.log('Aștept notificări de aprobare/respingere...');
        };
        ws.onmessage = function(event) {
            // Primește datele de la server (aprobare sau respingere)
            console.log('📩 Mesaj WebSocket primit:', event.data);
            try {
                const data = JSON.parse(event.data);
                console.log('📦 Date parsate:', data);
                console.log('Type:', data.type, 'Status:', data.status);
                
                // Verifică dacă este o actualizare de cerere
                if (data.type === 'requestUpdate') {
                    console.log('✓ Este o actualizare de cerere!');
                    if (data.status === 'Aprobat') {
                        console.log('✓ Cerere APROBATĂ - afișez modal verde');
                        // Afișează modal cu datele de logare
                        let modal = document.createElement('div');
                        modal.className = 'modal';
                        modal.style = `
                            position: absolute;
                            top: 0; left: 0; width: 100vw; height: 100vh;
                            background: rgba(20,22,30,0.90);
                            display: flex; align-items: center; justify-content: center;
                            z-index: 1000;
                            animation: fadeIn 0.3s ease-in;
                        `;
                        modal.innerHTML = `
                        <div class="modal-content" style="background:#23272f; color:#e3e6f3; border-radius: 16px; padding: 36px 36px 24px 36px; width: 420px; box-shadow: 0 4px 32px #0008; position: relative; text-align:center; animation: slideDown 0.3s ease-out;">
                            <span class="close" style="position:absolute;top:18px;right:24px;font-size:2em;cursor:pointer;color:#bbb;" onclick="this.closest('.modal').remove()">&times;</span>
                            <h2 style="color:#4caf50;margin-bottom:18px;">✓ Cerere aprobată</h2>
                            <div style="margin:18px 0 10px 0;color:#bdbdbd;">Datele de logare:</div>
                            <div style="background:#1a1d24;padding:18px 18px 10px 18px;border-radius:12px;box-shadow:0 2px 12px #0003;display:inline-block;text-align:left;min-width:260px;">
                                <div style='margin-bottom:6px;'><b>Platformă:</b> <span style='color:#8ab4f8;'>${data.platforma || '-'}</span></div>
                                <div style='margin-bottom:6px;'><b>Utilizator:</b> <span style='color:#8ab4f8;'>${data.user || '-'}</span></div>
                                <div style='margin-bottom:6px;'><b>Parolă:</b> <span style='color:#ffd54f;'>${data.parola || '-'}</span></div>
                                <div style='margin-bottom:6px;'><b>Scop:</b> ${data.scop || '-'}</div>
                                <div><b>Data aprobare:</b> ${data.data || '-'}</div>
                            </div>
                        </div>`;
                        document.body.appendChild(modal);
                    } else if (data.status === 'Respins') {
                        console.log('✗ Cerere RESPINSĂ - afișez modal roșu');
                        // Afișează modal pentru respingere
                        const modal = document.createElement('div');
                        modal.className = 'modal';
                        modal.style = `
                            position: fixed;
                            top: 0; left: 0; width: 100vw; height: 100vh;
                            background: rgba(20,22,30,0.90);
                            display: flex; align-items: center; justify-content: center;
                            z-index: 1000;
                            animation: fadeIn 0.3s ease-in;
                        `;
                        modal.innerHTML = `
                        <div class="modal-content" style="background:#23272f; color:#e3e6f3; border-radius: 16px; padding: 36px 36px 24px 36px; width: 420px; box-shadow: 0 4px 32px #0008; position: relative; text-align:center; animation: slideDown 0.3s ease-out;">
                            <span class="close" style="position:absolute;top:18px;right:24px;font-size:2em;cursor:pointer;color:#bbb;" onclick="this.closest('.modal').remove()">&times;</span>
                            <h2 style="color:#e57373;margin-bottom:18px;">✗ Cerere respinsă</h2>
                            <div style="margin:18px 0 10px 0;color:#bdbdbd;">Cererea ta a fost respinsă de administrator.</div>
                            <div style="margin:10px 0;color:#999;">Nu poți primi parola solicitată.</div>
                            <div style="margin-top:18px;color:#777;font-size:0.9em;">Data respingere: ${data.data || new Date().toLocaleString()}</div>
                        </div>`;
                        document.body.appendChild(modal);
                    }
                } else {
                    console.warn('⚠️ Mesaj fără type=requestUpdate:', data);
                }
            } catch (e) { 
                console.error('❌ Eroare parsare WS:', e, 'Data primită:', event.data); 
            }
        };
        ws.onerror = function(error) {
            console.error('WebSocket error:', error);
        };
        ws.onclose = function() {
            console.log('WebSocket închis. Reconectare în 3 secunde...');
            //setTimeout(() => {
            //    location.reload();
            //}, 3000);
        };
    } catch (e) { console.error('WebSocket connection error:', e); }
    // Încărcare cereri din API dacă disponibil, altfel fallback localStorage
    let history = [];
    const apiAvailable = typeof getRequests === 'function' && typeof getCurrentUser === 'function';
    if (apiAvailable) {
        try {
            const user = getCurrentUser();
            if (user) {
                history = await getRequests(user.id || user.email);
            }
        } catch(apiErr) {
            console.warn('Fallback localStorage pentru cereri:', apiErr);
            history = JSON.parse(localStorage.getItem('requestHistory') || '[]');
        }
    } else {
        history = JSON.parse(localStorage.getItem('requestHistory') || '[]');
    }
    if (history.length === 0) return;
    const lastRequest = history[history.length - 1];
    if (lastRequest.status === 'Aprobat') {
        // Caută parola selectată de admin
        let parola = null, user = null, platforma = null;
        if (lastRequest.selectedPasswordId) {
            const passwords = JSON.parse(localStorage.getItem('passwords') || '[]');
            const selected = passwords.find(p => p.id == lastRequest.selectedPasswordId);
            if (selected) {
                parola = selected.parola;
                user = selected.username;
                platforma = selected.denumire;
            }
        }
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
        <div class="modal-content" style="max-width:420px;">
            <span class="close" style="float:right;font-size:2em;cursor:pointer;" onclick="this.closest('.modal').remove()">&times;</span>
            <h2 style="color:#4caf50;text-align:center;">Cerere aprobată</h2>
            <div style="margin:18px 0 10px 0;color:#bdbdbd;">Datele de logare:</div>
            <div style="background:#23272f;padding:18px 18px 10px 18px;border-radius:12px;box-shadow:0 2px 12px #0003;">
                <div><b>Platformă:</b> <span style="color:#8ab4f8;">${platforma || '-'}</span></div>
                <div><b>Utilizator:</b> <span style="color:#8ab4f8;">${user || '-'}</span></div>
                <div><b>Parolă:</b> <span style="color:#ffd54f;">${parola || '-'}</span></div>
                <div><b>Scop:</b> ${lastRequest.scop || '-'}</div>
                <div><b>Data aprobare:</b> ${lastRequest.data || '-'}</div>
            </div>
        </div>`;
        document.body.appendChild(modal);
    } else if (lastRequest.status === 'Respins') {
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
        <div class="modal-content" style="max-width:420px;">
            <span class="close" style="float:right;font-size:2em;cursor:pointer;" onclick="this.closest('.modal').remove()">&times;</span>
            <h2 style="color:#e57373;text-align:center;">Cerere respinsă</h2>
            <div style="margin:18px 0 10px 0;color:#bdbdbd;">Cererea ta a fost respinsă de administrator.<br>Nu poți primi parola.</div>
        </div>`;
        document.body.appendChild(modal);
    }
});
// Replace the form submit handler with an AJAX PUT request to /requests
document.querySelector('.request-form').addEventListener('submit', async function (e) {
    e.preventDefault();
    // Include clientId pentru identificare WebSocket
    let clientId = localStorage.getItem('clientId');
    if (!clientId) {
        clientId = 'client_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('clientId', clientId);
    }
    const data = {
        nume: document.getElementById('requestName').value,
        scop: document.getElementById('purpose').value,
        detalii: document.getElementById('details').value,
        prioritate: document.getElementById('priority').value,
        data: new Date().toLocaleString(),
        clientId: clientId,
        status: 'Requested'
    };


    let success = false;
    try {
        console.log("start request");
        $.ajax({
            url: '/requests',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                dictionary: data
            }),
            success: function () {
                console.log("success request");

                success = true;
                alert(`✓ Cererea a fost actualizată!\nNume: ${data.nume}\nPrioritate: ${data.prioritate}`);
                // Resetare formular
                e.target.reset();
            },
            error: function (xhr, status, error) {
                console.log("error request");

                alert('Eroare la trimiterea cererii: ' + error);
            }
        });
    } catch (err) {
        alert('Eroare la trimiterea cererii: ' + err.message);
    }

    if (success) {
        // Resetare formular
        e.target.reset();
    }
});

// Colorează selectul de prioritate și opțiunile în funcție de valoare
const prioritySelect = document.getElementById('priority');
function updatePriorityColor() {
    prioritySelect.classList.remove('priority-high', 'priority-medium', 'priority-low');
    let color = '';
    if (prioritySelect.value === 'ridicat') {
        prioritySelect.classList.add('priority-high');
        color = '#e57373';
    } else if (prioritySelect.value === 'mediu') {
        prioritySelect.classList.add('priority-medium');
        color = '#ffd54f';
    } else if (prioritySelect.value === 'scazut') {
        prioritySelect.classList.add('priority-low');
        color = '#81c784';
    }
    // Schimbă culoarea textului selectat (pentru compatibilitate maximă)
    prioritySelect.style.color = color;
}
// Colorează opțiunile din dropdown (doar la deschidere, nu toate browserele suportă)
Array.from(prioritySelect.options).forEach(opt => {
    if (opt.value === 'ridicat') opt.style.color = '#e57373';
    else if (opt.value === 'mediu') opt.style.color = '#ffd54f';
    else if (opt.value === 'scazut') opt.style.color = '#81c784';
});
prioritySelect.addEventListener('change', updatePriorityColor);
updatePriorityColor();
