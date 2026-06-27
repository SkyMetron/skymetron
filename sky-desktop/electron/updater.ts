import { autoUpdater } from 'electron-updater';
import { BrowserWindow } from 'electron';

let mainWindow: BrowserWindow | null = null;

export function initUpdater(window: BrowserWindow) {
  mainWindow = window;

  autoUpdater.autoDownload = false;
  autoUpdater.autoInstallOnAppQuit = true;

  autoUpdater.on('checking-for-update', () => {
    mainWindow?.webContents.send('update-status', 'checking');
  });

  autoUpdater.on('update-available', (info) => {
    mainWindow?.webContents.send('update-status', 'available', {
      version: info.version,
      releaseDate: info.releaseDate,
    });
  });

  autoUpdater.on('update-not-available', () => {
    mainWindow?.webContents.send('update-status', 'not-available');
  });

  autoUpdater.on('download-progress', (progress) => {
    mainWindow?.webContents.send('update-status', 'downloading', {
      percent: progress.percent,
      bytesPerSecond: progress.bytesPerSecond,
      total: progress.total,
      transferred: progress.transferred,
    });
  });

  autoUpdater.on('update-downloaded', (info) => {
    mainWindow?.webContents.send('update-status', 'downloaded', {
      version: info.version,
    });
  });

  autoUpdater.on('error', (err) => {
    mainWindow?.webContents.send('update-status', 'error', {
      message: err.message,
    });
  });

  autoUpdater.checkForUpdates();
}

export function downloadUpdate() {
  autoUpdater.downloadUpdate();
}

export function installUpdate() {
  autoUpdater.quitAndInstall(false, true);
}
