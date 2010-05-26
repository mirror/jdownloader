package jd.gui.swing.jdgui.interfaces;

import javax.swing.AbstractAction;

import jd.utils.JDTheme;

public abstract class ContextMenuAction extends AbstractAction {

    private static final long serialVersionUID = -7198618139414010743L;

    protected ContextMenuAction() {
    }

    /**
     * Must be called manually to init the name and the icon of the action.
     */
    protected final void init() {
        putValue(NAME, getName());

        String icon = getIcon();
        if (icon != null) putValue(SMALL_ICON, JDTheme.II(getIcon(), 16, 16));
    }

    protected abstract String getName();

    protected abstract String getIcon();

}
