package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.gui.skins.simple.tasks.TaskPanel;

import org.jdesktop.swingx.JXTaskPaneContainer;

public class TaskPane extends JXTaskPaneContainer implements ActionListener {
    private ArrayList<TaskPanel> panels;
    private TaskPanel lastSource;

    public TaskPane() {
        panels = new ArrayList<TaskPanel>();

    }

    public void add(TaskPanel panel) {
        super.add(panel);
        if (!panels.contains(panel)) {
            panel.addActionListener(this);
            panels.add(panel);
            lastSource = panel;
            switcher();
        }

    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() instanceof TaskPanel) {
            TaskPanel source = ((TaskPanel) e.getSource());

            if (source != lastSource) {
                lastSource = source;
                switcher();
            }
        }

    }

    private void switcher() {
        for (TaskPanel p : panels) {
            if (p != lastSource) {
                p.setCollapsed(true);
            } else {
                p.setCollapsed(false);
            }
        }
    }
}
