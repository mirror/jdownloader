package org.jdownloader.actions;

import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;

import org.appwork.swing.action.BasicAction;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.jdownloader.images.NewTheme;

/**
 * This abstract class is the parent class for all actions in JDownloader
 * 
 * @author thomas
 * 
 */
public abstract class AppAction extends BasicAction {

    public static final int SIZE_CONTEXT_MENU = 18;

    public static final int BUTTON_ICON_SIZE  = 20;

    private String          iconKey;

    private int             size;

    public AppAction() {
        super();

    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {

        if (!Application.isJared(AppAction.class)) {
            // just check if actions are missused. to not do this in jared
            // version. only in dev enviroment
            StackTraceElement[] st = new Exception().getStackTrace();
            StackTraceElement caller = st[2];
            if ("javax.swing.JMenuItem".equals(caller.getClassName())) {
                ImageIcon icon = getSmallIcon();
                if (icon != null && icon.getIconWidth() != SIZE_CONTEXT_MENU) {
                    Log.exception(new Exception("Action used in contextmenu, but not set to org.jdownloader.actions.AppAction.toContextMenuAction()"));
                }

            } else if ("javax.swing.JButton".equals(caller.getClassName())) {
                ImageIcon icon = getSmallIcon();
                if (icon != null && icon.getIconWidth() != BUTTON_ICON_SIZE) {
                    Log.exception(new Exception("Action used in Jbutton, but not set to org.jdownloader.actions.AppAction.toButtonAction()"));
                }
            }
            // if ("ButtonActionPropertyChangeListener".equals(sn)) {

            // }
        }
        super.addPropertyChangeListener(listener);

    }

    public AppAction toButtonAction() {
        setIconSizes(AppAction.BUTTON_ICON_SIZE);
        return this;
    }

    public AppAction toContextMenuAction() {
        setIconSizes(AppAction.SIZE_CONTEXT_MENU);
        return this;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
        setIconSizes(BUTTON_ICON_SIZE);
    }

    public AppAction setIconSizes(int size) {
        this.size = size;
        return this;
    }

    public Object getValue(String key) {
        if (iconKey != null && LARGE_ICON_KEY.equalsIgnoreCase(key)) {
            return NewTheme.I().getIcon(iconKey, size);
        } else if (iconKey != null && SMALL_ICON.equalsIgnoreCase(key)) {
            //
            return NewTheme.I().getIcon(iconKey, size);
        }
        return super.getValue(key);
    }

}
