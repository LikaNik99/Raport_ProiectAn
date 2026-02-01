const { app, BrowserWindow, Menu } = require('electron');
const path = require('path');
const http = require('http');
const os = require('os');

// Configurare pentru instanțe multiple (evită erorile de cache locking și separă sesiunile)
// Fiecare fereastră va avea propriul său director de date temporar.
const userDataDir = path.join(os.tmpdir(), `chat-electron-${Date.now()}-${process.pid}`);
app.setPath('userData', userDataDir);

// Configurare
const DOCKER_PORT = 5555;
const SERVER_URL = `http://localhost:${DOCKER_PORT}`;
const APP_TITLE = 'Chat App (Docker Client)';

let mainWindow;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        backgroundColor: '#0a0f21',
        icon: path.join(__dirname, '../client/assets/favicon.ico'), // iconita daca exista
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
        },
        title: APP_TITLE,
        show: false // Afișăm doar când e gata
    });

    // Încărcare URL
    mainWindow.loadURL(SERVER_URL).catch(err => {
        console.error("Failed to load URL:", err);
        mainWindow.loadFile(path.join(__dirname, '../client/error_connect.html')).catch(() => {
            // Fallback simplu daca nu avem html de eroare
            mainWindow.loadURL(`data:text/html;charset=utf-8,<h1>Eroare Conexiune</h1><p>Nu ma pot conecta la serverul Docker (${SERVER_URL}).</p><p>Asigura-te ca ai pornit containerul: <code>docker-compose up -d</code></p><button onclick="location.reload()">Reincearca</button>`);
        });
    });

    mainWindow.once('ready-to-show', () => {
        mainWindow.show();
    });

    // Meniu simplu (opțional)
    const menuTemplate = [
        {
            label: 'Fereastră',
            submenu: [
                { role: 'reload', label: 'Reîncarcă' },
                { role: 'toggleDevTools', label: 'Developer Tools' },
                { type: 'separator' },
                { role: 'quit', label: 'Ieșire' }
            ]
        }
    ];
    const menu = Menu.buildFromTemplate(menuTemplate);
    Menu.setApplicationMenu(menu);

    mainWindow.on('closed', function () {
        mainWindow = null;
    });
}

// Permite rularea mai multor instanțe (pentru testare multi-user)
// Implicit Electron blochează a doua instanță, dar noi vrem să permitem asta pentru testare.
// Eliminăm verificarea `requestSingleInstanceLock` sau o adaptăm.
// De fapt, pentru dezvoltare, rulând `np run start:electron` din terminale diferite, se vor deschide instante diferite ale procesului Electron.
// Dar dacă build-uiești app-ul, va fi single instance. 
// Aici, fiind un script de dev, e ok.

app.on('ready', () => {
    console.log(`🔌 Connecting to Docker server at ${SERVER_URL}...`);
    createWindow();
});

app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') app.quit();
});

app.on('activate', function () {
    if (mainWindow === null) createWindow();
});
