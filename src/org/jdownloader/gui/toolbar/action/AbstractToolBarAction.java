package org.jdownloader.gui.toolbar.action;

import javax.swing.AbstractButton;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;

public abstract class AbstractToolBarAction extends CustomizableAppAction {

    public AbstractToolBarAction() {
        super();
        addContextSetup(new ToolbarContext());
    }

    protected String createMnemonic() {
        return "-";
    }

    public String getIconKey() {
        if (StringUtils.isEmpty(super.getIconKey())) return IconKey.ICON_QUESTION;
        if (MenuItemData.EMPTY_NAME.equals(super.getIconKey())) return IconKey.ICON_QUESTION;
        return iconKey;
    }

    public Object getValue(String key) {

        if (LARGE_ICON_KEY == (key)) { return NewTheme.I().getIcon(getIconKey(), 24); }
        if (SMALL_ICON == (key)) { return NewTheme.I().getIcon(getIconKey(), 18); }

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

    public AbstractButton createButton() {
        return null;
    }
}
