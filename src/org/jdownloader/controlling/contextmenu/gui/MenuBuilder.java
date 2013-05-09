package org.jdownloader.controlling.contextmenu.gui;

import javax.swing.JComponent;
import javax.swing.JMenu;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.views.SelectionInfo;

public class MenuBuilder {

    private JComponent               root;
    private SelectionInfo<?, ?>      selection;
    private MenuContainer            menuData;
    private ContextMenuManager<?, ?> menuManager;
    private LogSource                logger;

    public MenuBuilder(ContextMenuManager<?, ?> menuManager, JComponent root, SelectionInfo<?, ?> si, MenuContainer md) {
        this.root = root;
        selection = si;
        this.menuManager = menuManager;
        menuData = md;
        logger = menuManager.getLogger();

    }

    /**
     * @param root
     * @param md
     */
    protected void createLayer(final JComponent root, MenuContainer md) {
        if (root == null) return;

        for (MenuItemData i : md.getItems()) {
            try {
                final MenuItemData inst = i;
                if (inst._getValidateException() != null) continue;
                if (root.getComponentCount() == 0 && inst instanceof SeperatorData) continue;

                switch (inst.getType()) {
                case ACTION:

                    inst.addTo(root, selection);

                    break;
                case CONTAINER:
                    final JMenu submenu = (JMenu) inst.addTo(root, selection);

                    createLayer(submenu, (MenuContainer) inst);

                }
                ;

            } catch (Throwable e) {
                logger.warning("Could Not Build MenuItem: " + i);
                logger.log(e);
            }

        }
        ;
        if (root instanceof ExtMenuInterface) {
            ((ExtMenuInterface) root).cleanup();
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
