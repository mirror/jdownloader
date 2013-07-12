package org.jdownloader.controlling.contextmenu;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.JMenu;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.gui.ExtMenuImpl;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class MenuContainer extends MenuItemData {
    public MenuContainer() {
        super();
        setType(null);

    }

    public void add(Class<? extends AppAction> class1) {
        add(new ActionData(class1));
    }

    public void add(ActionData actionData) {
        add(new MenuItemData(actionData));
    }

    public Type getType() {
        return Type.CONTAINER;
    }

    public MenuContainer(String name, String iconKey) {
        super();
        setType(null);
        setName(name);
        setIconKey(iconKey);
    }

    @Override
    public JMenu createItem(SelectionInfo<?, ?> selection) {

        JMenu subMenu = new ExtMenuImpl(getName());
        if (StringUtils.isNotEmpty(_getDescription())) {
            subMenu.getAccessibleContext().setAccessibleDescription(_getDescription());
        }
        if (StringUtils.isNotEmpty(getMnemonic())) {

            Field f;
            try {
                f = KeyEvent.class.getField("VK_" + Character.toUpperCase(getMnemonic().charAt(0)));

                final int m = (Integer) f.get(null);
                subMenu.setMnemonic(m);
            } catch (Exception e) {
                throw new WTFException(e);
            }

        }
        if (getIconKey() != null) {
            subMenu.setIcon(NewTheme.I().getIcon(getIconKey(), 18));
        }

        return subMenu;
    }

}
