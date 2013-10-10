package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.GetterSetter;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.Customizer;

public class CustomPanel extends MigPanel {

    private MenuManagerDialog managerFrame;
    private LogSource    logger;

    public CustomPanel(MenuManagerDialog managerFrame) {
        super("ins 0", "[]", "[]");
        this.managerFrame = managerFrame;
        logger = managerFrame.getLogger();
        setOpaque(false);
    }

    public void add(final ActionData actionData, AppAction actionClass, final GetterSetter gs) {
        try {
            if (Clazz.isBoolean(gs.getType())) {
                add(new JLabel(gs.getAnnotation(Customizer.class).name()), "pushx");
                final JCheckBox jb = new JCheckBox();
                add(jb, "wrap");
                try {
                    jb.setSelected((Boolean) actionData.fetchSetup(gs.getKey()));
                } catch (Exception e) {
                    try {
                        jb.setSelected((Boolean) gs.get(actionClass));
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                }
                jb.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        actionData.putSetup(gs.getKey(), jb.isSelected());
                        managerFrame.repaint();

                    }
                });

            } else if (Clazz.isEnum(gs.getType())) {

                final Object[] values = ReflectionUtils.getEnumValues((Class<? extends Enum>) gs.getType());
                String value = (String) actionData.fetchSetup(gs.getKey());
                Object myValue = value == null ? null : ReflectionUtils.getEnumValueOf((Class<? extends Enum>) gs.getType(), value);

                if (value == null || myValue == null) {
                    myValue = gs.get(actionClass);
                    System.out.println(1);
                    value = myValue.toString();
                }

                String[] strings = new String[values.length];
                int index = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i].toString().equals(value)) myValue = values[i];

                    if (myValue == values[i]) index = i;
                    strings[i] = values[i].toString();
                    EnumLabel lbl = ((Class) gs.getType()).getDeclaredField(values[i].toString()).getAnnotation(EnumLabel.class);
                    if (lbl != null) {
                        strings[i] = lbl.value();
                    }
                }
                final JComboBox cb = new JComboBox(strings);
                cb.setSelectedIndex(index);
                cb.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        actionData.putSetup(gs.getKey(), values[cb.getSelectedIndex()].toString());
                        managerFrame.repaint();
                    }
                });
                add(cb, "growx,spanx,wrap");
            } else if (gs.getType() == String.class) {

                String value = (String) actionData.fetchSetup(gs.getKey());

                if (value == null) {
                    value = (String) gs.get(actionClass);

                }

                final ExtTextField cb = new ExtTextField() {

                    @Override
                    public void onChanged() {
                        actionData.putSetup(gs.getKey(), getText());
                        managerFrame.repaint();
                    }

                };
                cb.setHelpText(gs.getAnnotation(Customizer.class).name());
                cb.setText(value);

                add(cb, "growx,spanx,wrap,pushx,growx");
            }
        } catch (Exception e) {
            logger.log(e);
        }
    }
}
