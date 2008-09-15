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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import jd.Main;
import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDLightTray extends PluginOptional implements MouseListener, MouseMotionListener, WindowStateListener {
    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_JDLIGHTTRAY");

    private static final String PROPERTY_START_MINIMIZED = "PROPERTY_START_MINIMIZED";

    private static final int INIT_COUNTER = 5;

    private int counter = 0;

    private JWindow toolParent;

    private JLabel toolLabel;

    private TrayIconPopup trayIconPopup;

    private TrayIcon trayIcon;

    private TrayInfo trayInfo;

    private JFrame guiFrame;

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    public JDLightTray(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem) {
            subConfig.setProperty(PROPERTY_START_MINIMIZED, (((MenuItem) e.getSource()).getActionID() == 0));
            subConfig.save();
        }
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
    public String getHost() {
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
        trayIcon.addMouseMotionListener(this);

        toolLabel = new JLabel("jDownloader");
        toolLabel.setVisible(true);
        toolLabel.setOpaque(true);
        toolLabel.setBackground(new Color(0xb9cee9));

        toolParent = new JWindow();
        toolParent.setAlwaysOnTop(true);
        toolParent.add(toolLabel);
        toolParent.pack();
        toolParent.setVisible(false);

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
            if (toolParent.isVisible()) {
                hideTooltip();
            }

            if (e.getClickCount() >= 1 && !SwingUtilities.isRightMouseButton(e)) {
                guiFrame.setVisible(!guiFrame.isVisible());
                if (guiFrame.isVisible()) guiFrame.setState(JFrame.NORMAL);
            } else {
                if (trayIconPopup != null && trayIconPopup.isShowing()) {
                    trayIconPopup.dispose();
                    trayIconPopup = null;
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    trayIconPopup = new TrayIconPopup(this);
                    calcLocation(trayIconPopup, e.getPoint());
                    trayIconPopup.setVisible(true);
                }
            }

        }

    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        if (trayIconPopup != null && trayIconPopup.isVisible()) return;
        if (counter > 0) {
            counter = INIT_COUNTER;
            return;
        }

        counter = INIT_COUNTER;

        trayInfo = new TrayInfo(e.getPoint());
        trayInfo.start();
        // trayIcon.displayMessage("jDownloader", "Erstmal nur ein Test, ok?",
        // TrayIcon.MessageType.INFO);
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

    private void calcLocation(final JWindow window, final Point p) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;

                if (p.x <= limitX) {
                    if (p.y <= limitY) {
                        // top left
                        window.setLocation(p.x, p.y);
                    } else {
                        // bottom left
                        window.setLocation(p.x, p.y - window.getHeight());
                    }
                } else {
                    if (p.y <= limitY) {
                        // top right
                        window.setLocation(p.x - window.getWidth(), p.y);
                    } else {
                        // bottom right
                        window.setLocation(p.x - window.getWidth(), p.y - window.getHeight());
                    }
                }

            }
        });
    }

    private void hideTooltip() {
        toolParent.setVisible(false);
        counter = 0;
    }

    private String createHTMLInfoString() {
        StringBuilder creater = new StringBuilder();
        creater.append("<html><center><b>jDownloader</b></center><hr>");
        int downloads = JDUtilities.getController().getRunningDownloadNum();
        if (downloads == 0) {
            creater.append(JDLocale.L("plugins.optional.trayIcon.nodownload", "No Download in progress") + "<br>");
        } else {
            creater.append("<table>");
            creater.append("<tr><td><i>" + JDLocale.L("plugins.optional.trayIcon.downloads", "Downloads:") + "</i></td>" + downloads + "</tr>");
            creater.append("<tr><td><i>" + JDLocale.L("plugins.optional.trayIcon.speed", "Speed:") + "</i></td>" + JDUtilities.formatKbReadable(JDUtilities.getController().getSpeedMeter() / 1024) + "/s </tr>");
            creater.append("</table>");
        }
        creater.append("</html>");

        return creater.toString();
    }

    private class TrayInfo extends Thread {
        private Point p;

        public TrayInfo(Point p) {
            this.p = p;
        }

        public void run() {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                interrupt();
            }

            if (trayIconPopup != null && trayIconPopup.isVisible()) return;

            toolLabel.setText(createHTMLInfoString());
            toolParent.pack();
            calcLocation(toolParent, p);
            toolParent.setVisible(true);
            toolParent.toFront();

            while (counter > 0) {
                toolLabel.setText(createHTMLInfoString());
                toolParent.pack();

                counter--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }

            hideTooltip();
        }
    }

}
