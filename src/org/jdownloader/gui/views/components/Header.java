package org.jdownloader.gui.views.components;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.views.linkgrabber.quickfilter.HeaderInterface;

public class Header extends MigPanel implements HeaderInterface {

    private ExtCheckBox       checkBox;
    private JLabel            lbl;
    private BooleanKeyHandler keyHandler;
    private JLabel            counter;

    public void setEnabled(boolean enabled) {
        // checkBox.setEnabled(enabled);
        lbl.setEnabled(enabled);
        counter.setEnabled(enabled);

    }

    public Header(BooleanKeyHandler visible, String title) {
        super("ins 0", "[][grow,fill][]8[]4", "[]");
        setOpaque(false);
        setBackground(null);
        // add(new JSeparator(), "gapleft 15");
        add(Box.createGlue());
        lbl = SwingUtils.toBold(new JLabel(title));
        counter = SwingUtils.toBold(new JLabel(""));
        // lbl.setForeground(new
        // Color(LAFOptions.getInstance().getPanelHeaderLineColor()));
        add(counter);
        add(lbl);
        keyHandler = visible;
        checkBox = new ExtCheckBox(visible, lbl, counter);
        add(checkBox);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JTable().getGridColor()));

    }

    public BooleanKeyHandler getKeyHandler() {
        return keyHandler;
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public void setFilterCount(final int size) {

    }

}
