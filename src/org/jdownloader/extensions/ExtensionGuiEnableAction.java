package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.jdownloader.images.NewTheme;

public class ExtensionGuiEnableAction extends AbstractAction {

    protected AbstractExtension<?> plg;
    private ImageIcon              icon16Enabled;
    private ImageIcon              icon16Disabled;

    public ExtensionGuiEnableAction(AbstractExtension<?> plg) {

        super(plg.getName());
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
