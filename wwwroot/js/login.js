// login.js - autentificare prin API (MongoDB)
// Necesită ca serverul Node să ruleze pe http://localhost:3000
// Dacă api-client.js este încărcat, folosim funcția login; altfel fallback simplu.

document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('.login-form');
    if (!form) return;
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value.trim();

        // Fallback vechi dacă API nu e disponibil
        if (typeof login !== 'function') {
            if (username === 'admin' && password === 'admin') {
                localStorage.setItem('currentUser', JSON.stringify({ email: 'admin', role: 'admin' }));
                window.location.href = 'password';
            } else {
                alert('User sau parolă greșite (fallback)!');
            }
            return;
        }

        try {
            const result = await login(username, password);
            if (result.success) {
                window.location.href = 'password';
            } else {
                alert(result.error || 'Autentificare eșuată');
            }
        } catch (err) {
            console.error('Eroare login:', err);
            alert('Eroare conexiune la server. Verifică dacă rulează Node.js API.');
        }
    });
});
