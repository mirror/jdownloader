package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import jd.SecondLevelLaunch;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.uio.UIOManager;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ContextMenuFactory;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveSelectionAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class LinkGrabberTable extends PackageControllerTable<CrawledPackage, CrawledLink> {

    private static final long  serialVersionUID = 8843600834248098174L;
    private ContextMenuFactory contextMenuFactory;

    public LinkGrabberTable(LinkGrabberPanel linkGrabberPanel, final LinkGrabberTableModel tableModel) {
        super(tableModel);

        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
        this.setTransferHandler(new LinkGrabberTableTransferHandler(this));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);

        contextMenuFactory = new ContextMenuFactory(this, linkGrabberPanel);
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

        SecondLevelLaunch.CRAWLERLIST_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ph.setString(_GUI._.LinkGrabberTable_LinkGrabberTable_object_wait_for_loading_links());

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
                if (LinkCollector.getInstance().isSaveAllowed()) {
                    removeLoaderPanel(loaderPanel, orgLayout, rendererPane);
                } else {
                    LinkCollector.getInstance().getEventsender().addListener(new LinkCollectorListener() {

                        @Override
                        public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
                        }

                        @Override
                        public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
                        }

                        @Override
                        public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorContentModified(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorAbort(LinkCollectorEvent event) {
                        }

                        @Override
                        public void onLinkCollectorListLoaded() {
                            LinkCollector.getInstance().getEventsender().removeListener(this);
                            removeLoaderPanel(loaderPanel, orgLayout, rendererPane);
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

    public void sortPackageChildren(ExtDefaultRowSorter<AbstractNode> rowSorter, String nextSortIdentifier) {
        // TODO:
        // set LinkGrabberTableModel.setTRistate....to false and implement sorter here
    }

    protected boolean onHeaderSortClick(final MouseEvent event, final ExtColumn<AbstractNode> oldColumn, final String oldIdentifier, ExtColumn<AbstractNode> newColumn) {
        if (((LinkGrabberTableModel) getExtTableModel()).isTristateSorterEnabled()) return false;
        try {
            //
            UIOManager.I().showConfirmDialog(Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.getNextSortIdentifier_sort_warning_rly_title_(), _JDT._.getNextSortIdentifier_sort_warning_rly_msg(newColumn.getName()), NewTheme.I().getIcon("help", 32), _JDT._.basics_yes(), _JDT._.basics_no());

            sortPackageChildren(newColumn.getRowSorter(), getExtTableModel().getNextSortIdentifier(newColumn.getSortOrderIdentifier()));

            return true;
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean ret = super.editCellAt(row, column, e);

        return ret;
    }

    @Override
    public boolean isSearchEnabled() {
        return false;
    }

    @Override
    protected boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        JPopupMenu m = new JPopupMenu();

        if (obj instanceof AbstractPackageNode) {

            Image back = (((AbstractPackageNode<?, ?>) obj).isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));

            m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)), SwingConstants.LEFT)));
            m.add(new JSeparator());
        } else if (obj instanceof CrawledLink) {

            Image back = (((CrawledLink) obj).getDownloadLink().getIcon().getImage());

            m.add(SwingUtils.toBold(new JLabel(_GUI._.ContextMenuFactory_createPopup_properties(obj.getName()), new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)), SwingConstants.LEFT)));
            m.add(new JSeparator());
        }

        final ExtColumn<AbstractNode> col = this.getExtColumnAtPoint(e.getPoint());

        for (JMenuItem mm : ContextMenuFactory.fillPropertiesMenu(new SelectionInfo<CrawledPackage, CrawledLink>(obj, getExtTableModel().getSelectedObjects()), col)) {
            m.add(mm);
        }
        m.show(this, e.getPoint().x, e.getPoint().y);
        return false;
    }

    @Override
    protected JPopupMenu onContextMenu(final JPopupMenu popup, final AbstractNode contextObject, final java.util.List<AbstractNode> selection, final ExtColumn<AbstractNode> column, MouseEvent event) {
        return contextMenuFactory.createPopup(contextObject, selection, column, event);
    }

    @Override
    protected boolean onShortcutDelete(final java.util.List<AbstractNode> selectedObjects, final KeyEvent evt, final boolean direct) {
        new RemoveSelectionAction(new SelectionInfo<CrawledPackage, CrawledLink>(selectedObjects)).actionPerformed(null);
        return true;
    }

    @Override
    protected boolean updateMoveButtonEnabledStatus() {
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
        return LinkGrabberTableModel.getInstance().expandCollapse;
    }
}
