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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.skins.simple.Factory;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.startmenu.actions.AddContainerAction;
import jd.gui.skins.simple.startmenu.actions.AddUrlAction;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class TrayIconPopup extends JWindow implements MouseListener, MouseMotionListener, ChangeListener, ActionListener {

    private static final long serialVersionUID = 2623190748929934409L;

    private static final int ACTION_START = 0;
    private static final int ACTION_STOP = 1;
    private static final int ACTION_PAUSE = 2;
    private static final int ACTION_ADD = 3;
    private static final int ACTION_LOAD = 4;
    private static final int ACTION_UPDATE = 5;
    private static final int ACTION_RECONNECT = 6;
    private static final int ACTION_TOGGLE_PREMIUM = 7;
    private static final int ACTION_TOGGLE_CLIPBOARD = 8;
    private static final int ACTION_TOGGLE_RECONNECT = 9;
    private static final int ACTION_EXIT = 10;

    private JPanel entryPanel;
    private JPanel bottomPanel;
    private boolean enteredPopup;
    private ArrayList<JButton> entries = new ArrayList<JButton>();
    private JSpinner spMax;
    private JSpinner spMaxDls;

    public TrayIconPopup() {
        setVisible(false);
        setLayout(new MigLayout("ins 5, wrap 1", "[]", "[]5[]5[]5[]5[]"));
        addMouseMotionListener(this);
        addMouseListener(this);

        initEntryPanel();
        initBottomPanel();

        add(new JLabel("<html><b>" + JDUtilities.getJDTitle() + "</b></html>"), "align center");
        add(new JSeparator(), "growx, spanx");
        add(entryPanel);
        add(new JSeparator(), "growx, spanx");
        add(bottomPanel);

        // toFront();
        setAlwaysOnTop(true);
        pack();
    }

    private void initEntryPanel() {
        entryPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[]", "[]0[]0[]0[]0[]0[]0[]0[]0[]0[]"));

        switch (JDUtilities.getController().getDownloadStatus()) {
        case JDController.DOWNLOAD_NOT_RUNNING:
            addMenuEntry(ACTION_START, "gui.images.next", JDL.L("plugins.trayicon.popup.menu.start", "Download starten"));
            addDisabledMenuEntry("gui.images.break", JDL.L("plugins.trayicon.popup.menu.pause2", "Download pausieren"));
            break;
        case JDController.DOWNLOAD_RUNNING:
            addMenuEntry(ACTION_STOP, "gui.images.stop", JDL.L("plugins.trayicon.popup.menu.stop", "Download anhalten"));
            addMenuEntry(ACTION_PAUSE, "gui.images.break", JDL.L("plugins.trayicon.popup.menu.pause2", "Download pausieren"));
            break;
        default:
            addDisabledMenuEntry("gui.images.next", JDL.L("plugins.trayicon.popup.menu.start", "Download starten"));
            addDisabledMenuEntry("gui.images.break", JDL.L("plugins.trayicon.popup.menu.pause2", "Download pausieren"));
        }

        addMenuEntry(ACTION_ADD, "gui.images.add", JDL.L("plugins.trayicon.popup.menu.add", "Downloads hinzufügen"));
        addMenuEntry(ACTION_LOAD, "gui.images.load", JDL.L("plugins.trayicon.popup.menu.load", "Container laden"));
        addMenuEntry(ACTION_UPDATE, "gui.images.update_manager", JDL.L("plugins.trayicon.popup.menu.update", "JD aktualisieren"));
        addMenuEntry(ACTION_RECONNECT, "gui.images.reconnect", JDL.L("plugins.trayicon.popup.menu.reconnect", "Reconnect durchführen"));
        addMenuEntry(ACTION_TOGGLE_PREMIUM, getPremiumImage(), JDL.L("plugins.trayicon.popup.menu.togglePremium", "Premium an/aus"));
        addMenuEntry(ACTION_TOGGLE_CLIPBOARD, getClipBoardImage(), JDL.L("plugins.trayicon.popup.menu.toggleClipboard", "Zwischenablage an/aus"));
        addMenuEntry(ACTION_TOGGLE_RECONNECT, getReconnectImage(), JDL.L("plugins.trayicon.popup.menu.toggleReconnect", "Reconnect an/aus"));
        addMenuEntry(ACTION_EXIT, "gui.images.exit", JDL.L("plugins.trayicon.popup.menu.exit", "Beenden"));
    }

    private void initBottomPanel() {
        int maxspeed = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

        spMax = new JSpinner();
        spMax.setModel(new SpinnerNumberModel(maxspeed, 0, Integer.MAX_VALUE, 50));
        spMax.setToolTipText(JDL.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen (KB/s) [0:unendlich]"));
        spMax.addChangeListener(this);

        spMaxDls = new JSpinner();
        spMaxDls.setModel(new SpinnerNumberModel(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setToolTipText(JDL.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
        spMaxDls.addChangeListener(this);

        bottomPanel = new JPanel(new MigLayout("ins 0, wrap 2", "[]5[]", "[]2[]"));
        bottomPanel.setOpaque(false);
        bottomPanel.add(new JLabel(JDL.L("plugins.trayicon.popup.bottom.speed", "Geschwindigkeitsbegrenzung")));
        bottomPanel.add(spMax, "w 60!, h 20!");
        bottomPanel.add(new JLabel(JDL.L("plugins.trayicon.popup.bottom.simDls", "Gleichzeitige Downloads")));
        bottomPanel.add(spMaxDls, "w 60!, h 20!");
    }

    private void addDisabledMenuEntry(String iconKey, String label) {
        JButton b = Factory.createButton(label, JDTheme.II(iconKey, 16, 16));
        b.setOpaque(false);
        b.setEnabled(false);
        b.setForeground(Color.GRAY);

        entryPanel.add(b);
    }

    private void addMenuEntry(Integer id, String iconKey, String label) {
        JButton b = Factory.createButton(label, JDTheme.II(iconKey, 16, 16), this);
        b.setOpaque(false);

        entryPanel.add(b);

        while (entries.size() <= id) {
            entries.add(null);
        }
        entries.set(id, b);
    }

    private String getClipBoardImage() {
        if (ClipboardHandler.getClipboard().isEnabled()) {
            return "gui.images.clipboard_enabled";
        } else {
            return "gui.images.clipboard_disabled";
        }
    }

    private String getReconnectImage() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
            return "gui.images.reconnect_enabled";
        } else {
            return "gui.images.reconnect_disabled";
        }
    }

    private String getPremiumImage() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
            return "gui.images.premium_enabled";
        } else {
            return "gui.images.premium_disabled";
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        enteredPopup = true;
    }

    public void mouseExited(MouseEvent e) {
        if (e.getSource() == this && enteredPopup && !this.contains(e.getPoint())) {
            dispose();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void stateChanged(ChangeEvent e) {
        int max = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

        if (e.getSource() == spMax) {
            int value = (Integer) spMax.getValue();

            if (max != value) {
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                SubConfiguration.getConfig("DOWNLOAD").save();
            }
        } else if (e.getSource() == spMaxDls) {
            int value = (Integer) spMaxDls.getValue();

            if (max != value) {
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                SubConfiguration.getConfig("DOWNLOAD").save();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        int index = entries.indexOf(e.getSource());
        if (index < 0) return;

        switch (index) {
        case TrayIconPopup.ACTION_ADD:
            dispose();
            AddUrlAction.addUrlDialog();
            break;
        case TrayIconPopup.ACTION_LOAD:
            dispose();
            AddContainerAction.addContainerDialog();
            break;
        case TrayIconPopup.ACTION_PAUSE:
            JDUtilities.getController().pauseDownloads(true);
            break;
        case TrayIconPopup.ACTION_RECONNECT:
            // TODO
            if (SimpleGUI.CURRENTGUI != null) SimpleGUI.CURRENTGUI.doManualReconnect();
            break;
        case TrayIconPopup.ACTION_START:
        case TrayIconPopup.ACTION_STOP:
            JDUtilities.getController().toggleStartStop();
            break;
        case TrayIconPopup.ACTION_TOGGLE_CLIPBOARD:
            ClipboardHandler.getClipboard().setEnabled(!ClipboardHandler.getClipboard().isEnabled());
            break;
        case TrayIconPopup.ACTION_TOGGLE_RECONNECT:
            Reconnecter.toggleReconnect();
            break;
        case TrayIconPopup.ACTION_UPDATE:
            new WebUpdate().doUpdateCheck(true, true);
            break;
        case TrayIconPopup.ACTION_EXIT:
            // TODO
            if (SimpleGUI.CURRENTGUI != null) SimpleGUI.CURRENTGUI.getContentPane().getRightPanel().hide();
            JDUtilities.getController().exit();
            break;
        case TrayIconPopup.ACTION_TOGGLE_PREMIUM:
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
            JDUtilities.getConfiguration().save();
            break;
        }
        dispose();
    }

}