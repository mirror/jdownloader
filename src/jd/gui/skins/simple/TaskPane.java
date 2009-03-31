package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;

import jd.gui.skins.simple.tasks.TaskPanel;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;

public class TaskPane extends JXPanel implements ActionListener {

    private static final long serialVersionUID = 2484591650508276173L;
    private ArrayList<TaskPanel> panels;
    private TaskPanel lastSource;

    public TaskPane() {
        panels = new ArrayList<TaskPanel>();
        this.setLayout(new MigLayout("ins 5, wrap 1", "[fill,grow]"));
        this.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
    }

    public void add(TaskPanel panel) {
        super.add(panel);
        if (!panels.contains(panel)) {
            panel.addActionListener(this);
            panels.add(panel);
            lastSource = panel;
            switcher(null);
        }
    }

    public void add(int index, TaskPanel panel) {
        super.add(panel, index);
        if (!panels.contains(panel)) {
            panel.addActionListener(this);
            lastSource = panel;
            switcher(null);
        }
    }

    public void remove(TaskPanel panel) {
        super.remove(panel);
        if (panels.contains(panel)) {
            panel.removeActionListener(this);
            int index = panels.indexOf(panel);
            panels.remove(panel);
            switcher(panels.get(index));
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
        }
        for (TaskPanel p : panels) {
            if (p != lastSource) {
                p.setSpecial(false);
                p.setCollapsed(true);
            } else {
                p.setSpecial(true);
                p.setCollapsed(false);
            }
        }
    }

}
