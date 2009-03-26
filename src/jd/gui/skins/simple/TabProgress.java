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

package jd.gui.skins.simple;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.JCancelButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTaskPane;

/**
 * Diese Klasse zeigt alle Fortschritte von momenten aktiven Plugins an.
 * 
 * @author JD-Team
 */
public class TabProgress extends JXTaskPane implements ActionListener, ControlListener, MouseListener {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -8537543161116653345L;

    private static final int MAX_BARS = 6;

    private static final String COLLAPSED = "COLLAPSED";

    /**
     * Hier werden alle Fortschritte der Plugins gespeichert
     */
    private ArrayList<ProgressController> controllers;

    private ProgressEntry[] lines;

    /**
     * Die Tabelle für die Pluginaktivitäten
     */
    public TabProgress() {
        controllers = new ArrayList<ProgressController>();
        JDUtilities.getController().addControlListener(this);
        this.addMouseListener(this);
        this.setVisible(false);
        lines = new ProgressEntry[MAX_BARS];
        this.setCollapsed(JDUtilities.getSubConfig("gui").getBooleanProperty(TabProgress.COLLAPSED, false));
        this.setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]"));
        this.setTitle(JDLocale.LF("gui.progresspane.title", "%s modules running", 0));
        initGUI();
    }

    private void initGUI() {
        for (int i = 0; i < MAX_BARS; i++) {
            lines[i] = new ProgressEntry();
        }
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false);
    }

    private synchronized void addController(ProgressController source) {
        controllers.add(0, source);
    }

    private synchronized boolean hasController(ProgressController source) {
        return controllers.contains(source);
    }

    private synchronized void removeController(ProgressController source) {
        controllers.remove(source);
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_ON_PROGRESS && event.getSource() instanceof ProgressController) {
            ProgressController source = (ProgressController) event.getSource();
            try {
                if (source.isFinished()) {
                    removeController(source);
                    return;
                }
                if (!hasController(source)) {
                    addController(source);
                }
            } finally {
                sortControllers();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        update();
                    }
                });
            }

        }

    }

    protected void update() {
        for (int i = 0; i < Math.min(controllers.size(), MAX_BARS); i++) {
            if (!lines[i].isAttached()) {
                this.add(lines[i], "height 20!");
                System.out.println("ATTACH " + i);
                lines[i].setAttached(true);
            } else {
                System.out.println("OK " + i);
            }
            lines[i].update(controllers.get(i));

        }
        for (int i = Math.max(0, Math.min(controllers.size(), MAX_BARS)); i < MAX_BARS; i++) {
            if (lines[i].isAttached()) {
                this.remove(lines[i]);
                System.out.println("GONE " + i);

                lines[i].setAttached(false);
            }

        }
        if (controllers.size() == 0) {
            this.setVisible(false);
        } else {
            this.setVisible(true);
        }
        this.setTitle(JDLocale.LF("gui.progresspane.title", "%s modules running", controllers.size()));
        this.revalidate();
        this.repaint();
    }

    /**
     * Sorts the controllers
     */
    private void sortControllers() {
        Collections.sort(controllers, new Comparator<ProgressController>() {

            public int compare(ProgressController o1, ProgressController o2) {
                if (o1.getPercent() == o2.getPercent()) return 0;
                return o1.getPercent() < o2.getPercent() ? 1 : -1;
            }

        });
    }

    private class ProgressEntry extends JXPanel {

        private static final long serialVersionUID = 2676301394570621548L;
        private JLabel label;
        private JProgressBar bar;
        private AbstractButton cancel = new JCancelButton();
        private boolean attached = false;

        public void setAttached(boolean attached) {
            this.attached = attached;
        }

        public ProgressEntry() {
            this.setLayout(new MigLayout("ins 0", "[18!]0[grow,fill]", "16!"));
            this.add(label = new JLabel(), "sizegroup labels, width 100%");
            this.add(bar = new JProgressBar(), "sizegroup bars, width 100%");
            this.add(cancel, "sizegroup cancel, gap 5px, wrap");
            /*
             * TODO: cancel soll einen callback aufrufen welchen dann die
             * funktion abbricht
             */
            cancel.setVisible(false);
            this.add(new JSeparator(), "span");
        }

        public boolean isAttached() {
            return attached;
        }

        public void update(ProgressController controller) {
            label.setIcon(controller.getIcon());
            bar.setMaximum((int) controller.getMax());
            bar.setValue((int) controller.getValue());
            bar.setStringPainted(true);
            bar.setString(controller.getStatusText());
            bar.setBackground(controller.getColor());
        }

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        System.out.println("Task is :" + this.isCollapsed());
        JDUtilities.getSubConfig("gui").setProperty(TabProgress.COLLAPSED, this.isCollapsed());
        JDUtilities.getSubConfig("gui").save();
    }

}
