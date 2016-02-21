package org.jdownloader.api.myjdownloader.remotemenu;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractButton;

import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.api.content.v2.MyJDMenuItem;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.ExtensionNotLoadedException;

public abstract class MenuBuilder {

    // private MenuStructure2Storable root;

    // private MenuContainer menuData;
    // private ContextMenuManager<?, ?> menuManager;

    private HashMap<MyJDMenuItem, HashSet<Integer>> mnemonics;

    private LogInterface                            logger;

    public MenuBuilder() {
        // this.root = root;

        // this.menuManager = menuManager;
        // menuData = md;
        logger = LoggerFactory.getDefaultLogger();
        mnemonics = new HashMap<MyJDMenuItem, HashSet<Integer>>();

    }

    protected void registerMnemonic(MyJDMenuItem root, int mnem) {
        HashSet<Integer> set = mnemonics.get(root);
        if (set == null) {
            mnemonics.put(root, set = new HashSet<Integer>());
        }
        set.add(mnem);
    }

    public void applyMnemonic(MyJDMenuItem root, final AbstractButton submenu) {
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

    protected boolean isMnemonicUsed(MyJDMenuItem root, int mnem) {
        HashSet<Integer> set = mnemonics.get(root);
        if (set == null) {
            return false;
        }
        return set.contains(mnem);
    }

    /**
     * @param root
     * @param md
     */
    protected void createLayer(final MyJDMenuItem root, MenuContainer md) {
        if (root == null) {
            return;
        }
        // long t = System.currentTimeMillis();
        try {
            int counter = 0;
            // boolean hasToggle = false;

            for (MenuItemData i : md.getItems()) {
                try {
                    final MenuItemData inst = i;
                    if (inst._getValidateException() != null) {
                        continue;
                    }

                    switch (inst.getType()) {
                    case ACTION:

                        addAction(root, inst, counter, md.getItems().size());

                        break;
                    case CONTAINER:

                        MyJDMenuItem submenu;
                        submenu = addContainer(root, inst, counter, md.getItems().size());
                        createLayer(submenu, (MenuContainer) inst);

                        if (submenu.size() == 0) {
                            root.remove(submenu);
                        }

                    }
                    ;

                } catch (Throwable e) {
                    logger.warning("Could Not Build MenuItem: " + i);
                    logger.log(e);
                } finally {
                    counter++;
                }

            }

        } finally {
            // System.out.println("Menu Creation Layer: " + md + " took " + (System.currentTimeMillis() - t));
        }

    }

    protected abstract MyJDMenuItem addContainer(final MyJDMenuItem root, final MenuItemData inst, int index, int size) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException;

    protected abstract void addAction(final MyJDMenuItem root, final MenuItemData inst, int index, int size) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException;

}
