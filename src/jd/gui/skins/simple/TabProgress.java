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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
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

        // PanelUI uid = this.getUI();
        // org.jvnet.substance.swingx.SubstanceTaskPaneUI.
        // this.setUI(new UI());
        lines = new ProgressEntry[MAX_BARS];
        this.setCollapsed(JDUtilities.getSubConfig("gui").getBooleanProperty(TabProgress.COLLAPSED, false));
        setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]"));
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
        int index = controllers.indexOf(source);

        if (index >= 0) {

            controllers.remove(source);

        }

    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_ON_PROGRESS && event.getSource() instanceof ProgressController) {
            ProgressController source = (ProgressController) event.getSource();
            try {
                if (source.isFinished()) {
                    this.removeController(source);
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
        for (int i = 0; i < MAX_BARS; i++) {
            this.remove(this.lines[i]);
        }
        for (int i = 0; i < Math.min(controllers.size(), MAX_BARS); i++) {
            lines[i].update(this.controllers.get(i));
            this.add(lines[i], "height 20!");
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

    class ProgressEntry extends JXPanel {
        /**
         * 
         */
        private static final long serialVersionUID = 2676301394570621548L;
        private JLabel label;
        private JProgressBar bar;

        public ProgressEntry() {
            this.setLayout(new MigLayout("ins 0", "[20!]0[grow,fill]", "16!"));
            this.add(label = new JLabel(), "sizegroup labels");
            this.add(bar = new JProgressBar(), "wrap,sizegroup bars");
            this.add(new JSeparator(), "span");
        }

        public void update(ProgressController controller) {

            label.setIcon(controller.getIcon());
            bar.setMaximum((int) controller.getMax());
            bar.setValue((int) controller.getValue());
            bar.setStringPainted(true);
            bar.setString(controller.getStatusText());
        
           if(controller.getColor()!=null) bar.setBackground(controller.getColor());

          //  if (controller.getColor() != null) bar.setForeground(controller.getColor());

        }
    }

    // public static void main(String[] args) {
    // SwingUtilities.invokeLater(new Runnable() {
    // public void run() {
    // JFrame f = new JFrame("Test Oriented Collapsible Pane");
    //
    // f.add(new JLabel("Press Ctrl+F or Ctrl+G to collapse panes."),
    // BorderLayout.NORTH);
    //
    // JTree tree1 = new JTree();
    // tree1.setBorder(BorderFactory.createEtchedBorder());
    // f.add(tree1);
    //
    // JXCollapsiblePane pane = new JXCollapsiblePane();
    // pane.setCollapsed(true);
    // JTree tree2 = new JTree();
    // tree2.setBorder(BorderFactory.createEtchedBorder());
    // pane.add(tree2);
    // f.add(pane, BorderLayout.SOUTH);
    //
    // pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.
    // getKeyStroke("ctrl F"), JXCollapsiblePane.TOGGLE_ACTION);
    //
    // pane = new JXCollapsiblePane();
    // JTree tree3 = new JTree();
    // pane.add(tree3);
    // tree3.setBorder(BorderFactory.createEtchedBorder());
    // f.add(pane, BorderLayout.WEST);
    //
    // pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.
    // getKeyStroke("ctrl G"), JXCollapsiblePane.TOGGLE_ACTION);
    //
    // f.setSize(640, 480);
    // f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // f.setVisible(true);
    // }
    // });
    // }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        System.out.println("Task is :" + this.isCollapsed());
        JDUtilities.getSubConfig("gui").setProperty(TabProgress.COLLAPSED, this.isCollapsed());
        JDUtilities.getSubConfig("gui").save();
    }

}
