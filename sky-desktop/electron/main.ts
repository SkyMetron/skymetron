import { app, BrowserWindow, ipcMain } from 'electron';
import path from 'path';
import { initUpdater, downloadUpdate, installUpdate, checkNow } from './updater';

let mainWindow: BrowserWindow | null = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    titleBarStyle: 'hiddenInset',
    show: false,
  });

  if (process.env.NODE_ENV === 'development' || process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
    if (!app.isPackaged) {
      mainWindow.webContents.openDevTools();
    }
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow?.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  if (app.isPackaged) {
    initUpdater(mainWindow);
    setInterval(() => checkNow(mainWindow!), 30 * 60 * 1000);
  }
}

ipcMain.handle('update:check', () => {
  if (mainWindow) {
    if (app.isPackaged) {
      checkNow(mainWindow);
    } else {
      mainWindow.webContents.send('update-status', 'not-available');
    }
  }
});

ipcMain.handle('update:download', () => {
  downloadUpdate();
});

ipcMain.handle('update:install', () => {
  installUpdate();
});

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow();
});
