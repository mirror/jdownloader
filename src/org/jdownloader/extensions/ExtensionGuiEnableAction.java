package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.appwork.utils.Application;
import org.jdownloader.images.NewTheme;

public class ExtensionGuiEnableAction extends AbstractAction {

    private static final long serialVersionUID = 6997360773808826159L;
    private AbstractExtension plg;
    private ImageIcon         icon16Enabled;
    private ImageIcon         icon16Disabled;
    private boolean           java15;

    public ExtensionGuiEnableAction(AbstractExtension plg) {
        super(plg.getName());
        this.plg = plg;
        java15 = Application.getJavaVersion() < 16000000;
        putValue(SELECTED_KEY, plg.getGUI().isActive());
        icon16Enabled = getCheckBoxImage(20, true);
        icon16Disabled = getCheckBoxImage(20, false);
        updateIcon();
    }

    private void updateIcon() {
        if (isSelected()) {
            putValue(AbstractAction.SMALL_ICON, icon16Enabled);
        } else {
            putValue(AbstractAction.SMALL_ICON, icon16Disabled);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (java15) {
            this.setSelected(!this.isSelected());
        } else {
            updateIcon();
        }

        plg.getGUI().setActive(!plg.getGUI().isActive());
    }

    public boolean isSelected() {
        final Object value = getValue(SELECTED_KEY);
        return (value == null) ? false : (Boolean) value;
    }

    public void setSelected(final boolean selected) {
        putValue(SELECTED_KEY, selected);
        updateIcon();
    }

    public ImageIcon getCheckBoxImage(int size, boolean selected) {
        // ImageIcon ret = null;
        //
        // Image back = plg._getIcon(size).getImage();
        // Image checkBox = NewTheme.I().getImage("checkbox_" + selected, 12);
        // back = ImageProvider.merge(back, checkBox, 2, 0, 0,
        // back.getHeight(null) - checkBox.getHeight(null) + 2);
        // ret = new ImageIcon(back);
        // return ret;
        if (selected) {
            return plg.getIcon(size);
        } else {
            return NewTheme.I().getDisabledIcon(plg.getIcon(size));
        }
    }
}
