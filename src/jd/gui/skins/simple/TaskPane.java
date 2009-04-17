package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.gui.skins.simple.tasks.TaskPanel;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTaskPaneContainer;

public class TaskPane extends JXTaskPaneContainer implements ActionListener {

    private static final long serialVersionUID = 2484591650508276173L;
    private ArrayList<TaskPanel> panels;
    private TaskPanel lastSource;

    public TaskPane() {
        panels = new ArrayList<TaskPanel>();
        this.setBackgroundPainter(null);
        this.setLayout(new MigLayout("ins 2, wrap 1", "[fill,grow]", "[]2[]2[]2[]2[]2[]2[]2[]2[fill,grow]"));
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
            src.broadcastEvent(new ActionEvent(src, TaskPanel.ACTION_CLICK, "Click"));
            SimpleGUI.CURRENTGUI.hideSideBar(false);
        }

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
