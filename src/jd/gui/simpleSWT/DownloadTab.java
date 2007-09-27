package jd.gui.simpleSWT;

import java.util.LinkedList;

import jd.plugins.DownloadLink;
import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class DownloadTab {

    public ExtendedTree trDownload;
    /**
     * Geghoert zum Drag&Drop System
     */
    private ExtendedTreeItem[] dragSourceItem;
    private MainGui mainGui;
    /**
     * Liste Aller TreeEditoren
     */

    private int lastStep = 0;
    private int lastTime = (int) System.currentTimeMillis();
    private Text renameText;
    public DownloadTab(MainGui mainGui) {
        this.mainGui = mainGui;
        initDownload();
    }

    private void initDownload() {

        CTabItem tbDownloadFiles = new CTabItem(mainGui.folder, SWT.NONE);
        tbDownloadFiles.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.name"));
        tbDownloadFiles.setImage(JDSWTUtilities.getImageSwt("download"));
        /**
         * Listeners
         */
        Listener newFolderListener = new Listener() {
            public void handleEvent(Event event) {
                /**
                 * TODO
                 */
                String text = JDSWTUtilities.getSWTResourceString("DownloadTab.newFolder.name");

                if (trDownload.getSelectionCount() > 0) {
                    ExtendedTreeItem item = (ExtendedTreeItem) trDownload.getSelection()[0];
                        ExtendedTreeItem parent = item.getParentItem();
                        if (parent == null) {
                            int index = trDownload.tree.indexOf(item.item);
                            ExtendedTreeItem im = new ExtendedTreeItem(trDownload, index);
                            im.setType(ExtendedTreeItem.TYPE_FOLDER);
                            im.setText(text);
                            trDownload.setSelection(im);
                        } else if (parent.getType() == ExtendedTreeItem.TYPE_FOLDER && !item.isLocked()) {
                            int index = parent.item.indexOf(item.item);
                            ExtendedTreeItem im = new ExtendedTreeItem(parent, index);
                            im.setType(ExtendedTreeItem.TYPE_FOLDER);
                            im.setText(text);
                            trDownload.setSelection(im);
                        } else {
                            ExtendedTreeItem parent2 = parent.getParentItem();
                            if (parent2 == null) {
                                int index = trDownload.tree.indexOf(parent.item);
                                ExtendedTreeItem im = new ExtendedTreeItem(trDownload, index);
                                im.setType(ExtendedTreeItem.TYPE_FOLDER);
                                im.setText(text);
                                trDownload.setSelection(im);
                            } else {
                                int index = parent2.item.indexOf(parent.item);
                                ExtendedTreeItem im = new ExtendedTreeItem(parent2, index);
                                im.setType(ExtendedTreeItem.TYPE_FOLDER);
                                im.setText(text);
                                trDownload.setSelection(im);
                        }
                    }
                    // workaround fuer die scheiss treeEditoren
                    // laesst die treeEditoren neu zeichnen
                    trDownload.notifyListeners(SWT.Collapse, new Event());
                } else {

                    ExtendedTreeItem im = new ExtendedTreeItem(trDownload);
                    im.setType(ExtendedTreeItem.TYPE_FOLDER);
                    im.setText(text);
                    trDownload.setSelection(im);

                    // muss hier nicht gemacht werden, da das item
                    // sowieso an letzter stelle hinzugefuegt
                    // wird
                }
                mainGui.guiListeners.getListener("DownloadTab.rename").handleEvent(new Event());

            }

        };
        mainGui.guiListeners.addListener("DownloadTab.newFolder", newFolderListener);

        Listener newContainerListener = new Listener() {
            public void handleEvent(Event event) {
                /**
                 * TODO
                 */
                String[] text = {JDSWTUtilities.getSWTResourceString("DownloadTab.newContainer.name"), "", ""};

                if (trDownload.getSelectionCount() > 0) {
                    ExtendedTreeItem item = (ExtendedTreeItem) trDownload.getSelection()[0];
                        ExtendedTreeItem parent = item.getParentItem();
                        if (parent == null) {
                            int index = trDownload.tree.indexOf(item.item);
                            ExtendedTreeItem im = new ExtendedTreeItem(trDownload, index);
                            im.setType(ExtendedTreeItem.TYPE_CONTAINER);
                            im.setText(text);
                            trDownload.setSelection(im);
                        } else if (parent.getType() == ExtendedTreeItem.TYPE_FOLDER && !item.isLocked()) {
                            int index = parent.item.indexOf(item.item);
                            ExtendedTreeItem im = new ExtendedTreeItem(parent, index);
                            im.setType(ExtendedTreeItem.TYPE_CONTAINER);
                            im.setText(text);
                            trDownload.setSelection(im);
                        } else {
                            ExtendedTreeItem parent2 = parent.getParentItem();
                            if (parent2 == null) {
                                int index = trDownload.tree.indexOf(parent.item);
                                ExtendedTreeItem im = new ExtendedTreeItem(trDownload, index);
                                im.setType(ExtendedTreeItem.TYPE_CONTAINER);
                                im.setText(text);
                                trDownload.setSelection(im);
                            } else {
                                int index = parent2.item.indexOf(parent.item);
                                ExtendedTreeItem im = new ExtendedTreeItem(parent2, index);
                                im.setType(ExtendedTreeItem.TYPE_CONTAINER);
                                im.setText(text);
                                trDownload.setSelection(im);
                        }
                    }
                    trDownload.notifyListeners(SWT.Collapse, new Event());
                } else {

                    ExtendedTreeItem im = new ExtendedTreeItem(trDownload);
                    im.setType(ExtendedTreeItem.TYPE_CONTAINER);
                    im.setText(text);
                    trDownload.setSelection(im);

                    // muss hier nicht gemacht werden, da das item
                    // sowieso an letzter stelle hinzugefuegt
                    // wird
                }
                mainGui.guiListeners.getListener("DownloadTab.rename").handleEvent(new Event());

            }

        };
        mainGui.guiListeners.addListener("DownloadTab.newContainer", newContainerListener);

        Listener deleteListener = new Listener() {
            public void handleEvent(Event event) {
                ExtendedTreeItem[] items = trDownload.getOwnSelection();
                MessageBox mbDelete = new MessageBox(trDownload.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
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
                    trDownload.notifyListeners(SWT.Collapse, new Event());
                }

            }
        };
        mainGui.guiListeners.addListener("DownloadTab.delete", deleteListener);

        Listener goUpListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(1);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goUp", goUpListener);
        Listener goDownListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(2);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goDown", goDownListener);
        Listener goLastUpListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(3);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goLastUp", goLastUpListener);
        Listener goLastDownListener = new Listener() {

            public void handleEvent(Event arg0) {
                goTo(4);
            }

        };
        mainGui.guiListeners.addListener("DownloadTab.goLastDown", goLastDownListener);
        /**
         * Listenersende
         */
        CTabFolder folder = tbDownloadFiles.getParent();
        final Shell shell = folder.getShell();
        trDownload = new ExtendedTree(folder, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        tbDownloadFiles.setControl(trDownload.tree);
        trDownload.setHeaderVisible(true);
        /*
         * Workearound fuer ColumnReorder
         */
        Listener downloadColumnListener = new Listener() {

            public void handleEvent(Event e) {
                lastStep = 0;
                lastTime = e.time + 5000;
            }

        };
        Listener downloadColumnMoveListener = new Listener() {

            public void handleEvent(final Event e) {
                if (e.time == 0)
                    return;
                if (lastStep == 0) {
                    if (e.time < lastTime + 100)
                        return;
                    lastStep++;
                    lastTime = e.time;
                } else {
                    lastStep++;
                    lastTime = e.time;
                    if ((lastStep > 2)) {
                        lastStep = 0;
                        return;
                    }

                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(2);
                            } catch (InterruptedException e) {
                            }
                            trDownload.getDisplay().syncExec(new Runnable() {
                                public void run() {
                                    trDownload.setHeaderVisible(false);
                                    trDownload.setHeaderVisible(true);

                                }
                            });

                        }
                    }.start();

                }

            }

        };

        Listener downloadColumnResize = new Listener() {

            public void handleEvent(Event e) {
                lastTime = e.time;
                TreeColumn[] columns = trDownload.getColumns();
                int[] DownloadColumnWidht = new int[columns.length];
                for (int i = 0; i < (columns.length); i++) {
                    DownloadColumnWidht[i] = columns[i].getWidth();
                }
                if (shell.getMaximized() == true)
                    mainGui.guiConfig.DownloadColumnWidhtMaximized = DownloadColumnWidht;
                else
                    mainGui.guiConfig.DownloadColumnWidht = DownloadColumnWidht;
                DownloadColumnWidht = null;

            }

        };

        for (int i = 0; i < mainGui.guiConfig.DownloadColumnWidht.length; i++) {
            TreeColumn col = JDSWTUtilities.treeCol(JDSWTUtilities.getSWTResourceString("DownloadTab.column" + i + ".name"), trDownload.tree, mainGui.guiConfig.DownloadColumnWidht[i]);
            col.setResizable(true);
            col.setMoveable(true);
            col.addListener(SWT.Resize, downloadColumnResize);
            col.addListener(SWT.Move, downloadColumnMoveListener);
            col.addListener(SWT.Selection, downloadColumnListener);
        }
        trDownload.setColumnOrder(mainGui.guiConfig.DownloadColumnOrder);
        treeAddDragAndDrop();
        // Create the editor and set its attributes
        final TreeEditor editor = new TreeEditor(trDownload.tree);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        mainGui.guiListeners.addListener("DownloadTab.trDownload_MouseDown", new Listener() {
            ExtendedTreeItem ownSelected = null;
            public void handleEvent(Event e) {
                if (trDownload.getSelectionCount() == 1) {
                    ExtendedTreeItem[] items = trDownload.getOwnSelection();
                    if (items.length == 0)
                        ownSelected = null;
                    else if (ownSelected != null && ownSelected == items[0]) {
                        Point pt = new Point(e.x, e.y);
                        Rectangle rect = items[0].item.getBounds(0);
                        rect.x += 24;
                        rect.width -= 24;
                        if (rect.contains(pt) && e.button == 1) {
                            mainGui.guiListeners.getListener("DownloadTab.rename").handleEvent(e);
                            ownSelected = null;
                        } else
                            ownSelected = items[0];
                    } else
                        ownSelected = items[0];
                } else {
                    ownSelected = null;
                }
            }

        });
        trDownload.addListener(SWT.MouseDown, mainGui.guiListeners.getListener("DownloadTab.trDownload_MouseDown"));
        mainGui.guiListeners.addListener("DownloadTab.rename", new Listener() {

            public void handleEvent(Event event) {
                // Make sure one and only one item is selected when F2 is
                // pressed

                // Determine the item to edit
                ExtendedTreeItem[] tItems = trDownload.getOwnSelection();
                if (tItems.length < 1)
                    return;
                final ExtendedTreeItem item = tItems[0];
                trDownload.setSelection(item);

                if (item == null || item.item.isDisposed())
                    return;
                // Create a text field to do the editing
                renameText = new Text(trDownload.tree, SWT.NONE);
                renameText.setText(item.getText(0));
                renameText.selectAll();
                renameText.setFocus();

                // If the text field loses focus, set its text into the tree
                // and end the editing session

                renameText.addListener(SWT.FocusOut, new Listener() {

                    public void handleEvent(Event e) {
                        item.setText(0, renameText.getText());
                        renameText.dispose();

                    }

                });
                final Listener mouseDown = new Listener() {

                    public void handleEvent(Event e) {
                        Point pt = new Point(e.x, e.y);
                        Rectangle rect = item.item.getBounds(0);
                        rect.x += 24;
                        rect.width -= 24;
                        if (!rect.contains(pt)) {
                            renameText.notifyListeners(SWT.FocusOut, new Event());
                        }
                    }

                };
                trDownload.addListener(SWT.MouseDown, mouseDown);

                // If they hit Enter, set the text into the tree and end the
                // editing
                // session. If they hit Escape, ignore the text and end the
                // editing
                // session

                renameText.addListener(SWT.KeyDown, new Listener() {

                    public void handleEvent(Event e) {
                        switch (e.keyCode) {
                            case SWT.CR : {
                                // Enter hit--set the text into the tree and
                                // drop through
                                renameText.notifyListeners(SWT.FocusOut, new Event());
                                break;
                            }
                            case SWT.ESC :
                                // End editing session
                                renameText.dispose();
                                break;
                        }

                    }

                });

                renameText.addListener(SWT.Dispose, new Listener() {

                    public void handleEvent(Event e) {
                        trDownload.removeListener(SWT.MouseDown, mouseDown);

                    }

                });

                editor.setEditor(renameText, item.item);

            }
        });

        /**
         * Kontextmenu fuer die TreeItems
         */
        Menu menu = new Menu(shell, SWT.POP_UP);
        trDownload.tree.setMenu(menu);
        /*
         * Menu zum erstellen neuer Ordner
         */
        MenuItem itemNewFolder = new MenuItem(menu, SWT.PUSH);
        itemNewFolder.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.itemNewFolder.name"));
        itemNewFolder.addListener(SWT.Selection, newFolderListener);

        /*
         * Menu zum erstellen neuer Container
         */
        MenuItem itemNewContainer = new MenuItem(menu, SWT.PUSH);
        itemNewContainer.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.itemNewContainer.name"));
        itemNewContainer.addListener(SWT.Selection, newContainerListener);
        trDownload.tree.setMenu(menu);

        new MenuItem(menu, SWT.SEPARATOR);

        /*
         * Menu zum Kopieren von Items
         */
        final MenuItem itemCopy = new MenuItem(menu, SWT.PUSH);
        itemCopy.setText(JDSWTUtilities.getSWTResourceString("itemCopy.name"));
        itemCopy.addListener(SWT.Selection, mainGui.guiListeners.addTreeCopyListener(trDownload, "DownloadTab.copy"));

        /*
         * Menu zum loeschen von Items
         */
        final MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
        itemDelete.setText(JDSWTUtilities.getSWTResourceString("itemDelete.name"));
        itemDelete.addListener(SWT.Selection, deleteListener);

        /*
         * Menu zum umbennen von Items
         */
        final MenuItem itemRename = new MenuItem(menu, SWT.PUSH);
        itemRename.setText(JDSWTUtilities.getSWTResourceString("DownloadTab.itemRename.name"));
        itemRename.addListener(SWT.Selection, mainGui.guiListeners.getListener("DownloadTab.rename"));

        new MenuItem(menu, SWT.SEPARATOR);
        MenuItem itemSelectAll = new MenuItem(menu, SWT.PUSH);
        itemSelectAll.setText(JDSWTUtilities.getSWTResourceString("itemSelectAll.name"));
        itemSelectAll.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                trDownload.tree.selectAll();

            }
        });

        menu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(org.eclipse.swt.events.MenuEvent e) {
                boolean isSelected = trDownload.getSelectionCount() > 0;
                itemDelete.setEnabled(isSelected);
                itemRename.setEnabled(trDownload.getOwnSelection().length > 0);
                itemCopy.setEnabled(isSelected);

            }
        });

        {
            String[] text2 = {"Archiv.php", "", "", ""};

            ExtendedTreeItem tr1 = new ExtendedTreeItem(trDownload);
            tr1.setText(text2);

        }
        trDownload.addListener(SWT.Selection, mainGui.guiListeners.initToolBarBtSetEnabledListener());
        trDownload.addListener(SWT.Collapse, mainGui.guiListeners.getListener("toolBarBtSetEnabled"));
        trDownload.tree.addListener(SWT.KeyDown, mainGui.guiListeners.initTrDownloadKeyListener());
    }

    /**
     * Checkt ob das Item Selektiert ist
     * 
     * @param item
     * @param selection
     * @return
     */
    private boolean isPartof(ExtendedTreeItem item, ExtendedTreeItem[] selection) {
        for (int i = 0; i < selection.length; i++) {
            if (item == selection[i])
                return true;
        }
        return false;
    }
    /**
     * Diese Methode ist fuer die Buttons in der Toolbar die die Items
     * verschieben es gibt vier Positionen zwischen denen man waehlen kann
     * 1=GoUP 2=GoDown 3=GoLastUp 4=GoLastDown
     * 
     * @param pos
     */
    private void goTo(int pos) {
        if ((renameText != null) && !renameText.isDisposed())
            renameText.dispose();
        int pos2;
        if (pos == 1) {
            pos2 = 0;
            pos = -1;
        } else
            pos2 = 1;

        ExtendedTreeItem[] items = trDownload.getOwnSelection();
        for (int i = 0; i < items.length; i++) {
            int cc = i;
            if (pos == 2 || pos == 3)
                cc = items.length - i - 1;
            if (pos < 3) {
                ExtendedTreeItem parent = items[cc].getParentItem();
                if (parent != null) {
                    int index = parent.item.indexOf(items[cc].item);

                    if (pos < 0 & index > 0 || pos > 0 & index != (parent.item.getItemCount() - 1)) {
                        items[cc].setPosition(parent, index + pos);
                    } else {
                        ExtendedTreeItem parent2 = parent.getParentItem();
                        if (parent2 == null) {
                            index = trDownload.tree.indexOf(parent.item);
                            items[cc].setPosition(trDownload, index + pos2);
                        } else {
                            index = parent2.item.indexOf(parent.item);
                            items[cc].setPosition(parent2, index + pos2);
                        }

                    }
                } else {
                    int index = trDownload.tree.indexOf(items[cc].item);
                    items[cc].setPosition(trDownload, index + pos);
                }
            } else if (pos == 3) {
                items[cc].setPosition(trDownload, 0);
            } else {
                items[cc].setPosition(trDownload);
            }
        }
        trDownload.setSelection(items);
        trDownload.notifyListeners(SWT.Collapse, new Event());

    }
    public boolean isDragItem(ExtendedTreeItem item, ExtendedTreeItem[] dragSourceItems) {
        if (isPartof(item, dragSourceItems))
            return true;
        else {
            ExtendedTreeItem parent = item.getParentItem();
            if (parent != null)
                if (isPartof(parent, dragSourceItems))
                    return true;
        }
        return false;
    }
    // public void moveItem()
    /**
     * Hier faengt das Drag&Drop System an und meine Dokumentation hoert auf
     * denn das will sich sowieso keiner antun
     */
    private void treeAddDragAndDrop() {
        final ExtendedTree tree = trDownload;
        Transfer[] types = new Transfer[]{TextTransfer.getInstance()};
        int operations = DND.DROP_MOVE;
        final DragSource source = new DragSource(tree.tree, operations);
        source.setTransfer(types);
        source.addDragListener(new DragSourceListener() {
            public void dragStart(DragSourceEvent event) {
                if ((renameText != null) && !renameText.isDisposed())
                    renameText.dispose();
                event.doit = true;
                ExtendedTreeItem[] selection = tree.getOwnSelection();
                if (selection.length == 0) {
                    event.doit = false;
                }

                dragSourceItem = selection;

            };

            public void dragSetData(DragSourceEvent event) {

                String evd = "";
                for (int i = 0; i < dragSourceItem.length; i++) {
                    DownloadLink downloadLink = dragSourceItem[i].getDownloadLink();
                    if (downloadLink != null)
                        evd += downloadLink.getEncryptedUrlDownload() + ((i != dragSourceItem.length - 1) ? System.getProperty("line.separator") : "");
                    else
                        evd += dragSourceItem[i].getText() + ((i != dragSourceItem.length - 1) ? System.getProperty("line.separator") : "");
                }

                event.data = evd;
            }

            public void dragFinished(DragSourceEvent event) {
                dragSourceItem = null;
            }
        });

        DropTarget target = new DropTarget(tree.tree, operations);
        target.setTransfer(types);

        target.addDropListener(new DropTargetAdapter() {
            public void dragOver(DropTargetEvent event) {
                event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
                if (event.item != null) {
                    ExtendedTreeItem item = (ExtendedTreeItem) ((TreeItem) event.item).getData();;
                    Point pt = tree.getDisplay().map(null, tree.tree, event.x, event.y);
                    Rectangle bounds = item.item.getBounds();
                    if (pt.y < bounds.y + bounds.height / 3) {
                        event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
                    } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                        event.feedback |= DND.FEEDBACK_INSERT_AFTER;
                    } else {
                        event.feedback |= DND.FEEDBACK_SELECT;
                    }
                }
            }

            public void drop(DropTargetEvent event) {
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                if (dragSourceItem == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                if (event.item == null) {
                    for (int i = 0; i < dragSourceItem.length; i++) {
                        dragSourceItem[i].setPosition(tree);
                    }
                    tree.setSelection(dragSourceItem);

                } else {
                    ExtendedTreeItem item = (ExtendedTreeItem) ((TreeItem) event.item).getData();
                    boolean isContainer = item.getType() == ExtendedTreeItem.TYPE_CONTAINER;

                    if (isDragItem(item, dragSourceItem)) {
                        event.detail = DND.DROP_NONE;
                        return;
                    }
                    Point pt = tree.getDisplay().map(null, tree.tree, event.x, event.y);
                    Rectangle bounds = item.getBounds();
                    ExtendedTreeItem parent = item.getParentItem();

                    if (parent != null) {
                        int index = parent.item.indexOf(item.item);
                        if (pt.y < bounds.y + bounds.height / 3) {
                            for (int i = 0; i < dragSourceItem.length; i++) {
                                dragSourceItem[i].setPosition(parent, index);
                            }
                            tree.setSelection(dragSourceItem);
                        } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                            if (item.getType() == ExtendedTreeItem.TYPE_FOLDER | isContainer) {
                                LinkedList<ExtendedTreeItem> selectItems = new LinkedList<ExtendedTreeItem>();
                                for (int i = 0; i < dragSourceItem.length; i++) {
                                    if (!isContainer || dragSourceItem[i].getType() == ExtendedTreeItem.TYPE_FILE) {
                                        dragSourceItem[i].setPosition(parent, index + 1);
                                        selectItems.add(dragSourceItem[i]);
                                    }
                                }
                                tree.setSelection(selectItems.toArray(new ExtendedTreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        } else {
                            if (item.getType() == ExtendedTreeItem.TYPE_FOLDER | isContainer) {
                                LinkedList<ExtendedTreeItem> selectItems = new LinkedList<ExtendedTreeItem>();
                                for (int i = 0; i < dragSourceItem.length; i++) {
                                    if (!isContainer || dragSourceItem[i].getType() == ExtendedTreeItem.TYPE_FILE) {
                                        dragSourceItem[i].setPosition(item);
                                        selectItems.add(dragSourceItem[i]);
                                    }
                                }
                                tree.setSelection(selectItems.toArray(new ExtendedTreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        }

                    } else {
                        int index = tree.tree.indexOf(item.item);
                        if (pt.y < bounds.y + bounds.height / 3) {
                            for (int i = 0; i < dragSourceItem.length; i++) {
                                dragSourceItem[i].setPosition(tree, index);
                            }
                            tree.setSelection(dragSourceItem);
                        } else if (pt.y > bounds.y + 2 * bounds.height / 3) {
                            if (item.getType() == ExtendedTreeItem.TYPE_FOLDER | isContainer) {
                                LinkedList<ExtendedTreeItem> selectItems = new LinkedList<ExtendedTreeItem>();
                                for (int i = 0; i < dragSourceItem.length; i++) {
                                    if (!isContainer || dragSourceItem[i].getType() == ExtendedTreeItem.TYPE_FILE) {
                                        dragSourceItem[i].setPosition(tree, index + 1);
                                        selectItems.add(dragSourceItem[i]);
                                    }
                                }
                                tree.setSelection(selectItems.toArray(new ExtendedTreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        } else {
                            if (item.getType() == ExtendedTreeItem.TYPE_FOLDER | isContainer) {
                                LinkedList<ExtendedTreeItem> selectItems = new LinkedList<ExtendedTreeItem>();
                                for (int i = 0; i < dragSourceItem.length; i++) {
                                    if (!isContainer || dragSourceItem[i].getType() == ExtendedTreeItem.TYPE_FILE) {
                                        dragSourceItem[i].setPosition(item);
                                        selectItems.add(dragSourceItem[i]);
                                    }
                                }
                                tree.setSelection(selectItems.toArray(new ExtendedTreeItem[selectItems.size()]));
                                selectItems = null;
                            } else {
                                event.detail = DND.DROP_NONE;
                                return;
                            }
                        }
                    }

                }
                tree.notifyListeners(SWT.Collapse, new Event());
            }
        });
    }

}
