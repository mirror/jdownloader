package org.jdownloader.gui.views.linkgrabber;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.swing.components.ExtCheckBox;

public class Checkbox extends MigPanel {

    private JCheckBox checkbox;
    private JLabel    lbl;

    public Checkbox(Class<? extends ConfigInterface> class1, String storageKey, String label, String tt) {
        super("ins 0", "[grow,fill][]8[]4", "[]");
        checkbox = new ExtCheckBox(class1, storageKey);
        lbl = new JLabel(label);
        this.setToolTipText(tt);
        add(Box.createHorizontalGlue());
        add(lbl);
        add(checkbox);

    }

}
