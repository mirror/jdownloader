package jd.gui.skins.simple;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import jd.gui.skins.simple.config.ConfigPanelGeneral;

public class LeafPanelWrapper {

    private Class<ConfigPanelGeneral> clazz;
    private Object[] constructObjects;
    private JTabbedPanel panel = null;;

    public LeafPanelWrapper(Class<ConfigPanelGeneral> clazz, Object[] objects) {
        this.clazz = clazz;
        this.constructObjects = objects;
    }

    public LeafPanelWrapper(JTabbedPanel panel) {
        this.panel = panel;
    }

    public synchronized JTabbedPanel getPanel() {
        if (panel == null) {
            constructPanel();
        }

        return panel;

    }

    @SuppressWarnings("unchecked")
    private void constructPanel() {
        if (constructObjects == null || clazz == null) return;
        Class[] classes = new Class[constructObjects.length];
        for (int i = 0; i < constructObjects.length; i++) {
            classes[i] = constructObjects[i].getClass();
        }
        Constructor<?> con;
        try {
            con = clazz.getConstructor(classes);

            this.panel = (JTabbedPanel) con.newInstance(constructObjects);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
