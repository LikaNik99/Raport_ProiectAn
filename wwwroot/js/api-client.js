// api-client.js - Client pentru comunicarea cu backend-ul MongoDB
// Folosește automat host-ul paginii (pentru acces LAN). Dacă rulezi direct fișierul, fallback localhost.
const HOST = (typeof window !== 'undefined' && window.location && window.location.hostname) ? window.location.hostname : 'localhost';
const API_BASE_URL = `http://${HOST}:3000`;

// Stochează user-ul curent
let currentUser = JSON.parse(localStorage.getItem('currentUser') || 'null');

// ========== PAROLE ==========

async function getPasswords(userId = null) {
    try {
        const url = userId 
            ? `${API_BASE_URL}/passwords?userId=${userId}`
            : `${API_BASE_URL}/passwords`;
            
        const response = await fetch(url);
        const passwords = await response.json();
        
        // Convertește _id în id pentru compatibilitate
        return passwords.map(p => ({ ...p, id: p._id, _id: undefined }));
    } catch (error) {
        console.error('Eroare obținere parole:', error);
        return [];
    }
}

async function addPassword(passwordData) {
    try {
        const response = await fetch(`${API_BASE_URL}/passwords`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(passwordData)
        });
        
        const result = await response.json();
        return { success: result.success, id: result.id };
    } catch (error) {
        console.error('Eroare adăugare parolă:', error);
        return { success: false, error };
    }
}

async function updatePassword(id, updates) {
    try {
        const response = await fetch(`${API_BASE_URL}/passwords/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        });
        
        const result = await response.json();
        return { success: result.success };
    } catch (error) {
        console.error('Eroare actualizare parolă:', error);
        return { success: false, error };
    }
}

async function deletePassword(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/passwords/${id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        return { success: result.success };
    } catch (error) {
        console.error('Eroare ștergere parolă:', error);
        return { success: false, error };
    }
}

// ========== CERERI ==========


async function createRequest(requestData) {
    try {
        const response = await fetch(`${API_BASE_URL}/requests`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        });
        
        const result = await response.json();
        return { success: result.success, id: result.id };
    } catch (error) {
        console.error('Eroare creare cerere:', error);
        return { success: false, error };
    }
}

async function updateRequest(id, updates) {
    try {
        const response = await fetch(`${API_BASE_URL}/requests/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        });
        
        const result = await response.json();
        return { success: result.success };
    } catch (error) {
        console.error('Eroare actualizare cerere:', error);
        return { success: false, error };
    }
}

async function deleteRequest(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/requests/${id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        return { success: result.success };
    } catch (error) {
        console.error('Eroare ștergere cerere:', error);
        return { success: false, error };
    }
}

// Export funcțiilor pentru utilizare în alte scripturi
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        login,
        logout,
        getCurrentUser,
        getPasswords,
        addPassword,
        updatePassword,
        deletePassword,
        getRequests,
        createRequest,
        updateRequest,
        deleteRequest
    };
}
