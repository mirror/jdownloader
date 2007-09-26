package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

public class CompletedTab {
    public ExtendedTree trCompleted;
    private CTabFolder folder;
    private GuiListeners guiListeners;
    public CompletedTab(CTabFolder folder, GuiListeners guiListeners) {
        this.folder = folder;
        this.guiListeners = guiListeners;
        initCompleted();
    }
    private void initCompleted() {
        CTabItem tbCompletedFiles = new CTabItem(folder, SWT.NONE);
        tbCompletedFiles.setText(JDSWTUtilities.getSWTResourceString("CompletedTab.name"));
        tbCompletedFiles.setImage(JDSWTUtilities.getImageSwt("completed"));
        Shell shell = folder.getShell();
        trCompleted = new ExtendedTree(folder, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        tbCompletedFiles.setControl(trCompleted.tree);
        trCompleted.setHeaderVisible(true);
        JDSWTUtilities.treeCol(JDSWTUtilities.getSWTResourceString("CompletedTab.column0.name"), trCompleted.tree, 350);
        JDSWTUtilities.treeCol(JDSWTUtilities.getSWTResourceString("CompletedTab.column1.name"), trCompleted.tree, 300);
        JDSWTUtilities.treeCol(JDSWTUtilities.getSWTResourceString("CompletedTab.column2.name"), trCompleted.tree, 100);
        TreeColumn[] columns = trCompleted.getColumns();
        for (int i = 0; i < (columns.length); i++) {
            columns[i].setResizable(true);
            columns[i].setMoveable(true);
        }
        String[] text2 = {"Archiv.php", "", ""};
        ExtendedTreeItem tr1 = new ExtendedTreeItem(trCompleted);
        tr1.setText(text2);

        Listener compDeleteListener = new Listener() {
            public void handleEvent(Event event) {
                ExtendedTreeItem[] items = trCompleted.getOwnSelection();
                MessageBox mbDelete = new MessageBox(trCompleted.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                String itemsText = "";
                if (items.length < 30) {
                    for (int i = 0; i < items.length; i++) {
                        itemsText += System.getProperty("line.separator") + items[i].getText();
                    }
                    mbDelete.setMessage(JDSWTUtilities.getSWTResourceString("mbDelete.message").replace("%s", itemsText));
                } else
                    itemsText = JDSWTUtilities.getSWTResourceString("mbDelete.messageOver30").replace("%s", itemsText);

                mbDelete.setText(JDSWTUtilities.getSWTResourceString("mbDelete.name"));
                int response = mbDelete.open();
                if (response == SWT.YES) {
                    for (int i = 0; i < items.length; i++) {
                        items[i].dispose();
                    }
                    trCompleted.notifyListeners(SWT.Collapse, new Event());
                }

            }
        };
        guiListeners.addListener("CompletedTab.delete", compDeleteListener);
        /**
         * Kontextmenu fuer die TreeItems
         */
        Menu menu = new Menu(shell, SWT.POP_UP);
        trCompleted.tree.setMenu(menu);
        /*
         * Menu zum Kopieren von Items
         */
        final MenuItem itemCopy = new MenuItem(menu, SWT.PUSH);
        itemCopy.setText(JDSWTUtilities.getSWTResourceString("itemCopy.name"));
        itemCopy.addListener(SWT.Selection, guiListeners.addTreeCopyListener(trCompleted, "CompletedTab.copy"));

        final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
        /*
         * Menu zum loeschen von Items
         */
        itemDelete.setText(JDSWTUtilities.getSWTResourceString("itemDelete.name"));
        itemDelete.addListener(SWT.Selection, compDeleteListener);
        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(org.eclipse.swt.events.MenuEvent e) {
                itemDelete.setEnabled(trCompleted.getSelectionCount() > 0);
                itemCopy.setEnabled(trCompleted.getSelectionCount() > 0);
            }
        });
        trCompleted.addListener(SWT.Selection, guiListeners.initToolBarBtSetEnabledListener());
        trCompleted.addListener(SWT.Collapse, guiListeners.getListener("toolBarBtSetEnabled"));

    }

}
