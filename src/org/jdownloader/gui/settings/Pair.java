package org.jdownloader.gui.settings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;

public class Pair<T extends SettingsComponent> {

    private JLabel label;

    public JLabel getLabel() {
        return label;
    }

    public T getComponent() {
        return component;
    }

    private T              component;
    private Pair<Checkbox> conditionPair;

    public void setEnabled(boolean b) {
        label.setEnabled(b);
        component.setEnabled(b);
    }

    public Pair(JLabel lbl, T comp) {
        this.label = lbl;
        this.component = comp;
    }

    public void setToolTipText(String text) {
        label.setToolTipText(text);
        component.setToolTipText(text);
    }

    public void setConditionPair(final Pair<Checkbox> toggleCustomizedPath) {
        this.conditionPair = toggleCustomizedPath;
        // do not use actionlistener. actionevents only get fired when the user performs an action
        conditionPair.getComponent().addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setEnabled(conditionPair.getComponent().isSelected());
            }
        });
        setEnabled(conditionPair.getComponent().isSelected());
    }

    public void update() {
        if (conditionPair != null) {
            setEnabled(conditionPair.getComponent().isSelected());
        }
    }

}
