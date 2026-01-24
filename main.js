const { app, BrowserWindow } = require('electron');
const path = require('path');
const os = require('os');

// Pornește serverul Express existent
// Acesta va rula codul din server.js exact ca "node server.js"
require('./server.js');

function getLocalIp() {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            // Returnează prima adresă IPv4 care nu este internă (nu e localhost)
            if (iface.family === 'IPv4' && !iface.internal) {
                return iface.address;
            }
        }
    }
    return 'localhost'; // Fallback
}

function createWindow() {
    const win = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            nodeIntegration: false, // Securitate: nu permite Node în renderer (pagini web)
            contextIsolation: true,
        },
        title: "Taskboard App"
    });

    // Încărcăm URL-ul serverului local folosind IP-ul rețelei
    const PORT = process.env.PORT || 3000;
    const IP = getLocalIp();
    const URL = `http://${IP}:${PORT}`;

    console.log(`🌐 Application loading at: ${URL}`);
    console.log(`📡 To connect from another device, ensure you are on the same Wi-Fi using the URL above.`);

    // Așteptăm un pic (opțional) sau încărcăm direct.
    win.loadURL(URL);

    // Deschide DevTools (opțional, pentru debug)
    // win.webContents.openDevTools();
}

app.whenReady().then(() => {
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
