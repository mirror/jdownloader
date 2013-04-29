package org.jdownloader.gui.views.downloads.contextmenumanager;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jdownloader.actions.AppAction;
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

        JMenu subMenu = new JMenu(getName()) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        };
        if (getIconKey() != null) {
            subMenu.setIcon(NewTheme.I().getIcon(getIconKey(), 18));
        }

        return subMenu;
    }

}
