package org.jdownloader.extensions;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.components.IDImageIcon;
import org.appwork.utils.images.IconIO;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.gui.LAFOptions;

import net.miginfocom.swing.MigLayout;

public class Header extends JPanel {
    /**
     *
     */
    private static final long   serialVersionUID = 1L;
    private static final String CONSTRAINT       = "";
    private JLabel              label;
    private JCheckBox           enabled;
    private JLabel              iconLabel;
    private int                 version;

    public Header(String name, Icon icon) {
        super(new MigLayout("ins 0" + CONSTRAINT, "[35!]5[]10[grow,fill]"));
        iconLabel = icon == null ? new JLabel() : new JLabel(wrapIcon(icon));
        add(iconLabel, "alignx right");
        if (icon == null) {
            setLayout(new MigLayout("ins 0", "[0!]5[]10[grow,fill]"));
        }
        label = new JLabel("<html><u><b>" + name + "</b></u></html>");
        label.setBorder(null);
        LAFOptions.getInstance().applyConfigHeaderTextColor(label);
        add(label);
        add(new JSeparator());
        setOpaque(false);
    }

    public JLabel getIconLabel() {
        return iconLabel;
    }

    public Header(String name, Icon icon, BooleanKeyHandler listener) {
        this(name, icon, listener, -1);
    }

    public Header(String name, Icon icon, BooleanKeyHandler listener, int version) {
        super(new MigLayout("ins 0" + CONSTRAINT, "[35!]5[]10[grow,fill]10[]2[]"));
        iconLabel = new JLabel(wrapIcon(icon));
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
        add(new JLabel(_JDT.T.configheader_enabled()));
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
            if (enabled.isSelected() == isEnabled) {
                return;
            }
            enabled.setSelected(isEnabled);
        }
    }

    public boolean isHeaderEnabled() {
        return enabled.isSelected();
    }

    public void setIcon(Icon icon) {
        Icon _getIcon = wrapIcon(icon);
        if (iconLabel.getIcon() == null && _getIcon != null) {
            setLayout(new MigLayout("ins 0" + CONSTRAINT, "[35!]5[]10[grow,fill]"));
        } else if (iconLabel.getIcon() != null && _getIcon == null) {
            setLayout(new MigLayout("ins 0" + CONSTRAINT, "[0!]5[]10[grow,fill]"));
        }
        iconLabel.setIcon(_getIcon);
    }

    public Icon wrapIcon(Icon icon) {
        if (icon == null) {
            return null;
        }
        int max = icon.getIconHeight() > 30 || icon.getIconWidth() > 30 ? 32 : 22;
        if (icon.getIconWidth() > max || icon.getIconHeight() > max) {
            icon = IconIO.getScaledInstance(icon, max, max);
        }
        ExtMergedIcon _getIcon = new ExtMergedIcon(new IDImageIcon(IconIO.createEmptyImage(max, max)));
        _getIcon.add(icon, (max - icon.getIconWidth()) / 2, (max - icon.getIconHeight()) / 2);
        return _getIcon;
    }

    public void setText(String name) {
        if (version > 0) {
            label.setText("<html><u><b>" + name + "</b></u> Version " + version + "</html>");
        } else {
            label.setText("<html><u><b>" + name + "</b></u></html>");
        }
    }
}
