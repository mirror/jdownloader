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

import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import jd.gui.skins.simple.tasks.TaskPanel;

public class SingletonPanel {

    private JTabbedPanel panel;
    private TaskPanel taskPanel;
    private Class<? extends JTabbedPanel> clazz;
    private Object[] objs;

    public SingletonPanel(JTabbedPanel linkListPane) {
        this.panel = linkListPane;
    }

    public SingletonPanel(Class<? extends JTabbedPanel> class1, Object... objects) {
        clazz = class1;
        objs = objects;
    }

    public JTabbedPanel getPanel() {
        if (panel == null) {

            try {

                createPanel();
            } catch (Exception e) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occurred",e);
                return null;
            }

        }
        if (taskPanel != null && panel instanceof ActionListener) {
            taskPanel.addActionListener((ActionListener) panel);

        }
        return panel;
    }

    private void createPanel() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?>[] classes = new Class[objs.length];
        for (int i = 0; i < objs.length; i++)
            classes[i] = objs[i].getClass();

        Constructor<? extends JTabbedPanel> c = clazz.getConstructor(classes);
        panel = c.newInstance(objs);

    }

    public void setTaskPanel(TaskPanel taskPanel) {
        this.taskPanel = taskPanel;

    }

}
