//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.jdownloader.gui.jdtrayicon;

import java.awt.Color;
import java.awt.Image;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class TrayMouseAdapter extends org.appwork.swing.trayicon.TrayMouseAdapter {
    private final TrayIcon                            trayIcon;
    private final Image                               image;
    private final GenericConfigEventListener<Boolean> clipboardToggle;
    private final AtomicBoolean                       listenerFlag = new AtomicBoolean(false);

    public TrayMouseAdapter(TrayExtension lightTray, TrayIcon trayIcon) {
        super(lightTray, trayIcon);
        this.trayIcon = trayIcon;
        this.image = trayIcon.getImage();
        clipboardToggle = new GenericConfigEventListener<Boolean>() {
            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                TrayMouseAdapter.this.trayIcon.setImage(getCurrentTrayIconImage());
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        };
    }

    public void startListener() {
        CFG_GUI.CLIPBOARD_MONITORED.getEventSender().addListener(clipboardToggle, false);
        CFG_TRAY_CONFIG.TRAY_ICON_CLIPBOARD_INDICATOR.getEventSender().addListener(clipboardToggle, false);
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                if (listenerFlag.compareAndSet(false, true)) {
                    trayIcon.addMouseListener(TrayMouseAdapter.this);
                    trayIcon.addMouseMotionListener(TrayMouseAdapter.this);
                    trayIcon.setImage(getCurrentTrayIconImage());
                }
            }
        };
    }

    protected Image getCurrentTrayIconImage() {
        if (org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled() || !CFG_TRAY_CONFIG.TRAY_ICON_CLIPBOARD_INDICATOR.isEnabled()) {
            return image;
        } else {
            return IconIO.toImage(new BadgeIcon(new ImageIcon(image), NewTheme.I().getCheckBoxImage(IconKey.ICON_CLIPBOARD, false, Math.max(8, image.getHeight(null) / 2), new Color(0xFF9393)), 4, 2));
        }
    }

    public void stopListener() {
        CFG_GUI.CLIPBOARD_MONITORED.getEventSender().removeListener(clipboardToggle);
        CFG_TRAY_CONFIG.TRAY_ICON_CLIPBOARD_INDICATOR.getEventSender().removeListener(clipboardToggle);
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                if (listenerFlag.compareAndSet(true, false)) {
                    trayIcon.removeMouseListener(TrayMouseAdapter.this);
                    trayIcon.removeMouseMotionListener(TrayMouseAdapter.this);
                    trayIcon.setImage(getCurrentTrayIconImage());
                }
            }
        };
    }

    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        abortMouseLocationObserver();
    }

    public void abortMouseLocationObserver() {
        final Thread localmouseLocationObserver = mouseLocationObserver;
        this.mouseLocationObserver = null;
        if (localmouseLocationObserver != null) {
            localmouseLocationObserver.interrupt();
        }
    }

    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        abortMouseLocationObserver();
    }
}
