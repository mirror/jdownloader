package jd.gui.skins.simple;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BorderFactory;

import jd.gui.skins.simple.tasks.TaskPanel;
import jd.nutils.JDImage;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTaskPaneContainer;

public class TaskPane extends JXTaskPaneContainer implements ActionListener  {

    private static final long serialVersionUID = 2484591650508276173L;
    private ArrayList<TaskPanel> panels;
    private TaskPanel lastSource;

    private boolean overButton;

    public TaskPane() {
        panels = new ArrayList<TaskPanel>();
        this.setLayout(new MigLayout("ins 2, wrap 1", "[fill,grow]", "[]2[]2[]2[]2[]2[]2[]2[]2[fill,grow]"));
//        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        // add(new JLabel(JDImage.getImageIcon("default/enlarge_left")),
        // "cell 0 7, dock east");
      
   
    }



    public void add(TaskPanel panel) {
        super.add(panel);
        if (!panels.contains(panel)) {
            panel.addActionListener(this);
            panels.add(panel);
            switcher(null);
        }
    }

    public void add(int index, TaskPanel panel) {
        super.add(panel, index);
        if (!panels.contains(panel)) {
            panel.addActionListener(this);
            switcher(null);
        }
    }

    public void remove(TaskPanel panel) {
        super.remove(panel);
        if (panels.contains(panel)) {
            panel.removeActionListener(this);
            // int index = panels.indexOf(panel);
            panels.remove(panel);
            // switcher(panels.get(index));
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof TaskPanel) {
            TaskPanel source = ((TaskPanel) e.getSource());

            if (source != lastSource) {
                lastSource = source;
                switcher(null);
            }
        }
    }

    public void switcher(TaskPanel src) {
        if (src != null) {
            lastSource = src;
            src.broadcastEvent(new ActionEvent(src, TaskPanel.ACTION_TOGGLE, "Toggle"));
            SimpleGUI.CURRENTGUI.hideSideBar(false);
        }
      
        boolean all = true;
        for (TaskPanel p : panels) {
            if (p != lastSource) {
                p.setSpecial(false);
                p.setDeligateCollapsed(true);
            } else {
                p.setSpecial(true);
                p.setDeligateCollapsed(false);
            }
        }
    }

   
}
