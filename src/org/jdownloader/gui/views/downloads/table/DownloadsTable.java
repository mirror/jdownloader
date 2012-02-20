package org.jdownloader.gui.views.downloads.table;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.DownloadTableAction;
import org.jdownloader.gui.views.downloads.context.DeleteAction;
import org.jdownloader.images.NewTheme;

public class DownloadsTable extends PackageControllerTable<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 8843600834248098174L;

    public DownloadsTable(final DownloadsTableModel tableModel) {
        super(tableModel);

        if (Application.getJavaVersion() >= Application.JAVA16) {
            this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
            this.setTransferHandler(new DownloadsTableTransferHandler(this));
            this.setDragEnabled(true);
            this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }

        initActions();
        onSelectionChanged();
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    @Override
    protected void onSelectionChanged() {
        if (this.getExtTableModel().countSelectedObjects() == 0) {
            // disable move buttons
            moveDownAction.setEnabled(false);
            moveToBottomAction.setEnabled(false);
            moveTopAction.setEnabled(false);
            moveUpAction.setEnabled(false);
        } else {
            // TODO
            moveDownAction.setEnabled(true);
            moveToBottomAction.setEnabled(true);
            moveTopAction.setEnabled(true);
            moveUpAction.setEnabled(true);
        }
    }

    @Override
    protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
        if (!pressed) { return super.processKeyBinding(stroke, evt, condition, pressed); }

        switch (evt.getKeyCode()) {

        case KeyEvent.VK_UP:
            if (evt.isAltDown()) {
                this.moveUpAction.actionPerformed(null);
                return true;
            }
            break;
        case KeyEvent.VK_DOWN:
            if (evt.isAltDown()) {
                this.moveDownAction.actionPerformed(null);
                return true;
            }
            break;

        case KeyEvent.VK_HOME:
            if (evt.isAltDown()) {
                moveTopAction.actionPerformed(null);
                return true;

            }
            break;
        case KeyEvent.VK_END:
            if (evt.isAltDown()) {
                moveToBottomAction.actionPerformed(null);
                return true;
            }
            break;
        }

        return super.processKeyBinding(stroke, evt, condition, pressed);
    }

    private void initActions() {
        moveTopAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_totop());
                setToolTipText(_GUI._.BottomBar_BottomBar_totop_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-top", 20));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-top");
            }

        };
        moveUpAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_moveup());
                setToolTipText(_GUI._.BottomBar_BottomBar_moveup_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-up", 20));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-up");
            }

        };
        moveDownAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_movedown());
                setToolTipText(_GUI._.BottomBar_BottomBar_movedown_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-down", 20));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-down");
            }

        };
        moveToBottomAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_tobottom());
                setToolTipText(_GUI._.BottomBar_BottomBar_tobottom_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-bottom", 20));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-bottom");
            }

        };
    }

    private DownloadTableAction moveTopAction;
    private DownloadTableAction moveUpAction;
    private DownloadTableAction moveDownAction;
    private DownloadTableAction moveToBottomAction;

    public AppAction getMoveDownAction() {
        return moveDownAction;
    }

    public AppAction getMoveTopAction() {
        return moveTopAction;
    }

    public AppAction getMoveToBottomAction() {
        return moveToBottomAction;
    }

    public AppAction getMoveUpAction() {
        return moveUpAction;
    }

    @Override
    protected void onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        // if (!isEditing()) {
        // new EditLinkOrPackageAction(this, obj).actionPerformed(null);
        // }
    }

    @Override
    protected boolean onShortcutDelete(final ArrayList<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {

        new DeleteAction(getAllSelectedChildren(selectedObjects), direct).actionPerformed(null);
        return true;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final ArrayList<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent ev) {
        /* split selection into downloadlinks and filepackages */
        return DownloadTableContextMenuFactory.getInstance().create(this, popup, contextObject, selection, column, ev);

    }

    //
    // /**
    // * Creates all contextmenu items, and a initial rating for it.
    // *
    // * @param obj
    // * @param col
    // * @param alllinks
    // * @param sfp
    // * @return
    // */
    // protected RatedMenuController createMenuItems(AbstractNode obj, int col,
    // ArrayList<DownloadLink> alllinks, ArrayList<FilePackage> sfp) {
    // final RatedMenuController ret = new RatedMenuController();
    //
    // ret.add(new RatedMenuItem(new StopsignAction(obj), 0));
    // ret.add(new RatedMenuItem(new EnableAction(alllinks), 10));
    // ret.add(new RatedMenuItem(new DisableAction(alllinks), 10));
    // ret.add(new RatedMenuItem(new ForceDownloadAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new ResumeAction(alllinks), 10));
    //
    // ret.add(new RatedMenuItem(new ResetAction(alllinks), 5));
    // ret.add(RatedMenuItem.createSeparator());
    //
    // ret.add(new RatedMenuItem(new NewPackageAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new CheckStatusAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new CreateDLCAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new CopyURLAction(alllinks), 0));
    // ret.add(RatedMenuItem.createSeparator());
    //
    // ret.add(new RatedMenuItem(new SetPasswordAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new CopyPasswordAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new DeleteAction(alllinks), 0));
    // ret.add(new RatedMenuItem(new DeleteFromDiskAction(alllinks), 0));
    //
    // ret.add(RatedMenuItem.createSeparator());
    // if (obj instanceof FilePackage) {
    // ret.add(new RatedMenuItem(new OpenDirectoryAction(new File(((FilePackage)
    // obj).getDownloadDirectory())), 0));
    //
    // /* TODO: sort action */
    // // final JDTableColumn column =
    // // this.getJDTableModel().getJDTableColumn(col);
    // // if (column.isSortable(sfp)) {
    // // this.getDefaultSortMenuItem().set(column, sfp,
    // // _GUI._.gui_table_contextmenu_packagesort() + " (" + sfp.size() +
    // // "), (" + this.getJDTableModel().getColumnName(col) + ")");
    // // ret.add(new RatedMenuItem("SORTITEM",
    // // this.getDefaultSortMenuItem(), 0));
    // // }
    //
    // ret.add(new RatedMenuItem(new PackageNameAction(sfp), 0));
    // ret.add(new RatedMenuItem(new PackageDirectoryAction(sfp), 0));
    // } else if (obj instanceof DownloadLink) {
    // ret.add(new RatedMenuItem(new OpenDirectoryAction(new
    // File(((DownloadLink) obj).getFileOutput()).getParentFile()), 0));
    // ret.add(new RatedMenuItem(new OpenInBrowserAction(alllinks), 0));
    // /*
    // * check if Java version 1.6 or higher is installed, because the
    // * Desktop-Class (e.g. to open a file with correct application) is
    // * only supported by v1.6 or higher
    // */
    // if (Application.getJavaVersion() >= Application.JAVA16 &&
    // CrossSystem.isOpenFileSupported()) {
    // // add the Open File entry
    // ret.add(new RatedMenuItem(new OpenFileAction(new File(((DownloadLink)
    // obj).getFileOutput())), 0));
    // }
    // }
    // ret.add(RatedMenuItem.createSeparator());
    // final ArrayList<MenuAction> entries = new ArrayList<MenuAction>();
    // JDUtilities.getController().fireControlEventDirect(new ControlEvent(obj,
    // ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries, this));
    // if (entries != null && entries.size() > 0) {
    // for (final MenuAction next : entries) {
    // if (next.getType() == ToolBarAction.Types.SEPARATOR) {
    // ret.add(RatedMenuItem.createSeparator());
    // } else {
    // ret.add(new RatedMenuItem(next, 0));
    // }
    // }
    // }
    // ret.add(RatedMenuItem.createSeparator());
    // ret.add(new RatedMenuItem("PRIORITY",
    // PrioritySubMenu.createPrioMenu(alllinks), 0));
    // return ret;
    // }

    @Override
    public boolean editCellAt(int row, int column) {

        boolean ret = super.editCellAt(row, column);

        return ret;
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean ret = super.editCellAt(row, column, e);
        if (ret) {

            AbstractNode object = getExtTableModel().getObjectbyRow(row);
            if (object instanceof FilePackage) {
                String title = _GUI._.DownloadsTable_editCellAt_filepackage_title();
                String msg = _GUI._.DownloadsTable_editCellAt_filepackage_msg();
                ImageIcon icon = NewTheme.I().getIcon("wizard", 32);
                JDGui.help(title, msg, icon);

            } else {
                String title = _GUI._.LinkGrabberTable_editCellAt_link_title();
                String msg = _GUI._.LinkGrabberTable_editCellAt_link_msg();
                ImageIcon icon = NewTheme.I().getIcon("edit", 32);
                JDGui.help(title, msg, icon);
            }

        }
        return ret;
    }

    @Override
    protected void onHeaderSortClick(final MouseEvent e1, final ExtColumn<AbstractNode> oldSortColumn, String oldSortId) {

        // own thread to
        new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Timer t = (Timer) e.getSource();
                t.stop();
                if (oldSortColumn == getExtTableModel().getSortColumn()) return;
                if (getExtTableModel().getSortColumn() != null) {

                    try {

                        SimpleTextBallon d = new SimpleTextBallon(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsTable_actionPerformed_sortwarner_title(getExtTableModel().getSortColumn().getName()), _GUI._.DownloadsTable_actionPerformed_sortwarner_text(), NewTheme.I().getIcon("sort", 32)) {

                            @Override
                            protected String getDontShowAgainKey() {
                                return "downloadtabe_sortwarner";
                            }

                        };
                        d.setDesiredLocation(e1.getLocationOnScreen());
                        Dialog.getInstance().showDialog(d);
                    } catch (OffScreenException e1) {
                        e1.printStackTrace();
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }

                }

            }
        }).start();

    }

}
