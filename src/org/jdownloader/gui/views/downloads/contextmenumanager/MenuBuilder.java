package org.jdownloader.gui.views.downloads.contextmenumanager;

import javax.swing.JComponent;
import javax.swing.JMenu;

import org.jdownloader.gui.views.SelectionInfo;

public class MenuBuilder {

    private JComponent          root;
    private SelectionInfo<?, ?> selection;
    private MenuContainerRoot   menuData;
    private ContextMenuManager  menuManager;

    public MenuBuilder(ContextMenuManager menuManager, JComponent root, SelectionInfo<?, ?> si, MenuContainerRoot md) {
        this.root = root;
        selection = si;
        this.menuManager = menuManager;
        menuData = md;
    }

    /**
     * @param root
     * @param md
     */
    protected void createLayer(JComponent root, MenuContainer md) {
        if (root == null) return;
        for (MenuItemData i : md.getItems()) {
            try {
                MenuItemData inst = i.lazyReal();

                // if (inst instanceof ExtensionContextMenuItem) {
                // inst.addTo(root, selection);
                // menuManager.extend(root, (ExtensionContextMenuItem<?>) inst, selection, menuData);
                // } else {
                switch (inst.getType()) {
                case ACTION:
                    inst.addTo(root, selection);
                    break;
                case CONTAINER:

                    createLayer((JMenu) inst.addTo(root, selection), (MenuContainer) inst);

                }
                // }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }
    }

    public void run() {
        createLayer(root, menuData);
    }

}
