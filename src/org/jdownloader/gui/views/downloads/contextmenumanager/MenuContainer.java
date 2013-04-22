package org.jdownloader.gui.views.downloads.contextmenumanager;

import javax.swing.JMenu;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class MenuContainer extends MenuItemData {
    public MenuContainer() {
        super();
        setType(Type.CONTAINER);

    }

    public MenuContainer(String name, String iconKey) {
        super();
        setName(name);
        setIconKey(iconKey);
    }

    @Override
    public JMenu createItem(SelectionInfo<?, ?> selection) {

        JMenu subMenu = new JMenu(getName());
        if (getIconKey() != null) {
            subMenu.setIcon(NewTheme.I().getIcon(getIconKey(), 18));
        }

        return subMenu;
    }

}
