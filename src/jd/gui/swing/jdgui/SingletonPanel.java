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
import java.lang.reflect.InvocationTargetException;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;

/**
 * This calss is used as a jpanelwrapper.. it creates an instance of a Class<?
 * extends SwitchPanel> class only once
 * 
 * @author Coalado
 * 
 */
public class SingletonPanel {

    private SwitchPanel panel;

    private Class<? extends SwitchPanel> clazz;
    private Object[] objs;

    public SingletonPanel(SwitchPanel linkListPane) {
        this.panel = linkListPane;
    }

    /**
     * 
     * @param Class
     *            <? extends SwitchPanel> class
     * @param paranmeter
     *            of the desred constructor in class
     */
    public SingletonPanel(Class<? extends SwitchPanel> class1, Object... objects) {
        clazz = class1;
        objs = objects;
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

    private void createPanel() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
       
                    Class<?>[] classes = new Class[objs.length];
                    for (int i = 0; i < objs.length; i++)
                        classes[i] = objs[i].getClass();

                    Constructor<? extends SwitchPanel> c = clazz.getConstructor(classes);
                    panel = c.newInstance(objs);
                

        

    }

}
