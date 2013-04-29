package org.jdownloader.gui.views.downloads.contextmenumanager;

import javax.swing.JComponent;
import javax.swing.JMenu;

import org.appwork.utils.swing.EDTRunner;
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
    protected void createLayer(final JComponent root, MenuContainer md) {
        if (root == null) return;

        for (MenuItemData i : md.getItems()) {
            try {
                final MenuItemData inst = i.lazyReal();

                // if (inst instanceof ExtensionContextMenuItem) {
                // inst.addTo(root, selection);
                // menuManager.extend(root, (ExtensionContextMenuItem<?>) inst, selection, menuData);
                // } else {
                switch (inst.getType()) {
                case ACTION:

                    inst.addTo(root, selection);

                    break;
                case CONTAINER:
                    final JMenu submenu = (JMenu) inst.addTo(root, selection);

                    createLayer(submenu, (MenuContainer) inst);

                }
                // }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }
    }

    public void run() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                createLayer(root, menuData);
            }
        };

    }

}
