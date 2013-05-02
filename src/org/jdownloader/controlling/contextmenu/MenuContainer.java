package org.jdownloader.controlling.contextmenu;

import javax.swing.JMenu;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.gui.ExtMenuImpl;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class MenuContainer extends MenuItemData {
    public MenuContainer() {
        super();
        setType(null);

    }

    public void add(Class<? extends AppAction> class1, MenuItemProperty... ps) {
        add(new ActionData(class1, ps));
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

        if (getIconKey() != null) {
            subMenu.setIcon(NewTheme.I().getIcon(getIconKey(), 18));
        }

        return subMenu;
    }

}
