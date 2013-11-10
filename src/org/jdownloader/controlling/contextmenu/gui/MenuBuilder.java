package org.jdownloader.controlling.contextmenu.gui;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenu;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.extensions.ExtensionNotLoadedException;

public class MenuBuilder {

    private JComponent                            root;

    private MenuContainer                         menuData;
    private ContextMenuManager<?, ?>              menuManager;
    private LogSource                             logger;
    private HashMap<JComponent, HashSet<Integer>> mnemonics;

    public MenuBuilder(ContextMenuManager<?, ?> menuManager, JComponent root, MenuContainer md) {
        this.root = root;

        this.menuManager = menuManager;
        menuData = md;
        logger = menuManager.getLogger();
        mnemonics = new HashMap<JComponent, HashSet<Integer>>();

    }

    protected void registerMnemonic(JComponent root, int mnem) {
        HashSet<Integer> set = mnemonics.get(root);
        if (set == null) {
            mnemonics.put(root, set = new HashSet<Integer>());
        }
        set.add(mnem);
    }

    public void applyMnemonic(JComponent root, final AbstractButton submenu) {
        int mnem = submenu.getMnemonic();
        if (mnem == 0) {
            if (submenu.getText() != null) {
                for (int i = 0; i < submenu.getText().length(); i++) {

                    mnem = org.appwork.swing.action.BasicAction.charToMnemonic(submenu.getText().charAt(i));
                    if (mnem > 0 && !isMnemonicUsed(root, mnem)) {
                        submenu.setMnemonic(mnem);
                        break;
                    }
                }

            }
        }

        registerMnemonic(root, mnem);
    }

    protected boolean isMnemonicUsed(JComponent root, int mnem) {
        HashSet<Integer> set = mnemonics.get(root);
        if (set == null) { return false; }
        return set.contains(mnem);
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
                //
                int count = root.getComponentCount();
                if (root instanceof JMenu) {
                    count = ((JMenu) root).getMenuComponentCount() + root.getComponentCount();
                }
                if (count == 0 && inst instanceof SeperatorData) continue;

                switch (inst.getType()) {
                case ACTION:

                    addAction(root, inst);

                    break;
                case CONTAINER:
                    addContainer(root, inst);

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

    protected void addContainer(final JComponent root, final MenuItemData inst) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
        final JMenu submenu = (JMenu) inst.addTo(root);
        if (submenu == null) return;
        createLayer(submenu, (MenuContainer) inst);

        if (submenu.getMenuComponentCount() == 0) {
            root.remove(submenu);
        }
    }

    protected void addAction(final JComponent root, final MenuItemData inst) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
        inst.addTo(root);
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
