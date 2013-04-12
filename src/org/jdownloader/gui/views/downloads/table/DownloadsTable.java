package org.jdownloader.gui.views.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.TransferHandler;

import jd.SecondLevelLaunch;
import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.downloads.context.DeleteSelectionAction;
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

        final MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]");
        // loaderPanel.setPreferredSize(new Dimension(200, 200));

        loaderPanel.setOpaque(false);
        loaderPanel.setBackground(null);

        final CircledProgressBar loader = new CircledProgressBar() {
            public int getAnimationFPS() {
                return 25;
            }
        };

        loader.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 1.0f));

        loader.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 0.1f));
        ((ImagePainter) loader.getValueClipPainter()).setBackground(null);
        ((ImagePainter) loader.getValueClipPainter()).setForeground(null);
        loader.setIndeterminate(true);

        loaderPanel.add(loader);

        final JProgressBar ph = new JProgressBar();

        ph.setString(_GUI._.DownloadsTable_DownloadsTable_init_plugins());

        SecondLevelLaunch.HOST_PLUGINS_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ph.setString(_GUI._.DownloadsTable_DownloadsTable_object_wait_for_loading_links());

                    }

                };
            }
        });

        ph.setStringPainted(true);
        ph.setIndeterminate(true);
        loaderPanel.add(ph, "alignx center");
        // loaderPanel.setSize(400, 400);

        final LayoutManager orgLayout = getLayout();
        final Component rendererPane = getComponent(0);

        setLayout(new MigLayout("ins 0", "[grow]", "[grow]"));
        removeAll();
        add(loaderPanel, "alignx center,aligny 20%");

        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (DownloadController.getInstance().isSaveAllowed()) {
                    removeLoaderPanel(loaderPanel, orgLayout, rendererPane);
                } else {
                    DownloadController.getInstance().addListener(new DownloadControllerListener() {

                        @Override
                        public void onDownloadControllerEvent(DownloadControllerEvent event) {
                            if (event.getType() == DownloadControllerEvent.TYPE.DOWNLOADLINKS_LOADED) {
                                DownloadController.getInstance().removeListener(this);
                                removeLoaderPanel(loaderPanel, orgLayout, rendererPane);
                            }
                        }
                    });
                }
                return null;
            }
        });

    }

    protected void removeLoaderPanel(final MigPanel loaderPanel, final LayoutManager orgLayout, final Component rendererPane) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                remove(loaderPanel);
                setLayout(orgLayout);

                loaderPanel.setVisible(false);
                add(rendererPane);
                repaint();
            }
        };
    }

    protected boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        showPropertiesMenu(e.getPoint(), obj);

        return false;
    }

    @Override
    public boolean isSearchEnabled() {

        return false;
    }

    protected boolean onSingleClick(MouseEvent e, final AbstractNode obj) {

        if (e.isAltDown() || e.isAltGraphDown()) {
            showPropertiesMenu(e.getPoint(), obj);
            return true;
        }
        return super.onSingleClick(e, obj);
    }

    private void showPropertiesMenu(Point point, AbstractNode obj) {
        JPopupMenu m = new JPopupMenu();

        if (obj instanceof AbstractPackageNode) {

            Image back = (((AbstractPackageNode<?, ?>) obj).isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));

            m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)), SwingConstants.LEFT)));
            m.add(new JSeparator());
        } else if (obj instanceof DownloadLink) {

            Image back = (((DownloadLink) obj).getIcon().getImage());

            m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)), SwingConstants.LEFT)));
            m.add(new JSeparator());
        }

        final ExtColumn<AbstractNode> col = this.getExtColumnAtPoint(point);

        for (Component mm : DownloadTableContextMenuFactory.fillPropertiesMenu(new SelectionInfo<FilePackage, DownloadLink>(obj, getExtTableModel().getSelectedObjects()), col)) {
            m.add(mm);
        }
        m.show(this, point.x, point.y);
    }

    @Override
    protected boolean onShortcutDelete(final java.util.List<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {
        new DeleteSelectionAction(new SelectionInfo<FilePackage, DownloadLink>(null, selectedObjects, null, evt)).actionPerformed(null);
        return true;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final java.util.List<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent ev) {
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

        return ret;
    }

    @Override
    protected boolean onHeaderSortClick(final MouseEvent e1, final ExtColumn<AbstractNode> oldSortColumn, String oldSortId, ExtColumn<AbstractNode> newColumn) {

        // own thread to
        new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Timer t = (Timer) e.getSource();
                t.stop();
                if (oldSortColumn == getExtTableModel().getSortColumn()) return;
                if (getExtTableModel().getSortColumn() != null) {
                    HelpDialog.show(e1.getLocationOnScreen(), "downloadtabe_sortwarner", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsTable_actionPerformed_sortwarner_title(getExtTableModel().getSortColumn().getName()), _GUI._.DownloadsTable_actionPerformed_sortwarner_text(), NewTheme.I().getIcon("sort", 32));

                }

            }
        }).start();
        return false;
    }

    @Override
    protected boolean onShortcutCopy(java.util.List<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getCopyAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy"));
        return true;
    }

    @Override
    protected boolean onShortcutCut(java.util.List<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getCutAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cut"));
        return true;
    }

    @Override
    protected boolean onShortcutPaste(java.util.List<AbstractNode> selectedObjects, KeyEvent evt) {
        TransferHandler.getPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "paste"));
        return true;
    }

    @Override
    public ExtColumn<AbstractNode> getExpandCollapseColumn() {
        return DownloadsTableModel.getInstance().expandCollapse;
    }

}
