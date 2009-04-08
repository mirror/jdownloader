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
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
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
