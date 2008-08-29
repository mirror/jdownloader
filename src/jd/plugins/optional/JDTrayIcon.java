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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jd.Main;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.controlling.ClipboardHandler;
import jd.event.ControlEvent;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDTrayIcon extends PluginOptional implements WindowListener {
    private class info extends Thread {
        private Point p;

        public info(Point p) {
            this.p = p;
        }

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                interrupt();
            }

            if (popupMenu.isVisible()) { return; }

            String displaytext = "";
            int speed = 0;
            int downloads = 0;

            showTooltip(p);

            while (counter > 0) {
                displaytext = "<html><center><b>jDownloader</b></center><br><br>";
                downloads = JDUtilities.getController().getRunningDownloadNum();

                if (downloads == 0) {
                    displaytext += JDLocale.L("plugins.optional.trayIcon.nodownload", "No Download in progress") + "<br>";
                } else {
                    displaytext += "<i>" + JDLocale.L("plugins.optional.trayIcon.downloads", "Downloads:") + "</i> " + downloads + "<br>";
                }

                speed = JDUtilities.getController().getSpeedMeter() / 1000;

                displaytext += "<br><i>" + JDLocale.L("plugins.optional.trayIcon.speed", "Speed:") + "</i> " + speed + "kb/s";

                displaytext += "</html>";
                toollabel.setText(displaytext);

                counter--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }

            hideTooltip();
        }
    }

    public static int getAddonInterfaceVersion() {
        return 1;
    }

    private JCheckBoxMenuItem clipboard;
    private JMenuItem configuration;
    private int counter = 0;
    private JMenuItem dnd;
    private JMenuItem exit;
    private info i;
    private JPopupMenu popupMenu;
    private JCheckBoxMenuItem reconnect;
    private JMenuItem showhide;
    private JMenuItem speed1;
    private JMenuItem speed2;
    private JMenuItem speed3;
    private JMenuItem speed4;
    private JMenuItem speed5;
    private JMenu speeds;
    private JMenuItem startstop;
    private JMenuItem stopafter;
    private JLabel toollabel;
    private JWindow toolparent;
    private TrayIcon trayIcon;
    private JWindow trayParent;

    private JMenuItem update;

    public void actionPerformed(ActionEvent e) {
        SimpleGUI simplegui = (SimpleGUI) JDUtilities.getGUI();
        if (e.getSource() == showhide) {
            toggleshowhide();
        } else if (e.getSource() == exit) {
            JDUtilities.getController().exit();

        } else if (e.getSource() == startstop) {
            JDUtilities.getController().toggleStartStop();

        } else if (e.getSource() == clipboard) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CLIPBOARD, null));
        } else if (e.getSource() == dnd) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.ITEMS_DND, null));

        } else if (e.getSource() == stopafter) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_PAUSE_DOWNLOADS, null));
        } else if (e.getSource() == update) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_UPDATE, null));
        } else if (e.getSource() == configuration) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CONFIGURATION, null));
        } else if (e.getSource() == reconnect) {
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));

            JDUtilities.saveConfig();
        } else if (e.getSource() == speed1) {
            int speed = getPluginConfig().getIntegerProperty("SPEED1", 100);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed2) {
            int speed = getPluginConfig().getIntegerProperty("SPEED2", 200);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed3) {
            int speed = getPluginConfig().getIntegerProperty("SPEED3", 300);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed4) {
            int speed = getPluginConfig().getIntegerProperty("SPEED4", 400);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed5) {
            int speed = getPluginConfig().getIntegerProperty("SPEED5", 500);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            simplegui.setSpeedStatusBar(speed);
        }
    }

    /**
     * Compute the proper position for a popup
     */
    private Point computeDisplayPoint(int x, int y, Dimension dim) {
        if (x - dim.width > 0) {
            x -= dim.width;
        }
        if (y - dim.height > 0) {
            y -= dim.height;
        }
        return new Point(x, y);
    }

    private JMenuItem createMenuItem(String name) {
        JMenuItem menuItem = new JMenuItem(name);
        menuItem.setIcon(null);
        menuItem.addActionListener(this);
        popupMenu.add(menuItem);
        return menuItem;
    }

    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    public String getCoder() {
        return "JD-Team";
    }

    public String getPluginName() {
        return JDLocale.L("plugins.optional.trayIcon.name", "TrayIcon");
    }

    public String getRequirements() {
        return "JRE 1.6+";
    }

    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    private void hideTooltip() {
        toolparent.setVisible(false);
        counter = 0;
    }

    public boolean initAddon() {
        if (JDUtilities.getJavaVersion() >= 1.6) {
            try {

                JDUtilities.getController().addControlListener(this);
                logger.info("Systemtray OK");
                initGUI();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        } else {
            logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + JDUtilities.getJavaVersion());
            return false;
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            logger.info("JD Init complete");
            SimpleGUI.CURRENTGUI.getFrame().addWindowListener(this);
            return;
        }
        super.controlEvent(event);

    }

    private void initGUI() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED1", JDLocale.L("plugins.optional.trayIcon.speed1", "Speed 1:"), 1, 100000).setDefaultValue(100));
        cfg.setDefaultValue("100");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED2", JDLocale.L("plugins.optional.trayIcon.speed2", "Speed 2:"), 1, 100000).setDefaultValue(200));
        cfg.setDefaultValue("200");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED3", JDLocale.L("plugins.optional.trayIcon.speed3", "Speed 3:"), 1, 100000).setDefaultValue(300));
        cfg.setDefaultValue("300");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED4", JDLocale.L("plugins.optional.trayIcon.speed4", "Speed 4:"), 1, 100000).setDefaultValue(400));
        cfg.setDefaultValue("400");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED5", JDLocale.L("plugins.optional.trayIcon.speed5", "Speed 5:"), 1, 100000).setDefaultValue(500));
        cfg.setDefaultValue("500");

        popupMenu = new JPopupMenu();
        showhide = createMenuItem(JDLocale.L("plugins.optional.trayIcon.hide", "Hide"));
        popupMenu.addSeparator();
        update = createMenuItem(JDLocale.L("plugins.optional.trayIcon.update", "Update"));
        configuration = createMenuItem(JDLocale.L("plugins.optional.trayIcon.configuration", "Configuration"));
        popupMenu.addSeparator();
        startstop = createMenuItem(JDLocale.L("plugins.optional.trayIcon.startorstop", "Start/Stop"));
        stopafter = createMenuItem(JDLocale.L("plugins.optional.trayIcon.stopafter", "Stop after this Download"));

        speeds = new JMenu(JDLocale.L("plugins.optional.trayIcon.setspeeds", "Speeds"));
        popupMenu.add(speeds);

        speed1 = new JMenuItem(getPluginConfig().getStringProperty("SPEED1", "100") + " kb/s");
        speed1.addActionListener(this);
        speed1.setIcon(null);
        speeds.add(speed1);

        speed2 = new JMenuItem(getPluginConfig().getStringProperty("SPEED2", "200") + " kb/s");
        speed2.addActionListener(this);
        speed2.setIcon(null);
        speeds.add(speed2);

        speed3 = new JMenuItem(getPluginConfig().getStringProperty("SPEED3", "300") + " kb/s");
        speed3.addActionListener(this);
        speed3.setIcon(null);
        speeds.add(speed3);

        speed4 = new JMenuItem(getPluginConfig().getStringProperty("SPEED4", "400") + " kb/s");
        speed4.addActionListener(this);
        speed4.setIcon(null);
        speeds.add(speed4);

        speed5 = new JMenuItem(getPluginConfig().getStringProperty("SPEED5", "500") + " kb/s");
        speed5.addActionListener(this);
        speed5.setIcon(null);
        speeds.add(speed5);

        popupMenu.addSeparator();

        dnd = createMenuItem(JDLocale.L("plugins.optional.trayIcon.dnd", "Drag'n Drop"));

        clipboard = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.trayIcon.clipboard", "Clipboard"), false);
        popupMenu.add(clipboard);
        clipboard.addActionListener(this);

        reconnect = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.trayIcon.reconnect", "Reconnect"), false);
        popupMenu.add(reconnect);
        reconnect.addActionListener(this);

        popupMenu.add(reconnect);
        popupMenu.addSeparator();
        exit = createMenuItem(JDLocale.L("plugins.optional.trayIcon.exit", "Exit"));

        trayIcon = new TrayIcon(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        trayIcon.setImageAutoSize(true);

        trayParent = new JWindow();
        trayParent.setSize(0, 0);
        trayParent.setAlwaysOnTop(true);
        trayParent.setVisible(false);

        toolparent = new JWindow();
        toolparent.setSize(200, 100);
        toolparent.setAlwaysOnTop(true);
        toolparent.setVisible(false);

        toollabel = new JLabel("jDownloader");
        toollabel.setBounds(0, 0, toolparent.getWidth(), toolparent.getHeight());
        toollabel.setVisible(true);
        toollabel.setOpaque(true);
        toollabel.setBackground(new Color(0xb9cee9));

        toolparent.setLayout(null);
        toolparent.add(toollabel);

        setTrayPopUp(popupMenu);

        SystemTray systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void onExit() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }

    }

    private void setTrayPopUp(JPopupMenu trayMenu) {
        popupMenu = trayMenu;

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                trayParent.setVisible(false);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        popupMenu.setVisible(true);
        popupMenu.setVisible(false);

        trayIcon.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {

                    if (toolparent.isVisible()) {
                        hideTooltip();
                    }

                    if (e.getClickCount() == 2) {
                        toggleshowhide();
                    }
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e.getPoint());
                }
            }
        });

        trayIcon.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                if (popupMenu.isVisible()) { return; }
                if (counter > 0) {
                    counter = 2;
                    return;
                }

                counter = 2;

                i = new info(e.getPoint());
                i.start();
            }
        });
    }

    private void showPopup(final Point p) {
        toggleshowhide();
        toggleshowhide();
        trayParent.setVisible(true);
        trayParent.toFront();
        hideTooltip();

        clipboard.setSelected(ClipboardHandler.getClipboard().isEnabled());
        reconnect.setSelected(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));

        speed1.setText(getPluginConfig().getStringProperty("SPEED1", "100") + " kb/s");
        speed2.setText(getPluginConfig().getStringProperty("SPEED2", "200") + " kb/s");
        speed3.setText(getPluginConfig().getStringProperty("SPEED3", "300") + " kb/s");
        speed4.setText(getPluginConfig().getStringProperty("SPEED4", "400") + " kb/s");
        speed5.setText(getPluginConfig().getStringProperty("SPEED5", "500") + " kb/s");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Point p2 = computeDisplayPoint(p.x, p.y, popupMenu.getPreferredSize());
                popupMenu.show(trayParent, p2.x - trayParent.getLocation().x, p2.y - trayParent.getLocation().y);
            };
        });
    }

    private void showTooltip(final Point p) {
        toolparent.setVisible(true);
        toolparent.toFront();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;

                if (p.x <= limitX && p.y <= limitY) {
                    // top left
                    toolparent.setLocation(p.x, p.y);
                } else if (p.x <= limitX && p.y >= limitY) {
                    // bottom left
                    toolparent.setLocation(p.x, p.y - toolparent.getHeight());
                } else if (p.x >= limitX && p.y <= limitY) {
                    // top right
                    toolparent.setLocation(p.x - toolparent.getWidth(), p.y);
                } else if (p.x >= limitX && p.y >= limitY) {
                    // bottom right
                    toolparent.setLocation(p.x - toolparent.getWidth(), p.y - toolparent.getHeight());
                }

            };
        });
    }

    private void toggleshowhide() {
        SimpleGUI simplegui = (SimpleGUI) JDUtilities.getGUI();
        if (showhide.getText().equals(JDLocale.L("plugins.optional.trayIcon.hide", "Hide"))) {

            simplegui.getFrame().setVisible(false);
            showhide.setText(JDLocale.L("plugins.optional.trayIcon.show", "Show"));
        } else if (showhide.getText().equals(JDLocale.L("plugins.optional.trayIcon.show", "Show"))) {

            simplegui.getFrame().setVisible(true);
            showhide.setText(JDLocale.L("plugins.optional.trayIcon.hide", "Hide"));
            simplegui.getFrame().setState(JFrame.NORMAL);
        }
    }

    public void windowActivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowClosed(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowClosing(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowDeactivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowDeiconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    public void windowIconified(WindowEvent e) {
        if (e.getSource() == SimpleGUI.CURRENTGUI.getFrame()) {
            SimpleGUI simplegui = (SimpleGUI) JDUtilities.getGUI();

            simplegui.getFrame().setVisible(false);
            showhide.setText(JDLocale.L("plugins.optional.trayIcon.show", "Show"));

        }

    }

    public void windowOpened(WindowEvent e) {
        // TODO Auto-generated method stub

    }
}
