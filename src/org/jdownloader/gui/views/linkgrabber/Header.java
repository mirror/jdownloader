package org.jdownloader.gui.views.linkgrabber;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;

public class Header extends MigPanel {

    private ExtCheckBox checkBox;
    private JLabel      lbl;

    public Header(BooleanKeyHandler visible, String title) {
        super("ins 0", "[grow,fill][]8[]4", "[]");
        setOpaque(false);
        setBackground(null);
        // add(new JSeparator(), "gapleft 15");
        add(Box.createGlue());
        lbl = SwingUtils.toBold(new JLabel(title));
        // lbl.setForeground(new
        // Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor()));
        add(lbl);

        checkBox = new ExtCheckBox(visible, lbl);
        add(checkBox);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JTable().getGridColor()));

    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

}
