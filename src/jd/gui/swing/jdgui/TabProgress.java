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

package jd.gui.swing.jdgui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.GuiRunnable;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXBusyLabel;

/**
 * Diese Klasse zeigt alle Fortschritte von momenten aktiven Plugins an.
 * 
 * @author JD-Team
 */
public class TabProgress extends JPanel implements ControlListener {

    private static final long serialVersionUID = -8537543161116653345L;

    private static final int MAX_BARS = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_VISIBLE_MODULES, 6);

    /**
     * Hier werden alle Fortschritte der Plugins gespeichert
     */
    private ArrayList<ProgressController> controllers;

    private ProgressEntry[] lines;

    private JXBusyLabel title;

    private boolean updateInProgress = false;

    private long latestUpdateTime = 0;

    /**
     * Die Tabelle für die Pluginaktivitäten
     */
    public TabProgress() {
        controllers = new ArrayList<ProgressController>();
        JDUtilities.getController().addControlListener(this);
        this.setVisible(false);
        lines = new ProgressEntry[MAX_BARS];
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]"));

        initGUI();
        this.setTitle(0);
    }

    private void setTitle(int runningModules) {
        title.setText(JDL.LF("gui.progresspane.title", "%s module(s) running", runningModules));
    }

    private void initGUI() {
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));
        add(title = new JXBusyLabel(new Dimension(24, 24)), "split 3, gapleft 10, gapbottom 5, gaptop 5");
        title.setIconTextGap(15);
        add(new JSeparator(), "growx,pushx,gapright 15");
        add(new JLabel(JDTheme.II("gui.images.config.tip", 16, 16)));
        for (int i = 0; i < MAX_BARS; i++) {
            lines[i] = new ProgressEntry();
        }
    }

    private void addController(ProgressController source) {
        synchronized (controllers) {
            if (!controllers.contains(source)) controllers.add(0, source);
        }
    }

    private void removeController(ProgressController source) {
        synchronized (controllers) {
            controllers.remove(source);
        }
    }

    public void controlEvent(ControlEvent event) {
        synchronized (controllers) {
            if (event.getID() == ControlEvent.CONTROL_ON_PROGRESS && event.getSource() instanceof ProgressController) {
                ProgressController source = (ProgressController) event.getSource();
                addController(source);
                if (source.isFinished()) {
                    removeController(source);
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            update(true);
                            return null;
                        }
                    }.start();
                } else {
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            update(false);
                            return null;
                        }
                    }.start();
                }
            }
        }
    }

    protected void update(boolean force) {
        if (!force) {
            if (updateInProgress) return;
            if ((System.currentTimeMillis() - latestUpdateTime) < (500)) return;
            updateInProgress = true;
        }
        sortControllers();
        synchronized (controllers) {
            for (int i = 0; i < Math.min(controllers.size(), MAX_BARS); i++) {
                if (!lines[i].isAttached()) {
                    this.add(lines[i], "height 20!");
                    lines[i].setAttached(true);
                }
                lines[i].update(controllers.get(i));

            }
            for (int i = Math.max(0, Math.min(controllers.size(), MAX_BARS)); i < MAX_BARS; i++) {
                if (lines[i].isAttached()) {
                    this.remove(lines[i]);
                    lines[i].setAttached(false);
                }

            }
            if (controllers.isEmpty()) {
                title.setBusy(false);
                this.setVisible(false);
            } else {
                title.setBusy(true);
                this.setVisible(true);
            }
            this.setTitle(controllers.size());
            this.revalidate();
            this.repaint();
            if (!force) updateInProgress = false;
            latestUpdateTime = System.currentTimeMillis();
        }
    }

    /**
     * Sorts the controllers
     */
    private void sortControllers() {
        synchronized (controllers) {
            Collections.sort(controllers);
        }
    }

    private class ProgressEntry extends JPanel implements ActionListener {

        private static final long serialVersionUID = 2676301394570621548L;
        private JLabel label;
        private JProgressBar bar;
        private JButton cancel;
        private boolean attached = false;
        private ProgressController controller = null;
        private Color bgc;
        private Color fgc;

        public void setAttached(boolean attached) {
            this.attached = attached;
        }

        public ProgressEntry() {
            this.setLayout(new MigLayout("ins 0", "20![18!]11![grow,fill]5![20!]", "16!"));
            this.add(label = new JLabel());
            this.add(bar = new JProgressBar());
            this.add(cancel = new JButton(JDTheme.II("gui.images.cancel", 16, 16)), "width 16!,height 16!");
            bgc = bar.getBackground();
            fgc = bar.getForeground();
            cancel.setBorderPainted(false);
            cancel.setContentAreaFilled(false);
            cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cancel.setOpaque(false);
            cancel.setFocusable(false);
            cancel.addActionListener(this);
        }

        public boolean isAttached() {
            return attached;
        }

        public void update(ProgressController controller) {
            this.controller = controller;
            if (!controller.isInterruptable()) {
                cancel.setIcon(JDTheme.II("gui.images.cancel", 16, 16));
                cancel.setEnabled(false);
                cancel.setToolTipText(JDL.L("gui.progressbars.cancel.tooltip.disabled", "Not possible to interrupt this module"));
            } else {
                if (controller.isAbort()) {
                    cancel.setIcon(JDTheme.II("gui.images.bad", 16, 16));
                    cancel.setEnabled(false);
                    cancel.setToolTipText(JDL.L("gui.progressbars.cancel.tooltip.interrupted", "Termination in progress"));
                } else {
                    cancel.setIcon(JDTheme.II("gui.images.cancel", 16, 16));
                    cancel.setEnabled(true);
                    cancel.setToolTipText(JDL.L("gui.progressbars.cancel.tooltip.enabled", "Interrupt this module"));
                }
            }
            label.setIcon(controller.getIcon() == null ? JDTheme.II("gui.images.running", 16, 16) : controller.getIcon());
            label.setToolTipText(JDL.L("gui.tooltip.progressicon", "This module is active"));
            if (controller.isIndeterminate()) {
                bar.setIndeterminate(true);
            } else {
                bar.setMaximum(10000);
                bar.setValue(controller.getPercent());
            }
            bar.setStringPainted(true);
            bar.setString(controller.getStatusText());
            if (controller.getColor() != null) {
                bar.setBackground(controller.getColor());
                bar.setForeground(controller.getColor().brighter());
            } else {
                bar.setBackground(bgc);
                bar.setForeground(fgc);
            }
        }

        public void actionPerformed(ActionEvent arg0) {
            if (arg0.getSource() == this.cancel) {
                if (controller != null && !controller.isAbort()) controller.fireCancelAction();
            }
        }

    }

}
