package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.Customizer;

public class CustomPanel extends MigPanel {

    public CustomPanel() {
        super("ins 0", "[]", "[]");
        setOpaque(false);
    }

    public void add(final ActionData actionData, AppAction actionClass, final Field d) {
        d.setAccessible(true);
        if (Clazz.isBoolean(d.getType())) {
            add(new JLabel(d.getAnnotation(Customizer.class).name()), "pushx");
            final JCheckBox jb = new JCheckBox();
            add(jb, "wrap");
            try {
                jb.setSelected((Boolean) actionData.fetchSetup(d.getName()));
            } catch (Exception e) {
                try {
                    jb.setSelected((Boolean) d.get(actionClass));
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
            jb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    actionData.putSetup(d.getName(), jb.isSelected());

                }
            });

        }
    }
}
