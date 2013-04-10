package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;

public class CheckboxMenuItem extends MigPanel implements ActionListener {

    private BooleanKeyHandler keyHandler;

    public CheckboxMenuItem(String label, BooleanKeyHandler keyHandler, JComponent... dependencies) {
        super("ins 0", "[][]", "[]");
        ExtCheckBox cb = new ExtCheckBox(keyHandler, dependencies) {
            protected boolean getDependenciesLogic(JComponent c, boolean b) {
                return !b;
            }
        };
        this.keyHandler = keyHandler;
        add(cb);
        add(new JLabel(label));
        SwingUtils.setOpaque(this, false);
        cb.updateDependencies();
        // keyHandler.getEventSender().addListener(this, true);

    }

    //
    // @Override
    // public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    // }
    //
    // @Override
    // public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
    // setS
    // }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) {
            c.setEnabled(enabled);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // keyHandler.setValue(isSelected());
    }

}
