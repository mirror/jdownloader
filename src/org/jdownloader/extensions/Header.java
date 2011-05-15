package org.jdownloader.extensions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdownloader.translate._JDT;

public class Header extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JLabel            label;
    private JCheckBox         enabled;
    private JLabel            iconLabel;

    public Header(String name, ImageIcon icon) {
        super(new MigLayout("ins 0", "[35!]5[]10[grow,fill]"));
        iconLabel = new JLabel(icon);
        add(iconLabel, "alignx right");
        label = new JLabel("<html><u><b>" + name + "</b></u></html>");

        label.setBorder(null);

        add(label);
        add(new JSeparator());
        setOpaque(false);
    }

    public Header(String name, ImageIcon icon, ActionListener listener) {
        super(new MigLayout("ins 0", "[35!]5[]10[grow,fill]10[]2[]"));
        iconLabel = new JLabel(icon);
        add(iconLabel, "alignx right");
        label = new JLabel("<html><u><b>" + name + "</b></u></html>");

        label.setBorder(null);

        add(label);
        add(new JSeparator());
        enabled = new JCheckBox(_JDT._.configheader_enabled());
        enabled.addActionListener(listener);
        enabled.setHorizontalTextPosition(SwingConstants.LEFT);
        enabled.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                label.setEnabled(enabled.isSelected());
            }
        });
        add(enabled);
        add(new JSeparator(), "width 3!");
        setOpaque(false);
    }

    public void setHeaderEnabled(boolean isEnabled) {
        label.setEnabled(isEnabled);
        iconLabel.setEnabled(isEnabled);
        // do not fire events of nothing changed
        if (enabled != null) {
            if (enabled.isSelected() == isEnabled) return;
            enabled.setSelected(isEnabled);
        }

    }

    public boolean isHeaderEnabled() {
        return enabled.isSelected();
    }

    public void setIcon(ImageIcon _getIcon) {
        iconLabel.setIcon(_getIcon);
    }

    public void setText(String name) {
        label.setText("<html><u><b>" + name + "</b></u></html>");
    }

}
