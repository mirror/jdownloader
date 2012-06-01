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

package jd.gui.swing.jdgui;

import java.lang.reflect.Constructor;

import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;

/**
 * This class is used as a wrapper for {@link SwitchPanel}'s. It creates only
 * one instance of the desired SwitchPanel.
 * 
 * @author Coalado
 */
public class SingletonPanel {

    private SwitchPanel panel;

    private Class<? extends SwitchPanel> clazz;
    private Object[] objs;

    public SingletonPanel(SwitchPanel panel) {
        this.panel = panel;
    }

    /**
     * 
     * @param clazz
     *            {@link Class} reference of the extended {@link SwitchPanel}
     * @param objects
     *            the parameters of the desired constructor in class
     */
    public SingletonPanel(Class<? extends SwitchPanel> clazz, Object... objects) {
        this.clazz = clazz;
        this.objs = objects;
    }

    public synchronized SwitchPanel getPanel() {
        if (panel == null) {
            try {
                createPanel();
            } catch (Exception e) {
                JDLogger.exception(e);
                return null;
            }
        }

        return panel;
    }

    private void createPanel() throws Exception {
        Class<?>[] classes = new Class[objs.length];
        for (int i = 0; i < objs.length; i++)
            classes[i] = objs[i].getClass();

        Constructor<? extends SwitchPanel> c = clazz.getConstructor(classes);
        panel = c.newInstance(objs);
    }

}
