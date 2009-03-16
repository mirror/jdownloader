package jd.gui.skins.simple;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class LeafPanelWrapper {

    private Class<?> clazz;
    private Object[] constructObjects;
    private JTabbedPanel panel = null;;

    public LeafPanelWrapper(Class<?> clazz, Object[] objects) {
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

    private void constructPanel() {
        if (constructObjects == null || clazz == null) return;
        Class<?>[] classes = new Class[constructObjects.length];
        for (int i = 0; i < constructObjects.length; i++) {
            classes[i] = constructObjects[i].getClass();
        }
        Constructor<?> con;
        try {
            con = clazz.getConstructor(classes);

            this.panel = (JTabbedPanel) con.newInstance(constructObjects);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

}
