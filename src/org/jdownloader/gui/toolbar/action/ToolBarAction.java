package org.jdownloader.gui.toolbar.action;

import org.jdownloader.actions.AppAction;
import org.jdownloader.images.NewTheme;

public abstract class ToolBarAction extends AppAction {
    abstract protected String createAccelerator();

    protected String createMnemonic() {
        return "-";
    }

    public Object getValue(String key) {

        if (LARGE_ICON_KEY == (key)) { return NewTheme.I().getIcon(getIconKey(), 24); }
        if (SMALL_ICON == (key)) { return NewTheme.I().getIcon(getIconKey(), 18); }
        if (ACCELERATOR_KEY == key) {
            Object ret = super.getValue(key);
            if (ret == null) {
                setAccelerator(createAccelerator());
            }
            return super.getValue(key);
        }
        if (MNEMONIC_KEY == key || DISPLAYED_MNEMONIC_INDEX_KEY == key) {
            Object ret = super.getValue(key);
            if (ret == null) {
                if (getName() == null) setName(createTooltip());
                setMnemonic(createMnemonic());
            }
            return super.getValue(key);
        }
        if (SHORT_DESCRIPTION == key) { return createTooltip(); }
        return super.getValue(key);
    }

    protected abstract String createTooltip();
}
