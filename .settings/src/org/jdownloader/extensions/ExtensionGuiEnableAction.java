package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.jdownloader.actions.AppAction;
import org.jdownloader.images.NewTheme;

public class ExtensionGuiEnableAction extends AppAction {

    protected AbstractExtension<?, ?> plg;
    private ImageIcon                 icon16Enabled;
    private ImageIcon                 icon16Disabled;

    public ExtensionGuiEnableAction(AbstractExtension<?, ?> plg) {

        super();
        setName(plg.getName());
        this.plg = plg;
        setSelected(false);
        icon16Enabled = getCheckBoxImage(20, true);
        icon16Disabled = getCheckBoxImage(20, false);
        updateIcon();
    }

    protected void updateIcon() {
        if (isSelected()) {
            putValue(AbstractAction.SMALL_ICON, icon16Enabled);
        } else {
            putValue(AbstractAction.SMALL_ICON, icon16Disabled);
        }
    }

    public void actionPerformed(ActionEvent e) {

        updateIcon();

    }

    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        updateIcon();
    }

    public ImageIcon getCheckBoxImage(int size, boolean selected) {

        if (selected) {
            return plg.getIcon(size);
        } else {
            return NewTheme.I().getDisabledIcon(plg.getIcon(size));
        }
    }

}
