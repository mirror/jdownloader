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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
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
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDSpinner;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class TrayIconPopup extends JWindow implements MouseListener, ChangeListener {

    private static final long        serialVersionUID  = 2623190748929934409L;

    private SubConfiguration         config;
    private JPanel                   entryPanel;
    private JPanel                   quickConfigPanel;
    private JPanel                   bottomPanel;
    private boolean                  enteredPopup;
    private JDSpinner                spMaxSpeed;
    private JDSpinner                spMaxDls;
    private JDSpinner                spMaxChunks;
    private boolean                  hideThreadrunning = false;

    private JPanel                   exitPanel;
    private ArrayList<JToggleButton> resizecomps;

    private transient Thread         hideThread;

    public TrayIconPopup() {
        // required. JWindow needs a parent to grant a nested Component focus
        super(JDGui.getInstance().getMainFrame());
        config = SubConfiguration.getConfig("DOWNLOAD");
        resizecomps = new ArrayList<JToggleButton>();
        setVisible(false);
        setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        addMouseListener(this);
        initEntryPanel();
        initQuickConfigPanel();
        initBottomPanel();
        initExitPanel();
        JPanel content = new JPanel(new MigLayout("ins 5, wrap 1", "[]", "[]5[]5[]5[]5[]"));
        add(content);
        content.add(new JLabel("<html><b>" + JDUtilities.getJDTitle() + "</b></html>"), "align center");
        content.add(new JSeparator(), "growx, spanx");
        content.add(entryPanel);
        content.add(new JSeparator(), "growx, spanx");
        content.add(quickConfigPanel);
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
        hideThread = new Thread() {
            /*
             * this thread handles closing of popup because enter/exit/move
             * events are too slow and can miss the exitevent
             */
            public void run() {
                while (true && hideThreadrunning) {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                    }
                    if (enteredPopup && hideThreadrunning) {
                        PointerInfo mouse = MouseInfo.getPointerInfo();
                        Point current = TrayIconPopup.this.getLocation();
                        if (mouse.getLocation().x < current.x || mouse.getLocation().x > current.x + TrayIconPopup.this.getSize().width) {
                            dispose();
                            break;
                        } else if (mouse.getLocation().y < current.y || mouse.getLocation().y > current.y + TrayIconPopup.this.getSize().height) {
                            dispose();
                            break;
                        }
                    }
                }
            }
        };
        hideThreadrunning = true;
        hideThread.start();
    }

    /**
     * start autohide in 3 secs if mouse did not enter popup before
     */
    public void startAutoHide() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                if (!enteredPopup) {
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            hideThreadrunning = false;
                            dispose();
                            return null;
                        }
                    }.start();
                }
            }
        }.start();
    }

    private void initEntryPanel() {
        entryPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[]", "[]0[]0[]0[]0[]0[]0[]"));
        switch (DownloadWatchDog.getInstance().getDownloadStatus()) {
        case NOT_RUNNING:
            addMenuEntry(entryPanel, "toolbar.control.start");
            addMenuEntry(entryPanel, "toolbar.control.stop");
            break;
        case RUNNING:
            addMenuEntry(entryPanel, "toolbar.control.stop");
            addMenuEntry(entryPanel, "toolbar.control.pause");
            break;
        default:
            addMenuEntry(entryPanel, "toolbar.control.start");
            addMenuEntry(entryPanel, "toolbar.control.pause");
        }

        addMenuEntry(entryPanel, "action.addurl");
        addMenuEntry(entryPanel, "action.load");
        addMenuEntry(entryPanel, "toolbar.interaction.update");
        addMenuEntry(entryPanel, "toolbar.interaction.reconnect");
        addMenuEntry(entryPanel, "action.opendlfolder");
    }

    private void initQuickConfigPanel() {
        quickConfigPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[]", "[]0[]0[]"));
        addMenuEntry(quickConfigPanel, "premiumMenu.toggle");
        addMenuEntry(quickConfigPanel, "toolbar.quickconfig.clipboardoberserver");
        addMenuEntry(quickConfigPanel, "toolbar.quickconfig.reconnecttoggle");
    }

    private void initExitPanel() {
        exitPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[]", "[]"));
        addMenuEntry(exitPanel, "action.exit");
    }

    private void initBottomPanel() {
        spMaxSpeed = new JDSpinner(JDL.L("plugins.trayicon.popup.bottom.speed", "Speed limit(KiB/s)"), "width 60!,h 20!,right");
        spMaxSpeed.getSpinner().setModel(new SpinnerNumberModel(config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
        spMaxSpeed.setToolTipText(JDL.L("gui.tooltip.statusbar.speedlimiter", "Speed Limit (KiB/s) [0 = Infinite]"));
        spMaxSpeed.getSpinner().addChangeListener(this);
        colorizeSpinnerSpeed();

        spMaxDls = new JDSpinner(JDL.L("plugins.trayicon.popup.bottom.simdls", "Simultaneous downloads"), "width 60!,h 20!,right");
        spMaxDls.getSpinner().setModel(new SpinnerNumberModel(config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setToolTipText(JDL.L("gui.tooltip.statusbar.simultan_downloads", "Maximum simultaneous Downloads [1..20]"));
        spMaxDls.getSpinner().addChangeListener(this);

        spMaxChunks = new JDSpinner(JDL.L("plugins.trayicon.popup.bottom.simchunks", "Concurrent Connections"), "width 60!,h 20!,right");
        spMaxChunks.getSpinner().setModel(new SpinnerNumberModel(config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2), 1, 20, 1));
        spMaxChunks.setToolTipText(JDL.L("gui.tooltip.statusbar.max_chunks", "Max. Connections/File"));
        spMaxChunks.getSpinner().addChangeListener(this);

        bottomPanel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[]2[]2[]"));
        bottomPanel.setOpaque(false);
        bottomPanel.add(spMaxSpeed);
        bottomPanel.add(spMaxDls);
        bottomPanel.add(spMaxChunks);
    }

    private void addMenuEntry(JPanel panel, String actionId) {
        panel.add(getMenuEntry(actionId), "growx, pushx");
    }

    private JToggleButton getMenuEntry(String actionId) {
        final ToolBarAction action = ActionController.getToolBarAction(actionId);
        JToggleButton b = createButton(action);
        if (actionId.equals("action.exit")) {
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    hideThreadrunning = false;
                    TrayIconPopup.this.dispose();
                }
            });
        }
        resizecomps.add(b);
        return b;
    }

    private JToggleButton createButton(ToolBarAction action) {
        action.init();
        JToggleButton bt = new JToggleButton(action);
        bt.setOpaque(false);
        bt.setContentAreaFilled(false);
        bt.setBorderPainted(false);
        bt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideThreadrunning = false;
                TrayIconPopup.this.dispose();
            }
        });
        bt.setIcon((Icon) action.getValue(Action.SMALL_ICON));
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setFocusPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);
        bt.setIconTextGap(5);
        bt.addMouseListener(new HoverEffect(bt));
        return bt;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        enteredPopup = true;
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == spMaxSpeed.getSpinner()) {
            int value = spMaxSpeed.getValue();

            if (value != config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0)) {
                config.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                config.save();
            }
            colorizeSpinnerSpeed();
        } else if (e.getSource() == spMaxDls.getSpinner()) {
            int value = spMaxDls.getValue();

            if (value != config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2)) {
                config.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                config.save();
            }
        } else if (e.getSource() == spMaxChunks.getSpinner()) {
            int value = spMaxChunks.getValue();

            if (value != config.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2)) {
                config.setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, value);
                config.save();
            }
        }
    }

    /** färbt den spinner ein, falls speedbegrenzung aktiv */
    private void colorizeSpinnerSpeed() {
        if (spMaxSpeed.getValue() > 0) {
            spMaxSpeed.setColor(new Color(255, 12, 3));
        } else {
            spMaxSpeed.setColor(null);
        }
    }

}