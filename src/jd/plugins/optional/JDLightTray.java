//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.optional;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jd.Main;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDLightTray extends PluginOptional implements MouseListener, WindowStateListener {
    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_JDLIGHTTRAY");

    private static final String PROPERTY_START_MINIMIZED = "PROPERTY_START_MINIMIZED";

    private TrayIconPopup popup;

    private TrayIcon trayIcon;

    private JFrame guiFrame;

    public static int getAddonInterfaceVersion() {
        return 1;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        subConfig.setProperty(PROPERTY_START_MINIMIZED, (((MenuItem) e.getSource()).getActionID() == 0));
        subConfig.save();
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;
        if (!subConfig.getBooleanProperty(PROPERTY_START_MINIMIZED, false)) {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.JDLightTray.startMinimized", "Start minimized"), 0).setActionListener(this));
            m.setSelected(false);
        } else {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.JDLightTray.startMinimized", "Start minimized"), 1).setActionListener(this));
            m.setSelected(true);
        }
        return menu;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.JDLightTray.name", "JDLightTrayIcon");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.6+";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {
        if (JDUtilities.getJavaVersion() >= 1.6) {
            try {
                JDUtilities.getController().addControlListener(this);
                logger.info("Systemtray OK");
                initGUI();
            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + JDUtilities.getJavaVersion());
            return false;
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            logger.info("JDLightTrayIcon Init complete");
            guiFrame = SimpleGUI.CURRENTGUI.getFrame();
            if (subConfig.getBooleanProperty(PROPERTY_START_MINIMIZED, false)) {
                guiFrame.setState(JFrame.ICONIFIED);
            }
            guiFrame.addWindowStateListener(this);
            return;
        }
        super.controlEvent(event);

    }

    private void initGUI() {

        trayIcon = new TrayIcon(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(this);
        trayIcon.addMouseListener(this);

        SystemTray systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getSource() instanceof TrayIcon) {
            if (e.getClickCount() >= 1) {
                guiFrame.setVisible(!guiFrame.isVisible());
                if (guiFrame.isVisible()) guiFrame.setState(JFrame.NORMAL);
            } else {
                if (popup != null && popup.isShowing()) {
                    popup.dispose();
                    popup = null;
                } else if (SwingUtilities.isRightMouseButton(e)) {

                    int x = e.getPoint().x;
                    int y = e.getPoint().y;

                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    int limitX = (int) screenSize.getWidth() / 2;
                    int limitY = (int) screenSize.getHeight() / 2;

                    popup = new TrayIconPopup(this);

                    if (x <= limitX && y <= limitY) {
                        // top left
                        popup.setLocation(x, y);
                    } else if (x <= limitX && y >= limitY) {
                        // bottom left
                        popup.setLocation(x, y - popup.getHeight());
                    } else if (x >= limitX && y <= limitY) {
                        // top right
                        popup.setLocation(x - popup.getWidth(), y);
                    } else if (x >= limitX && y >= limitY) {
                        // bottom right
                        popup.setLocation(x - popup.getWidth(), y - popup.getHeight());
                    }

                }
            }

        }

    }

    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void onExit() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }

    public void windowStateChanged(WindowEvent arg0) {
        if ((arg0.getOldState() & JFrame.ICONIFIED) == 0) {
            if ((arg0.getNewState() & JFrame.ICONIFIED) != 0) {
                guiFrame.setVisible(false);
            }
        }
    }

}
