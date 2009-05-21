//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.awt.Dimension;
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
        // this.setBackgroundPainter(null);
//        this.setPreferredSize(new Dimension(180, 10));
        this.setLayout(new MigLayout("ins 5, wrap 1", "[fill,grow]", "[]2[]2[]2[]2[]2[]2[]2[]2[fill,grow]"));
    }

    public void add(TaskPanel panel) {
        super.add(panel);
//        panel.setMaximumSize(new Dimension(193, Integer.MAX_VALUE));
        if (!panels.contains(panel)) {
            panel.addActionListener(this);
            panels.add(panel);
            switcher(null);
        }
    }

    public void add(int index, TaskPanel panel) {
        super.add(panel, index);
//        panel.setMaximumSize(new Dimension(193, Integer.MAX_VALUE));
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
        if (e.getSource() instanceof TaskPanel && e.getID() == TaskPanel.ACTION_TOGGLE) {
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
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                for (TaskPanel p : panels) {
                    if (p != lastSource) {
                        p.setSpecial(false);
                        p.setDeligateCollapsed(true);
                    } else {
                        p.setSpecial(true);
                        p.setDeligateCollapsed(false);
                    }
                }
                return null;
            }

        }.start();

    }

}
