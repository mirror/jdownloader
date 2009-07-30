//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.optional.jdtrayicon;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.skins.SwingGui;
import jd.gui.swing.GuiRunnable;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "trayicon", interfaceversion = 4, minJVM = 1.6)
public class JDLightTray extends PluginOptional implements MouseListener, MouseMotionListener, WindowListener {

    private SubConfiguration subConfig = null;

    private static final String PROPERTY_START_MINIMIZED = "PROPERTY_START_MINIMIZED";

    private static final String PROPERTY_MINIMIZE_TO_TRAY = "PROPERTY_MINIMIZE_TO_TRAY";

    private static final String PROPERTY_SINGLE_CLICK = "PROPERTY_SINGLE_CLICK";

    private static final String PROPERTY_TOOLTIP = "PROPERTY_TOOLTIP";

    private TrayIconPopup trayIconPopup;

    private TrayIcon trayIcon;

    private JFrame guiFrame;

    private long lastDeIconifiedEvent = System.currentTimeMillis() - 1000;

    private TrayIconTooltip trayIconTooltip;

    public JDLightTray(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = SubConfiguration.getConfig("ADDONS_JDLIGHTTRAY");
        initConfig();
    }

    // @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    // @Override
    public boolean initAddon() {
        return new GuiRunnable<Boolean>() {

            @Override
            public Boolean runSave() {
                if (JDUtilities.getJavaVersion() < 1.6) {
                    logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + JDUtilities.getJavaVersion());
                    return false;
                }
                if (!SystemTray.isSupported()) {
                    logger.severe("Error initializing SystemTray: Tray isn't supported jet");
                    return false;
                }
                try {
                    JDUtilities.getController().addControlListener(JDLightTray.this);
                    if (SwingGui.getInstance() != null && SwingGui.getInstance() != null) {
                        guiFrame = SwingGui.getInstance().getMainFrame();
                        guiFrame.addWindowListener(JDLightTray.this);
                    }
                    logger.info("Systemtray OK");
                    initGUI();
                } catch (Exception e) {
                    return false;
                }
                return true;

            }

        }.getReturnValue();

    }

    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_MINIMIZE_TO_TRAY, JDL.L("plugins.optional.JDLightTray.minimizetotray", "Minimize to tray")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_START_MINIMIZED, JDL.L("plugins.optional.JDLightTray.startMinimized", "Start minimized")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SINGLE_CLICK, JDL.L("plugins.optional.JDLightTray.singleClick", "Toggle window status with single click")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_TOOLTIP, JDL.L("plugins.optional.JDLightTray.tooltip", "Show Tooltip")).setDefaultValue(true));
    }

    // @Override
    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            logger.info("JDLightTrayIcon Init complete");
            guiFrame = SwingGui.getInstance().getMainFrame();
            if (subConfig.getBooleanProperty(PROPERTY_START_MINIMIZED, false)) {
                guiFrame.setState(JFrame.ICONIFIED);
            }
            guiFrame.addWindowListener(this);
            return;
        }
        super.controlEvent(event);
    }

    private void initGUI() {
        SystemTray systemTray = SystemTray.getSystemTray();
        Image img = JDImage.getImage("logo/jd_logo_128_128").getScaledInstance((int) systemTray.getTrayIconSize().getWidth(), (int) systemTray.getTrayIconSize().getHeight(), Image.SCALE_SMOOTH);

        trayIcon = new TrayIcon(img);
        trayIcon.addActionListener(this);

        TrayMouseAdapter ma = new TrayMouseAdapter(this, trayIcon);
        trayIcon.addMouseListener(ma);
        trayIcon.addMouseMotionListener(ma);

        trayIconTooltip = new TrayIconTooltip();

        try {
            systemTray.add(trayIcon);
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        trayIconTooltip.hideWindow();
    }

    public void mousePressed(MouseEvent e) {

        trayIconTooltip.hideWindow();
        if (e.getSource() instanceof TrayIcon) {
            if (!OSDetector.isMac()) {
                if (e.getClickCount() >= (subConfig.getBooleanProperty(PROPERTY_SINGLE_CLICK, false) ? 1 : 2) && !SwingUtilities.isRightMouseButton(e)) {
                    miniIt();
                } else {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        trayIconPopup = new TrayIconPopup();
                        calcLocation(trayIconPopup, e.getPoint());
                        trayIconPopup.setVisible(true);
                    }
                }
            } else if (e.getSource() instanceof JWindow) {

            } else {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= (subConfig.getBooleanProperty(PROPERTY_SINGLE_CLICK, false) ? 1 : 2) && !SwingUtilities.isLeftMouseButton(e)) {
                        miniIt();
                    } else {
                        if (trayIconPopup != null && trayIconPopup.isShowing()) {
                            trayIconPopup.dispose();
                            trayIconPopup = null;
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            trayIconPopup = new TrayIconPopup();
                            Point pointOnScreen = e.getLocationOnScreen();
                            if (e.getX() > 0) pointOnScreen.x -= e.getPoint().x;
                            calcLocation(trayIconPopup, pointOnScreen);
                            trayIconPopup.setVisible(true);
                        }
                    }
                }
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {

    }

    // @Override
    public void onExit() {
        if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
        JDUtilities.getController().removeControlListener(this);
        if (guiFrame != null) guiFrame.removeWindowListener(this);
    }

    private void calcLocation(final JWindow window, final Point p) {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;
                if (!OSDetector.isMac()) {
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
                } else {
                    if (p.getX() <= (screenSize.getWidth() - window.getWidth())) {
                        window.setLocation((int) p.getX(), 22);
                    } else {
                        window.setLocation(p.x - window.getWidth(), 22);
                    }
                }

                return null;
            }
        }.waitForEDT();
    }

    private void miniIt() {
        if (System.currentTimeMillis() > this.lastDeIconifiedEvent + 750) {
            this.lastDeIconifiedEvent = System.currentTimeMillis();
            if (guiFrame.isVisible()) {
                guiFrame.setVisible(false);
            } else {
                guiFrame.setState(JFrame.NORMAL);
                guiFrame.setVisible(true);
                guiFrame.toFront();
            }
        }
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
        if (subConfig.getBooleanProperty(PROPERTY_MINIMIZE_TO_TRAY, true)) {
            guiFrame.setState(JFrame.NORMAL);
            guiFrame.setVisible(true);
            miniIt();
        }
    }

    public void windowOpened(WindowEvent arg0) {
    }

    /**
     * gets called if mouse stays over the tray. Edit delay in
     * TrayJDMouseAdapter
     * 
     * @param me
     */
    public void mouseStay(MouseEvent e) {

        if (!subConfig.getBooleanProperty(PROPERTY_TOOLTIP, true)) return;
        if (trayIconPopup != null && trayIconPopup.isVisible()) return;

        trayIconTooltip.show(((TrayMouseAdapter) e.getSource()).getEstimatedTopLeft(), this.trayIcon);

    }
}