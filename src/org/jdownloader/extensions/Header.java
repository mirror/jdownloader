package org.jdownloader.extensions;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.components.ExtCheckBox;
import org.jdownloader.translate._JDT;

public class Header extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JLabel            label;
    private JCheckBox         enabled;
    private JLabel            iconLabel;
    private int               version;

    public Header(String name, ImageIcon icon) {
        super(new MigLayout("ins 0", "[35!]5[]10[grow,fill]"));

        iconLabel = new JLabel(icon);
        add(iconLabel, "alignx right");
        if (icon == null) {
            setLayout(new MigLayout("ins 0", "[0!]5[]10[grow,fill]"));
        }
        label = new JLabel("<html><u><b>" + name + "</b></u></html>");

        label.setBorder(null);

        add(label);
        add(new JSeparator());
        setOpaque(false);
    }

    public JLabel getIconLabel() {
        return iconLabel;
    }

    public Header(String name, ImageIcon icon, BooleanKeyHandler listener) {
        this(name, icon, listener, -1);
    }

    public Header(String name, ImageIcon icon, BooleanKeyHandler listener, int version) {

        super(new MigLayout("ins 0", "[35!]5[]10[grow,fill]10[]2[]"));
        iconLabel = new JLabel(icon);
        add(iconLabel, "alignx right");
        if (version > 0) {
            label = new JLabel("<html><u><b>" + name + "</b></u> Version " + version + "</html>");
        } else {
            label = new JLabel("<html><u><b>" + name + "</b></u></html>");
        }

        this.version = version;
        label.setBorder(null);

        add(label);
        add(new JSeparator());
        add(new JLabel(_JDT._.configheader_enabled()));
        enabled = new ExtCheckBox(listener, label, iconLabel);
        enabled.setHorizontalTextPosition(SwingConstants.LEFT);

        add(enabled);
        // add(new JSeparator(), "width 3!");
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

    public void setIcon(Icon _getIcon) {
        if (iconLabel.getIcon() == null && _getIcon != null) {
            setLayout(new MigLayout("ins 0", "[35!]5[]10[grow,fill]"));
        } else if (iconLabel.getIcon() != null && _getIcon == null) {
            setLayout(new MigLayout("ins 0", "[0!]5[]10[grow,fill]"));
        }
        iconLabel.setIcon(_getIcon);
    }

    public void setText(String name) {

        if (version > 0) {
            label.setText("<html><u><b>" + name + "</b></u> Version " + version + "</html>");
        } else {
            label.setText("<html><u><b>" + name + "</b></u></html>");
        }
    }

}
