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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class TrayIconPopup extends JWindow implements MouseListener, ChangeListener {

    private static final long serialVersionUID = 2623190748929934409L;

    private SubConfiguration config;
    private JPanel entryPanel;
    private JPanel bottomPanel;
    private boolean enteredPopup;
    private JSpinner spMaxSpeed;
    private JSpinner spMaxDls;
    private JSpinner spMaxChunks;

    private JPanel exitPanel;
    private ArrayList<JToggleButton> resizecomps;

    public TrayIconPopup() {
        config = SubConfiguration.getConfig("DOWNLOAD");
        resizecomps = new ArrayList<JToggleButton>();

        setVisible(false);
        setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        addMouseListener(this);

        initEntryPanel();
        initBottomPanel();
        initExitPanel();
        JPanel content = new JPanel(new MigLayout("ins 5, wrap 1", "[]", "[]5[]5[]5[]5[]"));
        add(content);
        content.add(new JLabel("<html><b>" + JDUtilities.getJDTitle() + "</b></html>"), "align center");
        content.add(new JSeparator(), "growx, spanx");
        content.add(entryPanel);

        content.add(new JSeparator(), "growx, spanx");
        content.add(bottomPanel);
        content.add(new JSeparator(), "growx, spanx");
        content.add(exitPanel);
        content.setBorder(BorderFactory.createLineBorder(content.getBackground().darker()));
        Dimension size = new Dimension(getPreferredSize().width, resizecomps.get(0).getPreferredSize().height);
        for (JToggleButton c : resizecomps) {
            c.setPreferredSize(size);
            c.setMinimumSize(size);
            c.setMaximumSize(size);
        }
        setAlwaysOnTop(true);
        pack();
    }

    /*
     * start autohide in 3 secs if mouse did not enter popup before
     */
    public void startAutoHide() {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                if (enteredPopup == false) {
                    new GuiRunnable<Object>() {
                        public Object runSave() {
                            dispose();
                            return null;
                        }
                    }.start();
                }
            }
        }.start();
    }

    private void initEntryPanel() {
        entryPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[]", "[]0[]0[]0[]0[]0[]0[]0[]0[]0[]"));
        switch (DownloadWatchDog.getInstance().getDownloadStatus()) {
        case NOT_RUNNING:
            addMenuEntry("toolbar.control.start");
            addMenuEntry("toolbar.control.stop");
            break;
        case RUNNING:
            addMenuEntry("toolbar.control.stop");
            addMenuEntry("toolbar.control.pause");
            break;
        default:
            addMenuEntry("toolbar.control.start");
            addMenuEntry("toolbar.control.pause");
        }

        addMenuEntry("action.addurl");
        addMenuEntry("action.load");
        addMenuEntry("toolbar.interaction.update");
        addMenuEntry("toolbar.interaction.reconnect");
        addMenuEntry("premiumMenu.toggle");
        addMenuEntry("toolbar.quickconfig.clipboardoberserver");
        addMenuEntry("toolbar.quickconfig.reconnecttoggle");
        addMenuEntry("action.opendlfolder");
    }

    private void initExitPanel() {
        exitPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[]", "[]"));
        exitPanel.add(getMenuEntry("action.exit"), "growx,pushx");
    }

    private void initBottomPanel() {
        spMaxSpeed = new JSpinner();
        spMaxSpeed.setModel(new SpinnerNumberModel(config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
        spMaxSpeed.setToolTipText(JDL.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen (KB/s) [0:unendlich]"));
        spMaxSpeed.addChangeListener(this);

        spMaxDls = new JSpinner();
        spMaxDls.setModel(new SpinnerNumberModel(config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setToolTipText(JDL.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
        spMaxDls.addChangeListener(this);

        spMaxChunks = new JSpinner();
        spMaxChunks.setModel(new SpinnerNumberModel(config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2), 1, 20, 1));
        spMaxChunks.setToolTipText(JDL.L("gui.tooltip.statusbar.max_chunks", "Max. Connections/File"));
        spMaxChunks.addChangeListener(this);

        bottomPanel = new JPanel(new MigLayout("ins 0, wrap 2", "[]5[]", "[]2[]2[]"));
        bottomPanel.setOpaque(false);
        bottomPanel.add(new JLabel(JDL.L("plugins.trayicon.popup.bottom.speed", "Geschwindigkeitsbegrenzung")));
        bottomPanel.add(spMaxSpeed, "width 60!,h 20!");
        bottomPanel.add(new JLabel(JDL.L("plugins.trayicon.popup.bottom.simDls", "Gleichzeitige Downloads")));
        bottomPanel.add(spMaxDls, "width 60!, h 20!");
        bottomPanel.add(new JLabel(JDL.L("plugins.trayicon.popup.bottom.simChunks", "Gleichzeitige Verbindungen")));
        bottomPanel.add(spMaxChunks, "width 60!,h 20!");
    }

    private void addMenuEntry(String actionId) {
        JToggleButton b = getMenuEntry(actionId);
        entryPanel.add(b, "growx,pushx");
    }

    private JToggleButton getMenuEntry(String actionId) {
        final ToolBarAction action = ActionController.getToolBarAction(actionId);
        JToggleButton b = createButton(action);
        resizecomps.add(b);
        return b;
    }

    private JToggleButton createButton(final ToolBarAction action) {
        action.init();
        final JToggleButton bt = new JToggleButton(action);
        bt.setContentAreaFilled(true);
        bt.setBorderPainted(true);
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                TrayIconPopup.this.dispose();
            }

        });
        bt.setOpaque(true);
        bt.setIcon((Icon) action.getValue(Action.SMALL_ICON));

        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setFocusPainted(false);

        bt.setHorizontalAlignment(JButton.LEFT);
        bt.setIconTextGap(5);
        return bt;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        enteredPopup = true;
    }

    public void mouseExited(MouseEvent e) {
        if (e.getSource() == this && enteredPopup && !this.contains(e.getPoint())) {
            dispose();
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == spMaxSpeed) {
            int value = (Integer) spMaxSpeed.getValue();

            if (value != config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0)) {
                config.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                config.save();
            }
        } else if (e.getSource() == spMaxDls) {
            int value = (Integer) spMaxDls.getValue();

            if (value != config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2)) {
                config.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                config.save();
            }
        } else if (e.getSource() == spMaxChunks) {
            int value = (Integer) spMaxChunks.getValue();

            if (value != config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2)) {
                config.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, value);
                config.save();
            }
        }
    }

}