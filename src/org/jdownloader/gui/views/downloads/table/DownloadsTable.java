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
import javax.swing.Timer;
import javax.swing.TransferHandler;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.context.DeleteAction;
import org.jdownloader.images.NewTheme;

public class DownloadsTable extends PackageControllerTable<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 8843600834248098174L;

    public DownloadsTable(final DownloadsTableModel tableModel) {
        super(tableModel);
        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
        this.setTransferHandler(new DownloadsTableTransferHandler(this));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        onSelectionChanged();
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
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

    @Override
    protected boolean onShortcutCopy(ArrayList<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getCopyAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy"));
        return true;
    }

    @Override
    protected boolean onShortcutCut(ArrayList<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getCutAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cut"));
        return true;
    }

    @Override
    protected boolean onShortcutPaste(ArrayList<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "paste"));
        return true;
    }

}
